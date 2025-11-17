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
        // NAVER stores dimensions in different possible locations:
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
        console.log(`[OCR] convertedImageInfo:`, JSON.stringify(image.convertedImageInfo, null, 2));

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
        // CRITICAL: Return dimensions so Android can scale coordinates
        imageWidth: imageWidth,
        imageHeight: imageHeight
    };
}

/**
 * Calculate approximate timing for TTS
 * Average speaking rate: ~150 words per minute = 2.5 words/sec = 400ms per word
 */
function calculateTiming(text) {
    const words = text.split(/\s+/).filter(w => w.length > 0);
    const timings = [];
    let currentTime = 0;

    words.forEach((word, index) => {
        // Estimate duration based on word length
        // Base: 300ms + 50ms per character (approximate)
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
// ENDPOINT 1: POST /ocr
// ====================================================
app.post("/ocr", upload.single("file"), async (req, res) => {
    let filePath = null;

    try {
        if (!req.file) {
            return res.status(400).json({ error: "No file provided" });
        }

        filePath = req.file.path;
        const imageBuffer = fs.readFileSync(filePath);

        // Determine format from mimetype
        let format = "png";
        if (req.file.mimetype === "application/pdf") {
            format = "pdf";
        } else if (req.file.mimetype.includes("jpeg") || req.file.mimetype.includes("jpg")) {
            format = "jpg";
        }

        // Call NAVER CLOVA OCR API
        const ocrResponse = await axios.post(
            process.env.NAVER_OCR_URL,
            {
                version: "V2",
                requestId: `req_${Date.now()}`,
                timestamp: Date.now(),
                images: [
                    {
                        format: format,
                        name: req.file.originalname || "document",
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

        // Normalize response
        const normalized = normalizeOCRResponse(ocrResponse.data);

        // Delete temp file
        deleteFile(filePath);

        res.json(normalized);

    } catch (err) {
        console.error("OCR Error:", err.response?.data || err.message);

        // Clean up file on error
        if (filePath) {
            deleteFile(filePath);
        }

        res.status(500).json({
            error: "OCR failed",
            details: err.response?.data || err.message
        });
    }
});

// ====================================================
// ENDPOINT 2: POST /ocr/crop
// ====================================================
app.post("/ocr/crop", upload.single("file"), async (req, res) => {
    let filePath = null;
    let croppedPath = null;

    try {
        if (!req.file) {
            return res.status(400).json({ error: "No file provided" });
        }

        // Get crop coordinates from body
        const { x, y, width, height } = req.body;

        if (!x || !y || !width || !height) {
            deleteFile(req.file.path);
            return res.status(400).json({ error: "Missing crop coordinates (x, y, width, height)" });
        }

        filePath = req.file.path;

        // Crop image using sharp
        const croppedBuffer = await sharp(filePath)
            .extract({
                left: parseInt(x),
                top: parseInt(y),
                width: parseInt(width),
                height: parseInt(height)
            })
            .toBuffer();

        // Save cropped image temporarily
        croppedPath = `${filePath}_cropped.png`;
        fs.writeFileSync(croppedPath, croppedBuffer);

        // Now perform OCR on cropped image
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

        // Normalize response
        const normalized = normalizeOCRResponse(ocrResponse.data);

        // Clean up
        deleteFile(filePath);
        deleteFile(croppedPath);

        res.json(normalized);

    } catch (err) {
        console.error("OCR Crop Error:", err.response?.data || err.message);

        // Clean up files on error
        if (filePath) deleteFile(filePath);
        if (croppedPath) deleteFile(croppedPath);

        res.status(500).json({
            error: "OCR crop failed",
            details: err.response?.data || err.message
        });
    }
});

// ====================================================
// ENDPOINT 3: POST /tts
// ====================================================
app.post("/tts", async (req, res) => {
    try {
        const { text, speaker } = req.body;

        if (!text) {
            return res.status(400).json({ error: "No text provided" });
        }

        // Default speaker if not provided
        const speakerName = speaker || "matt"; // NAVER TTS Premium default voice

        // Call NAVER TTS Premium API
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

        // Convert audio to base64
        const audioBase64 = Buffer.from(ttsResponse.data).toString("base64");

        res.json({
            audio: audioBase64
        });

    } catch (err) {
        console.error("TTS Error:", err.response?.data || err.message);
        res.status(500).json({
            error: "TTS failed",
            details: err.message
        });
    }
});

// ====================================================
// ENDPOINT 4: POST /tts/timing
// ====================================================
app.post("/tts/timing", async (req, res) => {
    try {
        const { text } = req.body;

        if (!text) {
            return res.status(400).json({ error: "No text provided" });
        }

        // Calculate approximate timing
        const timings = calculateTiming(text);

        res.json({
            timings: timings
        });

    } catch (err) {
        console.error("Timing Error:", err.message);
        res.status(500).json({
            error: "Timing calculation failed",
            details: err.message
        });
    }
});

// ====================================================
// START SERVER
// ====================================================
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`🚀 Server running on port ${PORT}`);
    console.log(`📝 OCR endpoint: POST http://localhost:${PORT}/ocr`);
    console.log(`✂️  Crop endpoint: POST http://localhost:${PORT}/ocr/crop`);
    console.log(`🔊 TTS endpoint: POST http://localhost:${PORT}/tts`);
    console.log(`⏱️  Timing endpoint: POST http://localhost:${PORT}/tts/timing`);
});
