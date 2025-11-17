package com.example.voicereaderapp.data.repository.utp

import android.graphics.Bitmap
import android.content.Context
import android.os.Build
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.example.voicereaderapp.domain.model.utp.TextDetectionConfig
import com.example.voicereaderapp.domain.model.utp.TextDetectionResult
import com.example.voicereaderapp.domain.model.utp.TextMetadata
import com.example.voicereaderapp.domain.model.utp.TextSourceType
import com.example.voicereaderapp.domain.model.utp.Token
import com.example.voicereaderapp.domain.model.utp.UniversalText
import com.example.voicereaderapp.domain.repository.utp.DetectionContext
import com.example.voicereaderapp.domain.repository.utp.OcrSource
import com.example.voicereaderapp.domain.repository.utp.TextDetectorRepository
import kotlinx.coroutines.Dispatchers
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import com.example.voicereaderapp.data.repository.utp.backend.BackendOcrClient
import javax.inject.Inject
import kotlinx.coroutines.flow.flowOn

/**
 * Implementation of [TextDetectorRepository] providing accessibility and OCR delegation
 * features for the Universal Text Pipeline (UTP).
 *
 * Notes:
 * - OCR calls are delegated to a backend client via [BackendOcrClient]. Local OCR libraries are not used.
 * - Accessibility extraction traverses the node tree and produces `UniversalText` with token positions
 *   and character offsets when available.
 */
