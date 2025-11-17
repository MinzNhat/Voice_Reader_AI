# Voice Reader AI - Backend API

Backend service cho ứng dụng Voice Reader AI - hỗ trợ OCR, TTS, và xử lý PDF cho người khiếm thị.

## 📋 Tính năng

- **OCR (Optical Character Recognition)**: Trích xuất văn bản từ ảnh
  - Google Cloud Vision API
  - Tesseract.js (fallback)
- **TTS (Text-to-Speech)**: Chuyển văn bản thành giọng nói
  - Google Cloud Text-to-Speech
  - ElevenLabs (optional)
  - OpenAI (optional)
- **PDF Processing**: Xử lý file PDF
  - Trích xuất văn bản
  - Metadata extraction
  - Per-page extraction

## 🛠️ Tech Stack

- **Runtime**: Node.js (≥18.0.0)
- **Framework**: Express.js
- **Language**: TypeScript
- **APIs**: Google Cloud Vision, Google Cloud TTS, ElevenLabs, OpenAI
- **Libraries**: Multer, Joi, Tesseract.js, pdf-parse

## 📦 Cài đặt

```bash
# Clone repository
cd Voice_Reader_AI/backend

# Install dependencies
npm install

# Copy environment variables
cp .env.example .env

# Configure .env file with your API keys
```

## ⚙️ Cấu hình

Tạo file `.env` với các biến sau:

```env
# Server
NODE_ENV=development
PORT=3000

# Google Cloud
GOOGLE_APPLICATION_CREDENTIALS=./credentials/google-cloud-key.json
GOOGLE_PROJECT_ID=your-project-id

# Optional: Alternative TTS providers
ELEVENLABS_API_KEY=your-key
OPENAI_API_KEY=your-key

# File Upload
MAX_FILE_SIZE=10485760
UPLOAD_DIR=./uploads
```

### Google Cloud Setup

