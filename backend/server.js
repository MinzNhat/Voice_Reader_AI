 require("dotenv").config();

const express = require("express");

const multer = require("multer");

const axios = require("axios");

const fs = require("fs");

const path = require("path");

const cors = require("cors");

const sharp = require("sharp");


// --- IMPORTS CHO RAG ---

const { Pinecone } = require('@pinecone-database/pinecone');

const { PineconeStore } = require('@langchain/pinecone');

const { ChatGoogleGenerativeAI, GoogleGenerativeAIEmbeddings } = require('@langchain/google-genai');

const { RetrievalQAChain } = require('langchain/chains');

const { RecursiveCharacterTextSplitter } = require('langchain/text_splitter');

// Import cái này để định nghĩa TaskType cho Embedding (Fix lỗi dimension 0)

const { TaskType } = require("@google/generative-ai");


const app = express();

app.use(cors());

app.use(express.json());

app.use(express.static(path.join(__dirname, 'public')));


// Serve test HTML (Trang chủ mặc định là OCR cũ)

app.get("/", (req, res) => {

    // Nếu bạn để file index.html trong folder public thì dòng này có thể bỏ

    // Nhưng giữ lại để chắc chắn nó trỏ đúng file bạn muốn

    const indexPath = path.join(__dirname, "public", "index.html");

    if (fs.existsSync(indexPath)) {

        res.sendFile(indexPath);

    } else {

        res.send("Vui lòng tạo file index.html trong thư mục public");

    }

});


// Health check endpoint

app.get("/health", (req, res) => {

    res.json({

        success: true,

        status: "ok",

        timestamp: Date.now()

    });

});


// Multer config

const upload = multer({ dest: "uploads/" });


// Ensure uploads directory exists

if (!fs.existsSync("uploads")) {

    fs.mkdirSync("uploads");

}


// ====================================================

// 1. CONFIGURATION CHO RAG (GEMINI & PINECONE)

// ====================================================

const pinecone = new Pinecone({

    apiKey: process.env.PINECONE_API_KEY

});

const pineconeIndex = pinecone.Index(process.env.PINECONE_INDEX_NAME);


const embeddings = new GoogleGenerativeAIEmbeddings({

    model: "text-embedding-004", 

    taskType: "RETRIEVAL_DOCUMENT", 

    title: "Document",

    apiKey: process.env.GOOGLE_API_KEY

});


const llm = new ChatGoogleGenerativeAI({

    model: "gemini-2.0-flash",

    maxOutputTokens: 2048,

    apiKey: process.env.GOOGLE_API_KEY

});



// ====================================================

// 2. HELPER FUNCTIONS (OCR LOGIC)

// ====================================================


/**

 * Normalize NAVER OCR response to our format

 */

function normalizeOCRResponse(naverResponse) {

    const words = [];

    let fullText = "";

    let wordIndex = 0;

    let imageWidth = 0;

    let imageHeight = 0;


    if (naverResponse.images && naverResponse.images.length > 0) {

        const image = naverResponse.images[0];


        imageWidth = image.width ||

                     image.inferResult?.width ||

                     image.convertedImageInfo?.width || 0;

        imageHeight = image.height ||

                      image.inferResult?.height ||

                      image.convertedImageInfo?.height || 0;


        if (imageWidth === 0 || imageHeight === 0) {

            let maxX = 0;

            let maxY = 0;

            if (image.fields) {

                image.fields.forEach((field) => {

                    const vertices = field.boundingPoly.vertices;

                    vertices.forEach((v) => {

                        maxX = Math.max(maxX, v.x);

                        maxY = Math.max(maxY, v.y);

                    });

                });

            }

            imageWidth = Math.ceil(maxX);

            imageHeight = Math.ceil(maxY);

        }


        console.log(`[OCR] NAVER processed image at: ${imageWidth}x${imageHeight}`);


        if (image.fields) {

            image.fields.forEach((field) => {

                const text = field.inferText;

                const vertices = field.boundingPoly.vertices;

                const bbox = {

                    x1: vertices[0].x, y1: vertices[0].y,

                    x2: vertices[1].x, y2: vertices[1].y,

                    x3: vertices[2].x, y3: vertices[2].y,

                    x4: vertices[3].x, y4: vertices[3].y

                };


                words.push({

                    text: text,

                    bbox: bbox,

                    index: wordIndex++

                });


                fullText += text + " ";

            });

        }

    }


    return {

        text: fullText.trim(),

        words: words,

        imageWidth: imageWidth,

        imageHeight: imageHeight

    };

}


