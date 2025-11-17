package com.example.voicereaderapp.domain.utp.test

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.voicereaderapp.domain.model.utp.*
import com.example.voicereaderapp.domain.repository.utp.TextDetectorRepository
import com.example.voicereaderapp.domain.repository.utp.TextNormalizerRepository
import com.example.voicereaderapp.domain.repository.utp.OcrSource
import com.example.voicereaderapp.domain.repository.utp.DetectionContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Mock implementation of TextDetectorRepository for testing
 */
class MockTextDetectorRepository : TextDetectorRepository {
    
    private var mockDetectionResult: TextDetectionResult? = null
    
    fun setMockResult(result: TextDetectionResult) {
        mockDetectionResult = result
    }
    
    override suspend fun extractFromAccessibility(rootNodeInfo: Any?): TextDetectionResult {
        return mockDetectionResult ?: TextDetectionResult.Success(
            UniversalText(
                rawText = "Mock accessibility text",
                tokens = listOf(
                    Token("Mock", Rect(0, 0, 50, 20), 0.95f, 0, TextSourceType.ACCESSIBILITY, startIndex = 0, endIndex = 4),
                    Token("accessibility", Rect(55, 0, 150, 20), 0.95f, 1, TextSourceType.ACCESSIBILITY, startIndex = 5, endIndex = 18),
                    Token("text", Rect(155, 0, 200, 20), 0.95f, 2, TextSourceType.ACCESSIBILITY, startIndex = 19, endIndex = 23)
                ),
                positions = listOf(Rect(0, 0, 200, 20)),
                sourceType = TextSourceType.ACCESSIBILITY
            )
        )
    }
    
    override suspend fun extractFromOCR(
        source: OcrSource,
        config: TextDetectionConfig
    ): TextDetectionResult {
        return mockDetectionResult ?: TextDetectionResult.Success(
            UniversalText(
                rawText = "Mock OCR text",
                tokens = listOf(
                    Token("Mock", Rect(0, 0, 50, 20), 0.90f, 0, TextSourceType.OCR, startIndex = 0, endIndex = 4),
                    Token("OCR", Rect(55, 0, 100, 20), 0.90f, 1, TextSourceType.OCR, startIndex = 5, endIndex = 8),
                    Token("text", Rect(105, 0, 150, 20), 0.90f, 2, TextSourceType.OCR, startIndex = 9, endIndex = 13)
                ),
                positions = listOf(Rect(0, 0, 150, 20)),
                sourceType = TextSourceType.OCR
            )
        )
    }
    
    override fun startContinuousOCR(interval: Long, config: TextDetectionConfig): Flow<TextDetectionResult> {
        return flowOf(
            TextDetectionResult.Success(
                UniversalText(
                    rawText = "Continuous OCR frame",
                    tokens = emptyList(),
                    positions = emptyList(),
                    sourceType = TextSourceType.CONTINUOUS_OCR
                )
            )
        )
    }
    
    override fun stopContinuousOCR() {
        // Mock implementation
    }
    
    override suspend fun extractFromWeb(url: String, config: TextDetectionConfig): TextDetectionResult {
        return mockDetectionResult ?: TextDetectionResult.Success(
            UniversalText(
                rawText = "Mock web content from $url",
                tokens = emptyList(),
                positions = emptyList(),
                sourceType = TextSourceType.WEB,
                metadata = TextMetadata(url = url)
            )
        )
    }
    
    override suspend fun extractAuto(
        context: DetectionContext,
        config: TextDetectionConfig
    ): TextDetectionResult {
        // Auto-detect based on context
        return when {
            context.hasUrl != null -> extractFromWeb(context.hasUrl, config)
            context.hasBitmap != null -> extractFromOCR(OcrSource.Image(context.hasBitmap), config)
            context.hasAccessibility -> extractFromAccessibility()
            else -> TextDetectionResult.Empty
        }
    }
}

/**
 * Mock implementation of TextNormalizerRepository for testing
 */
class MockTextNormalizerRepository : TextNormalizerRepository {
    
    private var mockUniversalText: UniversalText? = null
    
    fun setMockResult(text: UniversalText) {
        mockUniversalText = text
    }
    
    override suspend fun merge(sources: List<UniversalText>): UniversalText {
        val mergedText = sources.joinToString("\n") { it.rawText }
        val allTokens = mutableListOf<Token>()
        var tokenIndex = 0
        
        sources.forEach { source ->
            source.tokens.forEach { token ->
                allTokens.add(token.copy(index = tokenIndex++))
            }
        }
        
        return UniversalText(
            rawText = mergedText,
            tokens = allTokens,
            positions = sources.flatMap { it.positions },
            sourceType = TextSourceType.HYBRID
        )
    }
    