1. Tạo project tại [Google Cloud Console](https://console.cloud.google.com)
2. Enable APIs:
   - Cloud Vision API
   - Cloud Text-to-Speech API
3. Tạo Service Account và download JSON key
4. Đặt key vào `credentials/google-cloud-key.json`

## 🚀 Chạy ứng dụng

```bash
# Development mode với hot reload
npm run dev

# Build production
npm run build

# Run production
npm start
```

Server sẽ chạy tại `http://localhost:3000`

## 📚 API Documentation

### Base URL

```
http://localhost:3000/api
```

### Health Check

```
GET /health
```

---

### OCR Endpoints

#### 1. Extract Text from Image

```http
POST /api/ocr/extract
Content-Type: multipart/form-data

Body:
- image: File (JPEG, PNG)
- language: string (default: 'vi')
- engine: 'google' | 'tesseract' (default: 'google')

Response:
{
  "success": true,
  "data": {
    "text": "Extracted text...",
    "confidence": 95,
    "language": "vi"
  }
}
```

#### 2. Batch OCR

```http
POST /api/ocr/batch
Content-Type: multipart/form-data

Body:
- images: File[] (max 10 files)
- language: string (default: 'vi')
- engine: 'google' | 'tesseract'

Response:
{
  "success": true,
  "data": [
    {
      "filename": "image1.jpg",
      "text": "...",
      "confidence": 95
    }
  ]
}
```

---

### TTS Endpoints

#### 1. Synthesize Text to Speech

```http
POST /api/tts/synthesize
Content-Type: application/json

Body:
{
  "text": "Text to convert",
  "language": "vi-VN",
  "voice": "vi-VN-Standard-A",
  "speed": 1.0,
  "pitch": 0,
  "engine": "google"
}

Response:
{
  "success": true,
  "data": {
    "audioContent": "base64_encoded_audio",
    "duration": 5.2
  }
}
```

#### 2. Stream Audio

```http
POST /api/tts/stream
Content-Type: application/json

Body: (same as synthesize)

Response: audio/mp3 stream
```

#### 3. Get Available Voices

```http
GET /api/tts/voices?language=vi-VN&engine=google

Response:
{
  "success": true,
  "data": [
    {
      "name": "vi-VN-Standard-A",
      "gender": "FEMALE",
      "language": "vi-VN"
    }
  ]
}
```

---

### PDF Endpoints

#### 1. Extract Text from PDF

```http
POST /api/pdf/extract
Content-Type: multipart/form-data

Body:
- pdf: File
- language: string (default: 'vi')

Response:
{
  "success": true,
  "data": {
    "text": "Extracted text...",
    "pages": 10,
    "metadata": {
      "title": "Document Title",
      "author": "Author Name"
    }
  }
}
```

#### 2. Extract Specific Page

```http
POST /api/pdf/extract-page
Content-Type: multipart/form-data

Body:
- pdf: File
- page: number
- language: string

Response:
{
  "success": true,
  "data": {
    "text": "Page text...",
    "page": 1
  }
}
```

#### 3. Get PDF Metadata

```http
POST /api/pdf/metadata
Content-Type: multipart/form-data

Body:
- pdf: File

Response:
{
  "success": true,
  "data": {
    "pages": 10,
    "title": "Title",
    "author": "Author"
  }
}
```

---

## 🔒 Security

- **Rate Limiting**: 100 requests per 15 minutes
- **API Key Authentication**: Optional (set `API_KEY` in .env)
- **File Upload Limits**: Max 10MB per file
- **Helmet**: Security headers
- **CORS**: Configurable origins

## 📁 Cấu trúc thư mục

```
backend/
├── src/
│   ├── index.ts              # Entry point
│   ├── middleware/
│   │   ├── error.middleware.ts
│   │   ├── notFound.middleware.ts
│   │   └── auth.middleware.ts
│   ├── routes/
│   │   ├── ocr.route.ts
│   │   ├── tts.route.ts
│   │   └── pdf.route.ts
│   ├── services/
│   │   ├── ocr.service.ts
│   │   ├── tts.service.ts
│   │   └── pdf.service.ts
│   ├── utils/
│   │   ├── storage.ts
│   │   ├── response.ts
│   │   └── validation.ts
│   └── types/
│       └── index.ts
├── uploads/                  # Temporary file storage
├── credentials/              # Google Cloud credentials
├── .env                      # Environment variables
├── .env.example              # Environment template
├── .gitignore
├── package.json
└── tsconfig.json
```

## 🔗 Tích hợp với Android App

### Retrofit Interface Example

```kotlin
interface VoiceReaderApi {
    @Multipart
    @POST("api/ocr/extract")
    suspend fun extractText(
        @Part image: MultipartBody.Part,
        @Part("language") language: RequestBody
    ): OcrResponse

    @POST("api/tts/synthesize")
    suspend fun synthesizeText(
        @Body request: TtsRequest
    ): TtsResponse

    @Multipart
    @POST("api/pdf/extract")
    suspend fun extractPdf(
        @Part pdf: MultipartBody.Part,
        @Part("language") language: RequestBody
    ): PdfResponse
}
```

### Android Usage Example

```kotlin
// OCR
val imageFile = File(imagePath)
val requestFile = imageFile.asRequestBody("image/*".toMediaType())
val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
val languageBody = "vi".toRequestBody("text/plain".toMediaType())

val ocrResult = api.extractText(imagePart, languageBody)

// TTS
val ttsRequest = TtsRequest(
    text = "Xin chào",
    language = "vi-VN",
    speed = 1.0f
)
val ttsResult = api.synthesizeText(ttsRequest)
val audioBytes = Base64.decode(ttsResult.data.audioContent, Base64.DEFAULT)
```

## 🧪 Testing

```bash
# Run tests
npm test

# Run linter
npm run lint

# Format code
npm run format
```

## 📝 Coding Conventions

### TypeScript Style

- Use `interface` for data structures
- Use `type` for unions and complex types
- Async/await over Promises
- Strict mode enabled

### Naming Conventions

- Files: `kebab-case.ts`
- Classes: `PascalCase`
- Functions/Variables: `camelCase`
- Constants: `UPPER_SNAKE_CASE`

### Error Handling

- Use `AppError` class for custom errors
- Always include status codes
- Log errors to console in development

### API Response Format

```typescript
// Success
{
  "success": true,
  "message": "Optional message",
  "data": { ... }
}

// Error
{
  "success": false,
  "message": "Error message",
  "errors": [ ... ]
}
```

## 🚧 Roadmap

- [ ] Add WebSocket support for real-time TTS streaming
- [ ] Implement caching for OCR results
- [ ] Add support for more TTS providers
- [ ] Database integration for request logging
- [ ] Admin dashboard
- [ ] Docker support

## 📄 License

MIT

## 👥 Contributors

MinzNhat

## 📞 Support

For issues, please create an issue in the GitHub repository.