class TextDetectorRepositoryImpl @Inject constructor(
    private val context: Context,
    private val backendOcrClient: BackendOcrClient
) : TextDetectorRepository {

    // No local text recognizer is configured (OCR is delegated to a backend service)
    private var continuousOcrJob: Job? = null

    /**
     * A. Extract from Accessibility Service
     */
    override suspend fun extractFromAccessibility(rootNodeInfo: Any?): TextDetectionResult {
        return withContext(Dispatchers.Default) {
            try {
                if (rootNodeInfo == null) return@withContext TextDetectionResult.Empty

                val root = when (rootNodeInfo) {
                    is AccessibilityNodeInfo -> rootNodeInfo
                    else -> return@withContext TextDetectionResult.Error("Unsupported rootNodeInfo type: ${rootNodeInfo::class}")
                }

                val tokens = mutableListOf<Token>()
                val positions = mutableListOf<Rect>()
                var currentIndex = 0
                val MAX_DEPTH = 40
                val MAX_TOKENS = 600
                val rawBuilder = StringBuilder()
                var currentCharPos = 0
                val punctuationOnly = Regex("^[\\p{Punct}]+$")

                fun processNode(node: AccessibilityNodeInfo?, depth: Int = 0) {
                    if (node == null) return
                    if (depth > MAX_DEPTH || tokens.size >= MAX_TOKENS) return
                    try {
                        // Privacy: skip password or editable password fields
                        try { if (node.isPassword) return } catch (_: Throwable) {}

                        val text = node.text?.toString()?.trim().takeIf { !it.isNullOrBlank() }
                            ?: node.contentDescription?.toString()?.trim()
                        if (!text.isNullOrBlank()) {
                            val bounds = Rect()
                            node.getBoundsInScreen(bounds)
                            // Tokenize text by lines then words; add char offset mapping
                            val lines = text.split('\n')
                            for ((li, line) in lines.withIndex()) {
                                val words = line.split(Regex("\\s+"))
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                if (words.isEmpty()) {
                                    if (li < lines.size - 1) {
                                        rawBuilder.append('\n'); currentCharPos += 1
                                    }
                                    continue
                                }

                                if (words.size == 1) {
                                    val filtered = words[0]
                                    if (filtered.length > 1 && !punctuationOnly.matches(filtered)) {
                                        val start = currentCharPos
                                        rawBuilder.append(filtered).append(' ')
                                        currentCharPos += filtered.length + 1
                                        val token = Token(
                                            text = filtered,
                                            position = Rect(bounds),
                                            confidence = 1.0f,
                                            index = currentIndex++,
                                            sourceType = TextSourceType.ACCESSIBILITY,
                                            startIndex = start,
                                            endIndex = start + filtered.length
                                        )
                                        tokens.add(token)
                                        positions.add(bounds)
                                    }
                                } else {
                                    val totalChars = words.sumOf { it.length }.coerceAtLeast(1)
                                    var xLeft = bounds.left
                                    val top = bounds.top
                                    val bottom = bounds.bottom
                                    val fullWidth = bounds.width()
                                    for (wrd in words) {
                                        val filtered = wrd
                                        if (filtered.length <= 1 || punctuationOnly.matches(filtered)) {
                                            // skip
                                            continue
                                        }
                                        val w = ((filtered.length.toFloat() / totalChars.toFloat()) * fullWidth).toInt().coerceAtLeast(1)
                                        val wordRect = Rect(xLeft, top, (xLeft + w).coerceAtMost(bounds.right), bottom)
                                        val start = currentCharPos
                                        rawBuilder.append(filtered).append(' ')
                                        currentCharPos += filtered.length + 1
                                        val token = Token(
                                            text = filtered,
                                            position = wordRect,
                                            confidence = 1.0f,
                                            index = currentIndex++,
                                            sourceType = TextSourceType.ACCESSIBILITY,
                                            startIndex = start,
                                            endIndex = start + filtered.length
                                        )
                                        tokens.add(token)
                                        positions.add(wordRect)
                                        xLeft += w
                                    }
                                }

                                if (li < lines.size - 1) {
                                    rawBuilder.append('\n'); currentCharPos += 1
                                }
                            }
                        }
                    } catch (ignored: Exception) {
                        // Ignore node-specific traversal errors and continue
                    }

                    // Recurse through children
                    try {
                        val childCount = node.childCount
                        for (i in 0 until childCount) {
                            val child = node.getChild(i)
                            if (child != null) {
                                processNode(child, depth + 1)
                                child.recycle()
                            }
                        }
                    } catch (ignored: Exception) {}
                }

                // Process root node
                processNode(root)

                if (tokens.isEmpty()) return@withContext TextDetectionResult.Empty

                val rawText = rawBuilder.toString().trim()

                val universalText = UniversalText(
                    rawText = rawText,
                    tokens = tokens,
                    positions = positions,
                    sourceType = TextSourceType.ACCESSIBILITY,
                    metadata = TextMetadata(
                        extractionDuration = System.currentTimeMillis()
                    )
                )
                TextDetectionResult.Success(universalText)
            } catch (e: Exception) {
                TextDetectionResult.Error("Accessibility extraction failed", e)
            }
        }
    }

    /**
     * B. Extract from OCR (ML Kit)
     */
    override suspend fun extractFromOCR(
        source: OcrSource,
        config: TextDetectionConfig
    ): TextDetectionResult {
        return withContext(Dispatchers.Default) {
            try {
                Log.d("TextDetectorRepositoryImpl", "extractFromOCR: Attempting backend OCR for source=$source")
                // Delegate to backend OCR client
                when (source) {
                    is OcrSource.Image -> {
                        try {
                            val result = backendOcrClient.sendImageForOcr(source.bitmap)
                            return@withContext result.fold(
                                onSuccess = { universal -> TextDetectionResult.Success(universal) },
                                onFailure = { err -> TextDetectionResult.Error("Backend OCR failed: ${err.message}", err) }
                            )
                        } catch (e: Exception) {
                            return@withContext TextDetectionResult.Error("Backend OCR call failed", e)
                        }
                    }
                    is OcrSource.Uri, is OcrSource.FilePath -> {
                        // For URIs or file paths the caller should load the bitmap and pass as Image, or implement
                        // a backend endpoint that accepts URIs. For now: return informative error.
                        return@withContext TextDetectionResult.Error("URI/File OCR requires pre-upload; backend call not implemented in NoopBackendOcrClient")
                    }
                    is OcrSource.Screenshot -> {
                        return@withContext TextDetectionResult.Error("Screenshot OCR delegated to backend; not implemented")
                    }
                }
            } catch (e: Exception) {
                TextDetectionResult.Error("OCR extraction failed", e)
            }
        }
    }

    // Image pre-processing and thresholding operations were removed as this implementation
    // delegates heavy OCR and preprocessing to the backend. If future clients require local
    // preprocessing, re-introduce explicit helper APIs behind feature flags or separate utility classes.

    /**
     * C. Continuous Screen OCR
     */
    override fun startContinuousOCR(
        interval: Long,
        config: TextDetectionConfig
    ): Flow<TextDetectionResult> = flow {
        while (true) {
            try {
                // Continuous OCR is intentionally a no-op in this implementation. Implementers should
                // provide a platform-specific screenshot/capture flow and call extractFromOCR as needed.
                emit(TextDetectionResult.Empty)

                delay(interval)
            } catch (e: Exception) {
                emit(TextDetectionResult.Error("Continuous OCR failed", e))
                break
            }
        }
    }.flowOn(Dispatchers.Default)

    override fun stopContinuousOCR() {
        continuousOcrJob?.cancel()
        continuousOcrJob = null
    }

    /**
     * D. Extract from Web Content
     */
    override suspend fun extractFromWeb(
        url: String,
        config: TextDetectionConfig
    ): TextDetectionResult {
        return withContext(Dispatchers.IO) {
            try {
                // Web extraction is not implemented in the local repository; this is an extension point
                // for future implementations (server-side scraping or client-side parsing).
                TextDetectionResult.Empty
            } catch (e: Exception) {
                TextDetectionResult.Error("Web extraction failed", e)
            }
        }
    }

    /**
     * Auto-detect best source
     */
    override suspend fun extractAuto(
        context: DetectionContext,
        config: TextDetectionConfig
    ): TextDetectionResult {
        // Priority/flow recommended by UTP
        // 1) Accessibility: prefer if available (most accurate for UI text & bounding boxes)
        // 2) OCR: for images/PDF (delegated to backend in this app)
        // 3) Continuous OCR: when requiresContinuous is true
        // 4) Web fetch: if URL (HTTP/HTTPS)

        // 1) Accessibility
        if (context.hasAccessibility) {
            val result = extractFromAccessibility()
            if (result is TextDetectionResult.Success) return result
        }

        // 2) OCR (images / local files) — may be delegated to backend
        if (context.hasBitmap != null) {
            val imageResult = extractFromOCR(OcrSource.Image(context.hasBitmap), config)
            if (imageResult is TextDetectionResult.Success) return imageResult
        }

        // 3) Continuous OCR (if requested by context)
        if (context.requiresContinuous) {
            val flow = startContinuousOCR(config.continuousOCRInterval, config)
            try {
                // Collect only the first emission for auto-detect purposes
                var collected: TextDetectionResult? = null
                flow.collect { r ->
                    if (r is TextDetectionResult.Success) {
                        collected = r
                        return@collect
                    }
                }
                if (collected is TextDetectionResult.Success) return collected as TextDetectionResult
            } catch (_: Exception) {}
        }

        // 4) Web fetch if a URL is present
        if (context.hasUrl != null) {
            try {
                val u = android.net.Uri.parse(context.hasUrl)
                val scheme = u.scheme?.lowercase()
                Log.d("TextDetectorRepositoryImpl", "extractAuto: hasUrl=${context.hasUrl} scheme=$scheme")
                if (scheme == "http" || scheme == "https") {
                    val webResult = extractFromWeb(context.hasUrl, config)
                    if (webResult is TextDetectionResult.Success) return webResult
                } else if (scheme == "content" || scheme == "file" || scheme == "android.resource") {
                    // For local files we need OCR (delegated to backend), try it
                    when (val result = extractFromOCR(OcrSource.Uri(u), config)) {
                        is TextDetectionResult.Success -> return result
                        else -> {
                            // continue
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("TextDetectorRepositoryImpl", "extractAuto: URL parse/fetch failed: ${e.message}")
            }
        }

        // No result
        return TextDetectionResult.Empty
    }
}