    override suspend fun removeDuplicates(text: UniversalText): UniversalText {
        val uniqueTokens = text.tokens.distinctBy { it.text.lowercase() }
        return text.copy(tokens = uniqueTokens)
    }
    
    override suspend fun normalizeFormatting(text: UniversalText): UniversalText {
        val normalized = text.rawText.trim().replace(Regex("\\s+"), " ")
        return text.copy(rawText = normalized)
    }
    
    override suspend fun orderByReadingSequence(text: UniversalText): UniversalText {
        val ordered = text.tokens.sortedWith(
            compareBy({ it.position.top }, { it.position.left })
        )
        return text.copy(tokens = ordered)
    }
    
    override suspend fun filterNoise(
        text: UniversalText,
        keepHeaders: Boolean,
        keepNavigation: Boolean
    ): UniversalText {
        // Mock: filter out short tokens
        val filtered = text.tokens.filter { it.text.length > 2 }
        return text.copy(tokens = filtered)
    }
    
    override suspend fun tokenize(text: UniversalText, tokenizeBy: com.example.voicereaderapp.domain.repository.utp.TokenizeMode): UniversalText {
        // Already tokenized in mock
        return text
    }
    
    override suspend fun mapPositions(text: UniversalText): UniversalText {
        // Already mapped in mock
        return text
    }
    
    override suspend fun normalize(
        sources: List<UniversalText>,
        config: com.example.voicereaderapp.domain.repository.utp.NormalizationConfig
    ): UniversalText {
        var result = merge(sources)
        
        if (config.removeDuplicates) result = removeDuplicates(result)
        if (config.normalizeFormatting) result = normalizeFormatting(result)
        if (config.orderByReading) result = orderByReadingSequence(result)
        if (config.filterNoise) result = filterNoise(result)
        
        return result
    }
}

/**
 * Test data builder for UTP testing
 */
object UTPTestDataBuilder {
    
    fun createMockToken(
        text: String = "word",
        x: Int = 0,
        y: Int = 0,
        width: Int = 50,
        height: Int = 20,
        confidence: Float = 0.95f,
        index: Int = 0,
        sourceType: TextSourceType = TextSourceType.ACCESSIBILITY
    ,
        startIndex: Int = 0,
        endIndex: Int = 0
    ) = Token(
        text = text,
        position = Rect(x, y, x + width, y + height),
        confidence = confidence,
        index = index,
        sourceType = sourceType
        ,
        startIndex = startIndex,
        endIndex = endIndex
    )
    
    fun createMockUniversalText(
        rawText: String = "Sample text",
        tokens: List<Token>? = null,
        sourceType: TextSourceType = TextSourceType.ACCESSIBILITY
    ): UniversalText {
        val actualTokens = tokens ?: run {
            var cursor = 0
            rawText.split(" ").mapIndexed { index, word ->
                val start = cursor
                val end = cursor + word.length
                cursor = end + 1
                createMockToken(
                    text = word,
                    x = index * 60,
                    index = index,
                    sourceType = sourceType,
                    startIndex = start,
                    endIndex = end
                )
            }
        }
        
        return UniversalText(
            rawText = rawText,
            tokens = actualTokens,
            positions = actualTokens.map { it.position },
            sourceType = sourceType,
            metadata = TextMetadata(
                language = "vi",
                confidence = 0.95f,
                extractionDuration = 10L
            )
        )
    }
    
    fun createMockDetectionResult(
        rawText: String = "Mock text",
        sourceType: TextSourceType = TextSourceType.ACCESSIBILITY
    ): TextDetectionResult {
        return TextDetectionResult.Success(
            createMockUniversalText(rawText, null, sourceType)
        )
    }
    
    fun createMockOcrSource(
        width: Int = 1920,
        height: Int = 1080
    ): OcrSource {
        // Create a simple bitmap
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        return OcrSource.Image(bitmap)
    }
    
    fun createMockDetectionContext(
        hasUrl: String? = null,
        hasBitmap: Bitmap? = null,
        hasAccessibility: Boolean = false,
        requiresContinuous: Boolean = false
    ) = DetectionContext(
        hasUrl = hasUrl,
        hasBitmap = hasBitmap,
        hasAccessibility = hasAccessibility,
        requiresContinuous = requiresContinuous
    )
}

/**
 * Mock TTS Engine for testing highlight synchronization
 */
class MockTTSEngine {
    private val rangeListeners = mutableListOf<(Int, Int) -> Unit>()
    
    fun addRangeListener(listener: (Int, Int) -> Unit) {
        rangeListeners.add(listener)
    }
    
    fun removeRangeListener(listener: (Int, Int) -> Unit) {
        rangeListeners.remove(listener)
    }
    
