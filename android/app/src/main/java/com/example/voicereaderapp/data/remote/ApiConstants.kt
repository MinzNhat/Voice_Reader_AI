package com.example.voicereaderapp.data.remote

object ApiConstants {
    // For Android Emulator: use 10.0.2.2 to access host machine's localhost
    // For Physical Device: use your computer's IP address (e.g., "192.168.1.100")
//    const val BASE_URL = "http://49.50.134.27:3000/"
    const val BASE_URL = "http://192.168.1.197:3000/"


    const val API_KEY = "your-secure-api-key-here" // Match backend .env

    // Endpoints
    object Endpoints {
        const val HEALTH = "health"
        const val OCR_EXTRACT = "api/ocr/extract"
        const val TTS_SYNTHESIZE = "api/tts/synthesize"
//        const val TTS_STREAM = "api/tts/stream"
//        const val TTS_VOICES = "api/tts/voices"
//        const val PDF_EXTRACT = "api/pdf/extract"
//        const val PDF_EXTRACT_PAGE = "api/pdf/extract-page"
//        const val PDF_METADATA = "api/pdf/metadata"

        // 🔥 RAG (MỚI - Cần thêm 2 dòng này)
        const val RAG_INGEST = "api/rag/ingest"
        const val RAG_ASK = "api/rag/ask"

        // 🔥 SUMMARY (MỚI - Nếu ông muốn dùng tính năng tóm tắt)
        const val SUMMARY = "api/summary"
    }
}
