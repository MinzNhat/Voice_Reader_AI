require("dotenv").config();
const express = require("express");
const multer = require("multer");
const axios = require("axios");
const fs = require("fs");
const path = require("path");
const cors = require("cors");
const sharp = require("sharp");

const app = express();
app.use(cors());
app.use(express.json());

// Serve test HTML
app.get("/", (req, res) => {
    res.sendFile(path.join(__dirname, "index.html"));
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
// HELPER FUNCTIONS
// ====================================================

/**
 * Normalize NAVER OCR response to our format
 * CRITICAL: Include image dimensions so Android can scale coordinates properly
 */
function normalizeOCRResponse(naverResponse) {
    const words = [];
    let fullText = "";
    let wordIndex = 0;
    let imageWidth = 0;
    let imageHeight = 0;

    // NAVER OCR returns images array with fields array
    if (naverResponse.images && naverResponse.images.length > 0) {
        const image = naverResponse.images[0];

        // CRITICAL: Capture the actual image dimensions NAVER used for OCR
        imageWidth = image.width ||
                     image.inferResult?.width ||
                     image.convertedImageInfo?.width || 0;
        imageHeight = image.height ||
                      image.inferResult?.height ||
                      image.convertedImageInfo?.height || 0;

        // If still 0, calculate from bounding box coordinates
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

                // Convert vertices to bbox format
                const bbox = {
                    x1: vertices[0].x,
                    y1: vertices[0].y,
                    x2: vertices[1].x,
                    y2: vertices[1].y,
                    x3: vertices[2].x,
                    y3: vertices[2].y,
                    x4: vertices[3].x,
                    y4: vertices[3].y
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

/**
 * Calculate approximate timing for TTS
 */
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

/**
 * Delete file safely
 */
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
// ENDPOINT 1: POST /api/ocr/extract
// ====================================================
// Use upload.any() to accept both file and text fields (image + language)
app.post("/api/ocr/extract", upload.any(), async (req, res) => {
    let filePath = null;

    try {
        console.log("📥 Received OCR request");
        console.log("  Files received:", req.files?.length || 0);
        console.log("  Body:", req.body);

        // Get the image file (upload.any() puts files in req.files array)
        const imageFile = req.files?.find(f => f.fieldname === "image");

        if (!imageFile) {
            return res.status(400).json({
                success: false,
                error: "No image file provided"
            });
        }

        console.log("  File:", imageFile.originalname);
        console.log("  Size:", imageFile.size, "bytes");
        console.log("  Language:", req.body.language || "not specified");

        filePath = imageFile.path;
        const imageBuffer = fs.readFileSync(filePath);

        let format = "png";
        if (imageFile.mimetype === "application/pdf") {
            format = "pdf";
        } else if (imageFile.mimetype.includes("jpeg") || imageFile.mimetype.includes("jpg")) {
            format = "jpg";
        }

        const ocrResponse = await axios.post(
            process.env.NAVER_OCR_URL,
            {
                version: "V2",
                requestId: `req_${Date.now()}`,
                timestamp: Date.now(),
                images: [
                    {
                        format: format,
                        name: imageFile.originalname || "document",
                        data: imageBuffer.toString("base64")
                    }
                ]
            },
            {
                headers: {
                    "X-OCR-SECRET": process.env.NAVER_OCR_SECRET,
                    "Content-Type": "application/json"
                }
            }
        );

        const normalized = normalizeOCRResponse(ocrResponse.data);
        console.log("✅ OCR Success, text length:", normalized.text.length);
        console.log("📦 Words found:", normalized.words.length);

        deleteFile(filePath);

        // WRAPPED RESPONSE FOR ANDROID
        res.json({
            success: true,
            data: normalized,
            message: "OCR completed successfully"
        });

    } catch (err) {
        console.error("❌ OCR Error:", err.response?.data || err.message);

        if (filePath) {
            deleteFile(filePath);
        }

        res.status(500).json({
            success: false,
            error: "OCR failed",
            message: err.response?.data || err.message
        });
    }
});

// ====================================================
// ENDPOINT 2: POST /api/pdf/extract (Alias for OCR)
// ====================================================
app.post("/api/pdf/extract", upload.any(), async (req, res) => {
    let filePath = null;

    try {
        console.log("📥 Received PDF extract request");
        const pdfFile = req.files?.find(f => f.fieldname === "pdf" || f.fieldname === "image");

        if (!pdfFile) {
            return res.status(400).json({
                success: false,
                error: "No PDF file provided"
            });
        }

        console.log("  File:", pdfFile.originalname);
        console.log("  Size:", pdfFile.size, "bytes");

        filePath = pdfFile.path;
        const pdfBuffer = fs.readFileSync(filePath);

        const ocrResponse = await axios.post(
            process.env.NAVER_OCR_URL,
            {
                version: "V2",
                requestId: `req_pdf_${Date.now()}`,
                timestamp: Date.now(),
                images: [
                    {
                        format: "pdf",
                        name: pdfFile.originalname || "document",
                        data: pdfBuffer.toString("base64")
                    }
                ]
            },
            {
                headers: {
                    "X-OCR-SECRET": process.env.NAVER_OCR_SECRET,
                    "Content-Type": "application/json"
                }
            }
        );

        const normalized = normalizeOCRResponse(ocrResponse.data);
        console.log("✅ PDF OCR Success, text length:", normalized.text.length);

        deleteFile(filePath);

        res.json({
            success: true,
            data: normalized,
            message: "PDF OCR completed successfully"
        });

    } catch (err) {
        console.error("❌ PDF OCR Error:", err.response?.data || err.message);

        if (filePath) {
            deleteFile(filePath);
        }

        res.status(500).json({
            success: false,
            error: "PDF OCR failed",
            message: err.response?.data || err.message
        });
    }
});

// ====================================================
// ENDPOINT 3: POST /api/ocr/crop
// ====================================================
app.post("/api/ocr/crop", upload.any(), async (req, res) => {
    let filePath = null;
    let croppedPath = null;

    try {
        const imageFile = req.files?.find(f => f.fieldname === "image");

        if (!imageFile) {
            return res.status(400).json({
                success: false,
                error: "No image file provided"
            });
        }

        const { x, y, width, height } = req.body;

        if (!x || !y || !width || !height) {
            deleteFile(imageFile.path);
            return res.status(400).json({
                success: false,
                error: "Missing crop coordinates (x, y, width, height)"
            });
        }

        filePath = imageFile.path;

        const croppedBuffer = await sharp(filePath)
            .extract({
                left: parseInt(x),
                top: parseInt(y),
                width: parseInt(width),
                height: parseInt(height)
            })
            .toBuffer();

        croppedPath = `${filePath}_cropped.png`;
        fs.writeFileSync(croppedPath, croppedBuffer);

        const ocrResponse = await axios.post(
            process.env.NAVER_OCR_URL,
            {
                version: "V2",
                requestId: `req_crop_${Date.now()}`,
                timestamp: Date.now(),
                images: [
                    {
                        format: "png",
                        name: "cropped",
                        data: croppedBuffer.toString("base64")
                    }
                ]
            },
            {
                headers: {
                    "X-OCR-SECRET": process.env.NAVER_OCR_SECRET,
                    "Content-Type": "application/json"
                }
            }
        );

        const normalized = normalizeOCRResponse(ocrResponse.data);

        deleteFile(filePath);
        deleteFile(croppedPath);

        res.json({
            success: true,
            data: normalized,
            message: "OCR crop completed successfully"
        });

    } catch (err) {
        console.error("OCR Crop Error:", err.response?.data || err.message);

        if (filePath) deleteFile(filePath);
        if (croppedPath) deleteFile(croppedPath);

        res.status(500).json({
            success: false,
            error: "OCR crop failed",
            message: err.response?.data || err.message
        });
    }
});

// ====================================================
// ENDPOINT 4: POST /api/tts/synthesize
// ====================================================
app.post("/api/tts/synthesize", async (req, res) => {
    try {
        console.log("🔊 Received TTS request");
        const { text, voice, speaker } = req.body;

        if (!text) {
            return res.status(400).json({
                success: false,
                error: "No text provided"
            });
        }

        const speakerName = voice || speaker || "nara";
        console.log("  Speaker:", speakerName);
        console.log("  Text length:", text.length);

        const ttsResponse = await axios.post(
            process.env.NAVER_TTS_URL,
            {
                text: text,
                speaker: speakerName,
                volume: 0,
                speed: 0,
                pitch: 0,
                format: "mp3"
            },
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
        console.log("✅ TTS Success, audio size:", audioBase64.length);

        res.json({
            success: true,
            data: {
                audio: audioBase64,
                format: "mp3"
            }
        });

    } catch (err) {
        console.error("❌ TTS Error:", err.response?.data || err.message);
        res.status(500).json({
            success: false,
            error: "TTS failed",
            message: err.message
        });
    }
});

// ====================================================
// ENDPOINT 5: POST /api/tts/timing
// ====================================================
app.post("/api/tts/timing", async (req, res) => {
    try {
        const { text } = req.body;

        if (!text) {
            return res.status(400).json({
                success: false,
                error: "No text provided"
            });
        }

        const timings = calculateTiming(text);

        res.json({
            success: true,
            data: {
                timings: timings
            }
        });

    } catch (err) {
        console.error("Timing Error:", err.message);
        res.status(500).json({
            success: false,
            error: "Timing calculation failed",
            message: err.message
        });
    }
});

// ====================================================
// START SERVER
// ====================================================
const PORT = process.env.PORT || 3000;
app.listen(PORT, '0.0.0.0', () => {
    console.log(`🚀 Server running on port ${PORT}`);
    console.log(`📝 OCR endpoint: POST http://localhost:${PORT}/api/ocr/extract`);
    console.log(`📄 PDF endpoint: POST http://localhost:${PORT}/api/pdf/extract`);
    console.log(`✂️  Crop endpoint: POST http://localhost:${PORT}/api/ocr/crop`);
    console.log(`🔊 TTS endpoint: POST http://localhost:${PORT}/api/tts/synthesize`);
    console.log(`⏱️  Timing endpoint: POST http://localhost:${PORT}/api/tts/timing`);
    console.log(`🏥 Health check: GET http://localhost:${PORT}/health`);
});
