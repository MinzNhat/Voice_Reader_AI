package com.example.voicereaderapp.data.repository.utp.backend

import android.graphics.Bitmap
import com.example.voicereaderapp.domain.model.utp.UniversalText

/**
 * Backend OCR client interface
 * Implementations should call a remote OCR endpoint and return a UniversalText model.
 */
interface BackendOcrClient {
    suspend fun sendImageForOcr(bitmap: Bitmap): Result<UniversalText>
}