function calculateTiming(text) {

    const words = text.split(/\s+/).filter(w => w.length > 0);

    const timings = [];

    let currentTime = 0;


    words.forEach((word, index) => {

        const duration = 300 + (word.length * 50);

        timings.push({

            word: word,

            index: index,

            startMs: currentTime,

            endMs: currentTime + duration

        });

        currentTime += duration;

    });

    return timings;

}


function deleteFile(filePath) {

    try {

        if (fs.existsSync(filePath)) {

            fs.unlinkSync(filePath);

        }

    } catch (err) {

        console.error("Error deleting file:", err);

    }

}


// ====================================================

// 3. OCR / PDF / CROP ENDPOINTS (CŨ)

// ====================================================


// POST /api/ocr/extract

app.post("/api/ocr/extract", upload.any(), async (req, res) => {

    let filePath = null;

    try {

        console.log("📥 Received OCR request");

        const imageFile = req.files?.find(f => f.fieldname === "image");

        if (!imageFile) return res.status(400).json({ success: false, error: "No image file provided" });


        filePath = imageFile.path;

        const imageBuffer = fs.readFileSync(filePath);

        let format = "png";

        if (imageFile.mimetype === "application/pdf") format = "pdf";

        else if (imageFile.mimetype.includes("jpeg") || imageFile.mimetype.includes("jpg")) format = "jpg";


        const ocrResponse = await axios.post(

            process.env.NAVER_OCR_URL,

            {

                version: "V2",

                requestId: `req_${Date.now()}`,

                timestamp: Date.now(),

                images: [{ format: format, name: imageFile.originalname || "document", data: imageBuffer.toString("base64") }]

            },

            { headers: { "X-OCR-SECRET": process.env.NAVER_OCR_SECRET, "Content-Type": "application/json" } }

        );


        const normalized = normalizeOCRResponse(ocrResponse.data);

        deleteFile(filePath);


        res.json({ success: true, data: normalized, message: "OCR completed successfully" });


    } catch (err) {

        console.error("❌ OCR Error:", err.response?.data || err.message);

        if (filePath) deleteFile(filePath);

        res.status(500).json({ success: false, error: "OCR failed", message: err.response?.data || err.message });

    }

});


// POST /api/pdf/extract

app.post("/api/pdf/extract", upload.any(), async (req, res) => {

    let filePath = null;

    try {

        console.log("📥 Received PDF extract request");

        const pdfFile = req.files?.find(f => f.fieldname === "pdf" || f.fieldname === "image");

        if (!pdfFile) return res.status(400).json({ success: false, error: "No PDF file provided" });


        filePath = pdfFile.path;

        const pdfBuffer = fs.readFileSync(filePath);


        const ocrResponse = await axios.post(

            process.env.NAVER_OCR_URL,

            {

                version: "V2",

                requestId: `req_pdf_${Date.now()}`,

                timestamp: Date.now(),

                images: [{ format: "pdf", name: pdfFile.originalname || "document", data: pdfBuffer.toString("base64") }]

            },

            { headers: { "X-OCR-SECRET": process.env.NAVER_OCR_SECRET, "Content-Type": "application/json" } }

        );


        const normalized = normalizeOCRResponse(ocrResponse.data);

        deleteFile(filePath);

        res.json({ success: true, data: normalized, message: "PDF OCR completed successfully" });


    } catch (err) {

        console.error("❌ PDF OCR Error:", err.response?.data || err.message);

        if (filePath) deleteFile(filePath);

        res.status(500).json({ success: false, error: "PDF OCR failed", message: err.response?.data || err.message });

    }

});


