package com.example.voicereaderapp.domain.model.utp

import android.graphics.Rect

/**
 * Identifies the origin/source of text in the UniversalText model.
 *
 * ACCESSIBILITY: text extracted from Android Accessibility Service (UI elements).
 * OCR: text extracted from images or PDFs by OCR.
 * WEB: text extracted from HTTP(s) pages or web content.
 * HYBRID: merged content from two or more sources.
 * CONTINUOUS_OCR: material extracted from a screen capture stream.
 */
enum class TextSourceType {
    ACCESSIBILITY,
    OCR,
    WEB,
    HYBRID,
    CONTINUOUS_OCR
}

/**
 * Token represents an atomic text unit (usually a word) and its mapping to screen coordinates
 * and character offsets in a parent raw text string.
 *
 * @param text the token text
 * @param position bounding rectangle in screen coordinates
 * @param confidence confidence of the extraction (0..1)
 * @param index index of the token in the token sequence
 * @param sourceType origin of the token
 * @param startIndex inclusive character start position in the associated rawText; -1 if unknown
 * @param endIndex exclusive character end position in the associated rawText; -1 if unknown
 */
data class Token(
    val text: String,
    val position: Rect,
    val confidence: Float = 1.0f,
    val index: Int,
    val sourceType: TextSourceType,
    val startIndex: Int = -1,
    val endIndex: Int = -1
)

/**
 * UniversalText is the canonical representation of extracted text in the UTP.
 * It contains the full raw text, token list with positions and character offsets, and metadata.
 */
data class UniversalText(
    val rawText: String,
    val tokens: List<Token>,
    val positions: List<Rect>,
    val sourceType: TextSourceType,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: TextMetadata = TextMetadata()
)

/**
 * Metadata describing the extracted UniversalText, such as language, confidence and document/page information.
 */
data class TextMetadata(
    val title: String? = null,
    val url: String? = null,
    val language: String = "vi",
    val confidence: Float = 1.0f,
    val pageNumber: Int? = null,
    val totalPages: Int? = null,
    val author: String? = null,
    val extractionDuration: Long = 0L
)

/**
 * TextDetectionResult models the result returned by a detector – Success includes a UniversalText, Error wraps exceptions,
 * and Empty indicates no text extracted.
 */
sealed class TextDetectionResult {
    data class Success(val universalText: UniversalText) : TextDetectionResult()
    data class Error(val message: String, val throwable: Throwable? = null) : TextDetectionResult()
    object Empty : TextDetectionResult()
}

/**
 * Settings controlling behavior of text detection operations.
 */
data class TextDetectionConfig(
    val enableAccessibility: Boolean = true,
    val enableOCR: Boolean = true,
    val enableWebFetch: Boolean = true,
    val enableContinuousOCR: Boolean = false,
    val continuousOCRInterval: Long = 2000L, // 2 seconds
    val ocrLanguages: List<String> = listOf("vi", "en"),
    val mergeStrategy: MergeStrategy = MergeStrategy.SMART
)

/**
 * Strategy for merging text from multiple sources
 */
enum class MergeStrategy {
    ACCESSIBILITY_FIRST,  // Prefer accessibility over OCR
    OCR_FIRST,           // Prefer OCR over accessibility
    SMART,               // Use confidence-based merging
    PARALLEL             // Use all sources in parallel
}

/**
 * Text chunk for processing large documents
 */
data class TextChunk(
    val text: String,
    val startIndex: Int,
    val endIndex: Int,
    val tokens: List<Token>
)
