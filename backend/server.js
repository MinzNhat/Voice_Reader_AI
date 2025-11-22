require("dotenv").config();
const express = require("express");
const multer = require("multer");
const axios = require("axios");
const fs = require("fs");
const path = require("path");
const cors = require("cors");

// --- IMPORTS CHO RAG & AI (GEMINI + PINECONE) ---
const { Pinecone } = require('@pinecone-database/pinecone');
const { PineconeStore } = require('@langchain/pinecone');
const { ChatGoogleGenerativeAI, GoogleGenerativeAIEmbeddings } = require('@langchain/google-genai');
const { RetrievalQAChain } = require('langchain/chains');
const { RecursiveCharacterTextSplitter } = require('langchain/text_splitter');
const { PromptTemplate } = require("@langchain/core/prompts");
const { TaskType } = require("@google/generative-ai");

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// Serve Demo UI
app.get("/", (req, res) => {
    const indexPath = path.join(__dirname, "public", "index.html");
    if (fs.existsSync(indexPath)) res.sendFile(indexPath);
    else res.send("<h1>Voice Reader API is Running 🚀</h1><p>Please create public/index.html to see the demo.</p>");
});

// Health check
app.get("/health", (req, res) => res.json({ status: "ok", timestamp: Date.now() }));

// Multer config
const upload = multer({ dest: "uploads/" });
if (!fs.existsSync("uploads")) fs.mkdirSync("uploads");

// ====================================================
// 1. CONFIGURATION (AI SERVICES)
// ====================================================

// Pinecone (Vector DB)
const pinecone = new Pinecone({ apiKey: process.env.PINECONE_API_KEY });
const pineconeIndex = pinecone.Index(process.env.PINECONE_INDEX_NAME);

// Embedding Model (Dùng để vector hóa văn bản)
const embeddings = new GoogleGenerativeAIEmbeddings({
    model: "text-embedding-004", // Model mới nhất
    taskType: TaskType.RETRIEVAL_DOCUMENT, // Bắt buộc khi ingest
    title: "Document",
    apiKey: process.env.GOOGLE_API_KEY
});

// Chat Model (Dùng để trả lời câu hỏi & Tóm tắt)
const llm = new ChatGoogleGenerativeAI({
    model: "gemini-2.0-flash", // Nhanh và Free
    maxOutputTokens: 2048,
    apiKey: process.env.GOOGLE_API_KEY
});

// ====================================================
// 2. HELPER FUNCTIONS
// ====================================================

function deleteFile(filePath) {
    try { if (fs.existsSync(filePath)) fs.unlinkSync(filePath); } 
    catch (err) { console.error("Error deleting file:", err); }
}

function normalizeOCRResponse(naverResponse) {
    let fullText = "";
    if (naverResponse.images?.[0]?.fields) {
        naverResponse.images[0].fields.forEach(field => {
            fullText += field.inferText + " ";
        });
    }
    return { text: fullText.trim() };
}

// ====================================================
// 3. API ENDPOINTS
// ====================================================

// --- [OCR] Chuyển Ảnh -> Chữ ---
app.post("/api/ocr/extract", upload.any(), async (req, res) => {
    let filePath = null;
    try {
        console.log("📷 Received OCR request");
        const imageFile = req.files?.find(f => f.fieldname === "image");
        if (!imageFile) return res.status(400).json({ error: "No image file provided" });

        filePath = imageFile.path;
        const imageBuffer = fs.readFileSync(filePath);
        
        // Call Naver OCR
        const ocrResponse = await axios.post(
            process.env.NAVER_OCR_URL,
            {
                version: "V2",
                requestId: `req_${Date.now()}`,
                timestamp: Date.now(),
                images: [{ format: "png", name: "doc", data: imageBuffer.toString("base64") }]
            },
            { headers: { "X-OCR-SECRET": process.env.NAVER_OCR_SECRET, "Content-Type": "application/json" } }
        );

        const normalized = normalizeOCRResponse(ocrResponse.data);
        deleteFile(filePath);
        res.json({ success: true, data: normalized });

    } catch (err) {
        console.error("❌ OCR Error:", err.message);
        if (filePath) deleteFile(filePath);
        res.status(500).json({ success: false, error: err.message });
    }
});