// POST /api/ocr/crop

app.post("/api/ocr/crop", upload.any(), async (req, res) => {

    let filePath = null;

    let croppedPath = null;

    try {

        const imageFile = req.files?.find(f => f.fieldname === "image");

        if (!imageFile) return res.status(400).json({ success: false, error: "No image file provided" });


        const { x, y, width, height } = req.body;

        if (!x || !y || !width || !height) {

            deleteFile(imageFile.path);

            return res.status(400).json({ success: false, error: "Missing crop coordinates" });

        }


        filePath = imageFile.path;

        const croppedBuffer = await sharp(filePath)

            .extract({ left: parseInt(x), top: parseInt(y), width: parseInt(width), height: parseInt(height) })

            .toBuffer();


        croppedPath = `${filePath}_cropped.png`;

        fs.writeFileSync(croppedPath, croppedBuffer);


        const ocrResponse = await axios.post(

            process.env.NAVER_OCR_URL,

            {

                version: "V2",

                requestId: `req_crop_${Date.now()}`,

                timestamp: Date.now(),

                images: [{ format: "png", name: "cropped", data: croppedBuffer.toString("base64") }]

            },

            { headers: { "X-OCR-SECRET": process.env.NAVER_OCR_SECRET, "Content-Type": "application/json" } }

        );


        const normalized = normalizeOCRResponse(ocrResponse.data);

        deleteFile(filePath);

        deleteFile(croppedPath);

        res.json({ success: true, data: normalized, message: "OCR crop completed successfully" });


    } catch (err) {

        console.error("OCR Crop Error:", err.response?.data || err.message);

        if (filePath) deleteFile(filePath);

        if (croppedPath) deleteFile(croppedPath);

        res.status(500).json({ success: false, error: "OCR crop failed", message: err.response?.data || err.message });

    }

});


// ====================================================

// 4. TTS ENDPOINTS (CŨ)

// ====================================================


// POST /api/tts/synthesize

app.post("/api/tts/synthesize", async (req, res) => {

    try {

        console.log("🔊 Received TTS request");

        const { text, voice, speaker } = req.body;

        if (!text) return res.status(400).json({ success: false, error: "No text provided" });


        const speakerName = voice || speaker || "nara";

        const ttsResponse = await axios.post(

            process.env.NAVER_TTS_URL,

            { text: text, speaker: speakerName, volume: 0, speed: 0, pitch: 0, format: "mp3" },

            {

                headers: {

                    "X-NCP-APIGW-API-KEY-ID": process.env.NAVER_CLIENT_ID,

                    "X-NCP-APIGW-API-KEY": process.env.NAVER_CLIENT_SECRET,

                    "Content-Type": "application/x-www-form-urlencoded"

                },

                responseType: "arraybuffer"

            }

        );


        const audioBase64 = Buffer.from(ttsResponse.data).toString("base64");

        res.json({ success: true, data: { audio: audioBase64, format: "mp3" } });


    } catch (err) {

        console.error("❌ TTS Error:", err.response?.data || err.message);

        res.status(500).json({ success: false, error: "TTS failed", message: err.message });

    }

});


// POST /api/tts/timing

app.post("/api/tts/timing", async (req, res) => {

    try {

        const { text } = req.body;

        if (!text) return res.status(400).json({ success: false, error: "No text provided" });


        const timings = calculateTiming(text);

        res.json({ success: true, data: { timings: timings } });

    } catch (err) {

        res.status(500).json({ success: false, error: "Timing calculation failed", message: err.message });

    }

});


// ====================================================

// 5. RAG ENDPOINTS (MỚI - GEMINI + PINECONE)

// ====================================================


// API: Nạp tài liệu (Ingest)

