package com.example.voicereaderapp.data.repository.utp.backend

import android.graphics.Bitmap
import com.example.voicereaderapp.domain.model.utp.UniversalText

/**
 * Default no-op BackendOcrClient implementation.
 * Returns a failure Result indicating OCR must be handled by a real backend implementation.
 */
class NoopBackendOcrClient : BackendOcrClient {
    override suspend fun sendImageForOcr(bitmap: Bitmap): Result<UniversalText> {
        return Result.failure(IllegalStateException("Backend OCR client not configured. Replace NoopBackendOcrClient with a proper implementation that calls your OCR service."))
    }
}
