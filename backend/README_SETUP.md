# Voice Reader AI - Backend Setup Guide

Complete backend API for PDF/Image OCR and Text-to-Speech using NAVER CLOVA APIs.

## Quick Start

### 1. Install Dependencies

```bash
cd backend
npm install
```

### 2. Configure Environment Variables

Copy `.env.example` to `.env`:

```bash
cp .env.example .env
```

Edit `.env` with your NAVER CLOVA credentials:

```env
# NAVER CLOVA OCR
NAVER_OCR_URL=https://YOUR_OCR_APIGW_INVOKE_URL/YOUR_DOMAIN_ID
NAVER_OCR_SECRET=your_ocr_secret_key_here

# NAVER CLOVA TTS Premium
NAVER_TTS_URL=https://naveropenapi.apigw.ntruss.com/tts-premium/v1/tts
NAVER_CLIENT_ID=your_client_id_here
NAVER_CLIENT_SECRET=your_client_secret_here
```

### 3. Get NAVER CLOVA Credentials

#### OCR API:
1. Go to https://www.ncloud.com/product/aiService/ocr
2. Create a new OCR project
3. Copy the API URL and Secret Key

#### TTS Premium API:
1. Go to https://www.ncloud.com/product/aiService/clovaSpeech
2. Enable CLOVA Voice (TTS Premium)
3. Copy the Client ID and Client Secret

### 4. Run the Server

```bash
npm start
# or for development with auto-reload
npm run dev
```

Server runs on `http://localhost:3000`

### 5. Test the API

Open browser and go to: `http://localhost:3000`

You'll see the API Test Panel with all endpoints ready to test.

---

## API Endpoints

### 1. POST `/ocr`
Extract text and bounding boxes from PDF or image.

**Input:**
- `multipart/form-data` with `file` field
- Supports: PDF, JPG, PNG

**Output:**
```json
{
  "text": "full extracted text...",
  "words": [
    {
      "text": "word",
      "bbox": {
        "x1": 10, "y1": 20,
        "x2": 50, "y2": 20,
        "x3": 50, "y3": 40,
        "x4": 10, "y4": 40
      },
      "index": 0
    }
  ]
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:3000/ocr \
  -F "file=@document.pdf"
```

---

### 2. POST `/ocr/crop`
Extract text from a cropped region of an image.

**Input:**
- `multipart/form-data` with `file` field
- Body parameters: `x`, `y`, `width`, `height`

**Output:** Same as `/ocr`

**cURL Example:**
```bash
curl -X POST http://localhost:3000/ocr/crop \
  -F "file=@image.jpg" \
  -F "x=10" \
  -F "y=20" \
  -F "width=300" \
  -F "height=200"
```

---

### 3. POST `/tts`
Convert text to speech using NAVER TTS Premium.

**Input:**
```json
{
  "text": "Text to convert to speech",
  "speaker": "nara"
}
```

Available speakers: `nara`, `jinho`, `clara`, `matt`, `shinji`, `meimei`, `liangliang`, `jose`, `carmen`

**Output:**
```json
{
  "audio": "base64_encoded_mp3_audio..."
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:3000/tts \
  -H "Content-Type: application/json" \
  -d '{"text":"Hello world","speaker":"nara"}'
```

---

### 4. POST `/tts/timing`
Calculate approximate word timing for real-time highlighting.

**Input:**
```json
{
  "text": "Hello world this is a test"
}
```

**Output:**
```json
{
  "timings": [
    { "word": "Hello", "index": 0, "startMs": 0, "endMs": 320 },
    { "word": "world", "index": 1, "startMs": 320, "endMs": 620 }
  ]
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:3000/tts/timing \
  -H "Content-Type: application/json" \
  -d '{"text":"Hello world"}'
```

---

## Architecture

```
backend/
├── src/
│   ├── server.js         # Main server with all 4 endpoints
│   └── index.html        # API test panel
├── uploads/              # Temp file storage (auto-created)
├── package.json
├── .env.example
└── .env                  # Your credentials (not in git)
```

## Key Features

- **Normalized OCR Response**: Converts NAVER OCR format to clean `{text, words}` format
- **Auto File Cleanup**: Temp files deleted after processing
- **Crop Support**: Extract text from specific image regions
- **Base64 Audio**: Returns audio in base64 for easy embedding
- **Smart Timing**: Estimates word timing based on character count
- **Error Handling**: Detailed error messages for debugging

## Notes

- PDF files larger than 10 pages may need special handling
- OCR works best with clear, high-contrast text
- TTS timing is approximate - fine-tune for production
- All temp files are automatically deleted after processing
- CORS enabled for frontend integration

## Troubleshooting

**Error: "OCR failed"**
- Check NAVER_OCR_URL and NAVER_OCR_SECRET in `.env`
- Verify your NAVER Cloud OCR project is active

**Error: "TTS failed"**
- Check NAVER_CLIENT_ID and NAVER_CLIENT_SECRET in `.env`
- Ensure CLOVA Voice (TTS Premium) is enabled

**Error: "ENOENT: no such file or directory"**
- Run `mkdir uploads` in the backend directory
- Or restart the server (it auto-creates the folder)

**Sharp installation issues:**
```bash
npm install --platform=linux --arch=x64 sharp
```

---

## Ready for Android Integration

All endpoints return the exact format specified in CLAUDE.md. The Android app can now call these APIs using Retrofit.

Example Retrofit service:
```kotlin
interface VoiceReaderAPI {
    @Multipart
    @POST("ocr")
    suspend fun performOCR(@Part file: MultipartBody.Part): OCRResponse

    @POST("tts")
    suspend fun generateTTS(@Body request: TTSRequest): TTSResponse

    @POST("tts/timing")
    suspend fun getTimings(@Body request: TimingRequest): TimingResponse
}
```

---

For questions or issues, check the main CLAUDE.md file.