// --- [TTS] Chuyển Chữ -> Giọng Nói ---
app.post("/api/tts/synthesize", async (req, res) => {
    try {
        console.log("🔊 Received TTS request");
        const { text, speaker = "nara" } = req.body;
        if (!text) return res.status(400).json({ error: "No text provided" });

        const ttsResponse = await axios.post(
            process.env.NAVER_TTS_URL,
            { text, speaker, volume: 0, speed: 0, pitch: 0, format: "mp3" },
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
        console.error("❌ TTS Error:", err.message);
        res.status(500).json({ success: false, error: err.message });
    }
});

// --- [RAG] Nạp kiến thức (Ingest) ---
app.post('/api/rag/ingest', async (req, res) => {
    try {
        console.log("🧠 RAG Ingesting...");
        const { text } = req.body;
        if (!text) return res.status(400).json({ error: "Missing text" });

        const splitter = new RecursiveCharacterTextSplitter({ chunkSize: 500, chunkOverlap: 50 });
        const docs = await splitter.createDocuments([text]);
        
        // Xóa dữ liệu cũ nếu cần (Optional: await pineconeIndex.deleteAll();)
        await PineconeStore.fromDocuments(docs, embeddings, { pineconeIndex: pineconeIndex });

        console.log("✅ Ingest Success");
        res.json({ success: true, message: "Knowledge ingested successfully!" });
    } catch (error) {
        console.error("❌ Ingest Error:", error);
        res.status(500).json({ error: error.message });
    }
});

// --- [RAG] Hỏi đáp (Chatbot) ---
app.post('/api/rag/ask', async (req, res) => {
    try {
        console.log("❓ RAG Asking...");
        const { question } = req.body;
        if (!question) return res.status(400).json({ error: "Missing question" });

        const vectorStore = await PineconeStore.fromExistingIndex(embeddings, { pineconeIndex: pineconeIndex });
        
        // Prompt Template (Hybrid Mode: Context + Knowledge)
        const template = `
        You are a helpful AI assistant.
        Use the following context to answer the question. If the answer is not in the context, use your own knowledge.
        
        Context: {context}
        Question: {question}
        Answer:
        `;
        
        const prompt = new PromptTemplate({ template, inputVariables: ["context", "question"] });

        const chain = RetrievalQAChain.fromLLM(llm, vectorStore.asRetriever({ k: 3 }), {
            returnSourceDocuments: true,
            prompt: prompt
        });

        const response = await chain.call({ query: question });
        res.json({ answer: response.text, source: response.sourceDocuments });
    } catch (error) {
        console.error("❌ Ask Error:", error);
        res.status(500).json({ error: error.message });
    }
});

// --- [SUMMARY] Tóm tắt văn bản (Mới) ---
app.post('/api/summary', async (req, res) => {
    try {
        console.log("📝 Summarizing...");
        const { text } = req.body;
        if (!text) return res.status(400).json({ error: "Missing text to summarize" });

        // Gọi trực tiếp Gemini để tóm tắt
        const prompt = `Please summarize the following text concisely in Vietnamese:\n\n"${text}"`;
        
        const result = await llm.invoke(prompt); // Gọi LLM trực tiếp, không cần qua Chain
        const summary = result.content; // Lấy nội dung trả về

        console.log("✅ Summary generated");
        res.json({ success: true, summary: summary });

    } catch (error) {
        console.error("❌ Summary Error:", error);
        res.status(500).json({ error: error.message });
    }
});

// --- Placeholder cho các route cũ (để Client không crash) ---
const notImplemented = (req, res) => res.status(501).json({ error: "Not implemented in this simplified server" });
app.post("/api/pdf/extract", notImplemented);
app.post("/api/ocr/crop", notImplemented);
app.post("/api/tts/timing", notImplemented);

// ====================================================
// START SERVER
// ====================================================
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`🚀 Server running on port ${PORT}`);
    console.log(`   - OCR:     POST /api/ocr/extract`);
    console.log(`   - TTS:     POST /api/tts/synthesize`);
    console.log(`   - Ingest:  POST /api/rag/ingest`);
    console.log(`   - Ask:     POST /api/rag/ask`);
    console.log(`   - Summary: POST /api/summary`);
});