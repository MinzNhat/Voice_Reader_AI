package com.example.voicereaderapp.data.remote

object ApiConstants {
    // For Android Emulator: use 10.0.2.2 to access host machine's localhost
    // For Physical Device: use your computer's IP address (e.g., "192.168.1.100")
    const val BASE_URL = "http://192.168.1.19:3000/"

    const val API_KEY = "your-secure-api-key-here" // Match backend .env

    // Endpoints
    object Endpoints {
        const val HEALTH = "health"
        const val OCR_EXTRACT = "api/ocr/extract"
        const val OCR_BATCH = "api/ocr/batch"
        const val TTS_SYNTHESIZE = "api/tts/synthesize"
        const val TTS_STREAM = "api/tts/stream"
        const val TTS_VOICES = "api/tts/voices"
        const val PDF_EXTRACT = "api/pdf/extract"
        const val PDF_EXTRACT_PAGE = "api/pdf/extract-page"
        const val PDF_METADATA = "api/pdf/metadata"
    }
}