app.post('/api/rag/ingest', async (req, res) => {

    try {

        console.log("🧠 Received RAG Ingest request");

        const { text } = req.body;

        if (!text) return res.status(400).send("Missing text");


        const splitter = new RecursiveCharacterTextSplitter({ chunkSize: 500, chunkOverlap: 50 });

        const docs = await splitter.createDocuments([text]);

        

        console.log(`   Splitting into ${docs.length} chunks and embedding...`);

        await PineconeStore.fromDocuments(docs, embeddings, { pineconeIndex: pineconeIndex });


        console.log("✅ Ingest Success");

        res.json({ success: true, message: "Đã nạp kiến thức vào Pinecone với Gemini!" });

    } catch (error) {

        console.error("❌ RAG Ingest Error:", error);

        res.status(500).json({ error: error.message });

    }

});


// API: Hỏi đáp (Ask)

app.post('/api/rag/ask', async (req, res) => {

    try {

        console.log("❓ Received RAG Ask request");

        const { question } = req.body;

        if (!question) return res.status(400).send("Missing question");


        // Load vector store từ Index đã có

        const vectorStore = await PineconeStore.fromExistingIndex(embeddings, { pineconeIndex: pineconeIndex });

        

        // Tạo chain hỏi đáp

        const chain = RetrievalQAChain.fromLLM(llm, vectorStore.asRetriever({ k: 3 }), {

            returnSourceDocuments: true

        });


        console.log(`   Asking Gemini: "${question}"`);

        const response = await chain.call({ query: question });

        

        console.log("✅ Answer generated");

        res.json({ answer: response.text, source: response.sourceDocuments });

    } catch (error) {

        console.error("❌ RAG Ask Error:", error);

        res.status(500).json({ error: error.message });

    }

});

app.post('/api/summary', async (req, res) => {
    try {
        console.log("📝 Summarizing...");
        const { text } = req.body;
        if (!text) return res.status(400).json({ error: "Missing text to summarize" });

        const content = text;

        const systemInstruction = `
            You are an intelligent OCR Text Summarizer specializing in mobile screen capture analysis. The content below was extracted from a screenshot and may include:
            1. OCR errors (typos, bad line breaks).
            2. UI/Navigation artifacts (menu names, app names, buttons, timestamp, navigation bar text).

            Your task is three-fold:
            1. **Filter and Clean:** Identify and ignore text clearly belonging to UI elements (e.g., 'Settings', 'Back', '10:30 AM', app names, navigation titles).
            2. **Reconstruct:** Correct OCR errors and restore the logical flow of the main body text.
            3. **Summarize:** Summarize the clean, reconstructed main body text concisely, aiming for a length suitable for the specified maximum tokens.

            Output ONLY the final summary text, without any introductory phrases, commentary, or Markdown formatting.
        `;

        const finalContent = `${systemInstruction}\n\n--- TEXT TO SUMMARIZE ---\n${content}`;

        const result = await llm.invoke(finalContent);
        const summary = result.content;

        console.log("✅ Summary generated");
        res.json({ success: true, summary: summary });

    } catch (error) {
        console.error("❌ Summary Error:", error);
        res.status(500).json({ error: error.message });
    }
});



// ====================================================

// START SERVER

// ====================================================

const PORT = process.env.PORT || 3000;

app.listen(PORT, '0.0.0.0', () => {

    console.log(`🚀 Server running on port ${PORT}`);

    console.log(`\n--- OLD APIs ---`);

    console.log(`📝 OCR: POST /api/ocr/extract`);

    console.log(`🔊 TTS: POST /api/tts/synthesize`);

    console.log(`\n--- NEW RAG APIs ---`);

    console.log(`🧠 Ingest: POST /api/rag/ingest`);

    console.log(`❓ Ask:    POST /api/rag/ask`);

    console.log(`\n--- NEW SUMMARY API ---`);

    console.log(`🧠 Ingest: POST /api/summary`);


    console.log(`\n--- DEMO UI ---`);

    console.log(`🏠 OCR Demo: http://localhost:${PORT}/`);

    console.log(`🤖 RAG Demo: http://localhost:${PORT}/rag.html`);

}); 