    suspend fun simulateSpeech(text: String, wordsPerSecond: Int = 3) {
        val words = text.split(" ")
        var charPosition = 0
        
        words.forEach { word ->
            val start = charPosition
            val end = charPosition + word.length
            
            // Notify listeners
            rangeListeners.forEach { it(start, end) }
            
            // Simulate speech delay
            kotlinx.coroutines.delay(1000L / wordsPerSecond)
            
            charPosition = end + 1 // +1 for space
        }
    }
    
    fun stop() {
        rangeListeners.clear()
    }
}

/**
 * Test helper utilities
 */
class UTPTestHelper {
    
    /**
     * Simulate full pipeline: Detection → Normalization
     */
    suspend fun simulatePipeline(
        sourceText: String,
        sourceType: TextSourceType = TextSourceType.ACCESSIBILITY
    ): UniversalText {
        val detector = MockTextDetectorRepository()
        val normalizer = MockTextNormalizerRepository()
        
        // Detection
        val detectionResult = detector.extractFromAccessibility()
        
        // Normalization
        return when (detectionResult) {
            is TextDetectionResult.Success -> {
                normalizer.normalize(listOf(detectionResult.universalText))
            }
            else -> UTPTestDataBuilder.createMockUniversalText(sourceText, null, sourceType)
        }
    }
    
    /**
     * Verify token positions are valid
     */
    fun verifyTokenPositions(text: UniversalText): Boolean {
        return text.tokens.all { token ->
            if (token.startIndex >= 0 && token.endIndex > token.startIndex && token.endIndex <= text.rawText.length) {
                val expected = text.rawText.substring(token.startIndex, token.endIndex)
                expected == token.text
            } else {
                // If char offsets are not available, fallback to bounding box checks
                val substring = try {
                    text.rawText.substring(0, minOf(text.rawText.length, token.position.right / 10))
                } catch (_: Exception) { "" }
                substring.isNotEmpty()
            }
        }
    }
    
    /**
     * Verify bounding boxes are valid
     */
    fun verifyBoundingBoxes(text: UniversalText): Boolean {
        return text.positions.all { rect ->
            rect.left >= 0 && rect.top >= 0 &&
            rect.right > rect.left && rect.bottom > rect.top
        }
    }
    
    /**
     * Find token at character position
     */
    fun findTokenAtPosition(text: UniversalText, charPosition: Int): Token? {
        return text.tokens.find { token ->
            if (token.startIndex >= 0 && token.endIndex > token.startIndex) {
                charPosition in token.startIndex until token.endIndex
            } else {
                false
            }
        }
    }
    
    /**
     * Count words in text
     */
    fun countWords(text: String): Int {
        return text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
    }
}

/**
 * Performance measurement helper
 */
class UTPPerformanceTestHelper {
    
    data class PerformanceMetrics(
        val detectionTimeMs: Long,
        val normalizationTimeMs: Long,
        val totalTimeMs: Long,
        val textLength: Int,
        val tokenCount: Int,
        val throughput: Float // chars per second
    )
    
    suspend fun measurePipelinePerformance(
        detector: TextDetectorRepository,
        normalizer: TextNormalizerRepository,
        context: DetectionContext
    ): PerformanceMetrics {
        val startTime = System.currentTimeMillis()
        
        // Detection phase
        val detectionStart = System.currentTimeMillis()
        val detectionResult = detector.extractAuto(context)
        val detectionTime = System.currentTimeMillis() - detectionStart
        
        // Normalization phase
        val normalizationStart = System.currentTimeMillis()
        val universalText = when (detectionResult) {
            is TextDetectionResult.Success -> {
                normalizer.normalize(listOf(detectionResult.universalText))
            }
            else -> UTPTestDataBuilder.createMockUniversalText()
        }
        val normalizationTime = System.currentTimeMillis() - normalizationStart
        
        val totalTime = System.currentTimeMillis() - startTime
        val throughput = if (totalTime > 0) {
            universalText.rawText.length.toFloat() / totalTime * 1000
        } else 0f
        
        return PerformanceMetrics(
            detectionTimeMs = detectionTime,
            normalizationTimeMs = normalizationTime,
            totalTimeMs = totalTime,
            textLength = universalText.rawText.length,
            tokenCount = universalText.tokens.size,
            throughput = throughput
        )
    }
    
    fun printMetrics(metrics: PerformanceMetrics) {
        println("""
            📊 Performance Metrics:
            ├─ Detection: ${metrics.detectionTimeMs}ms
            ├─ Normalization: ${metrics.normalizationTimeMs}ms
            ├─ Total: ${metrics.totalTimeMs}ms
            ├─ Text Length: ${metrics.textLength} chars
            ├─ Token Count: ${metrics.tokenCount} tokens
            └─ Throughput: ${"%.2f".format(metrics.throughput)} chars/sec
        """.trimIndent())
    }
}
