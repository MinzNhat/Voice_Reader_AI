package com.example.voicereaderapp.domain.repository.utp

import android.graphics.Bitmap
import android.net.Uri
import com.example.voicereaderapp.domain.model.utp.TextDetectionConfig
import com.example.voicereaderapp.domain.model.utp.TextDetectionResult
import com.example.voicereaderapp.domain.model.utp.UniversalText
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for all text detection entrypoints (Accessibility, OCR, Web, Continuous OCR).
 * Implementations should create a [UniversalText] and return the result wrapped in [TextDetectionResult].
 */
interface TextDetectorRepository {
    /**
     * Extract text from an Accessibility node tree (usually provided by AccessibilityService).
     * @param rootNodeInfo the root AccessibilityNodeInfo or null
     * @return [TextDetectionResult] containing [UniversalText] or an Empty/Error result.
     */
    suspend fun extractFromAccessibility(
        rootNodeInfo: Any? = null // Conforming callers should pass AccessibilityNodeInfo
    ): TextDetectionResult

    /**
     * Extract text from images, URIs or file paths via OCR (may be delegated to a backend).
     */
    suspend fun extractFromOCR(
        source: OcrSource,
        config: TextDetectionConfig = TextDetectionConfig()
    ): TextDetectionResult

    /**
     * Start a continuous OCR stream (e.g., screen capture). Emits detection results periodically.
     */
    fun startContinuousOCR(
        interval: Long = 2000L,
        config: TextDetectionConfig = TextDetectionConfig()
    ): Flow<TextDetectionResult>

    /**
     * Stops continuous OCR stream.
     */
    fun stopContinuousOCR()

    /**
     * Extract text from web content (HTTP/HTTPS URLs) and return a [UniversalText].
     */
    suspend fun extractFromWeb(
        url: String,
        config: TextDetectionConfig = TextDetectionConfig()
    ): TextDetectionResult

    /**
     * Auto-detect the best source and perform extraction according to [DetectionContext].
     */
    suspend fun extractAuto(
        context: DetectionContext,
        config: TextDetectionConfig = TextDetectionConfig()
    ): TextDetectionResult
}

/**
 * Represents the different types of OCR sources supported by the detector.
 */
sealed class OcrSource {
    data class Image(val bitmap: Bitmap) : OcrSource()
    data class Uri(val uri: android.net.Uri) : OcrSource()
    data class FilePath(val path: String) : OcrSource()
    data class Screenshot(val timestamp: Long = System.currentTimeMillis()) : OcrSource()
}

/**
 * Context used for auto-detection to indicate available inputs.
 */
data class DetectionContext(
    val hasUrl: String? = null,
    val hasBitmap: Bitmap? = null,
    val hasAccessibility: Boolean = false,
    val requiresContinuous: Boolean = false
)
