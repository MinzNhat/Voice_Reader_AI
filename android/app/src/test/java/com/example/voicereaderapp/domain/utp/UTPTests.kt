package com.example.voicereaderapp.domain.utp

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.voicereaderapp.domain.model.utp.*
import com.example.voicereaderapp.domain.repository.utp.*
import com.example.voicereaderapp.domain.utp.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for UTP (Universal Text Pipeline) Architecture
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UTPTests {
    
    private lateinit var mockDetector: MockTextDetectorRepository
    private lateinit var mockNormalizer: MockTextNormalizerRepository
    private lateinit var testHelper: UTPTestHelper
    
    @Before
    fun setup() {
        mockDetector = MockTextDetectorRepository()
        mockNormalizer = MockTextNormalizerRepository()
        testHelper = UTPTestHelper()
    }
    
    // ========================================
    // Layer 1: Text Detection Tests
    // ========================================
    
    @Test
    fun `test accessibility extraction returns valid UniversalText`() = runTest {
        val result = mockDetector.extractFromAccessibility()
        
        assertTrue(result is TextDetectionResult.Success)
        val success = result as TextDetectionResult.Success
        assertEquals(TextSourceType.ACCESSIBILITY, success.universalText.sourceType)
        assertTrue(success.universalText.tokens.isNotEmpty())
    }
    
    @Test
    fun `test OCR extraction with bitmap`() = runTest {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val source = OcrSource.Image(bitmap)
        
        val result = mockDetector.extractFromOCR(source)
        
        assertTrue(result is TextDetectionResult.Success)
        val success = result as TextDetectionResult.Success
        assertEquals(TextSourceType.OCR, success.universalText.sourceType)
    }
    
    @Test
    fun `test web content extraction`() = runTest {
        val url = "https://example.com"
        val result = mockDetector.extractFromWeb(url)
        
        assertTrue(result is TextDetectionResult.Success)
        val success = result as TextDetectionResult.Success
        assertEquals(TextSourceType.WEB, success.universalText.sourceType)
        assertEquals(url, success.universalText.metadata.url)
    }
    
    @Test
    fun `test continuous OCR flow`() = runTest {
        val flow = mockDetector.startContinuousOCR()
        val firstResult = flow.first()
        
        assertTrue(firstResult is TextDetectionResult.Success)
        mockDetector.stopContinuousOCR()
    }
    
    @Test
    fun `test auto-detection with URL context`() = runTest {
        val context = UTPTestDataBuilder.createMockDetectionContext(
            hasUrl = "https://example.com"
        )
        
        val result = mockDetector.extractAuto(context)
        
        assertTrue(result is TextDetectionResult.Success)
        val success = result as TextDetectionResult.Success
        assertEquals(TextSourceType.WEB, success.universalText.sourceType)
    }
    
    // ========================================
    // Layer 2: Text Normalization Tests
    // ========================================
    
    @Test
    fun `test merging multiple text sources`() = runTest {
        val text1 = UTPTestDataBuilder.createMockUniversalText(
            "First text",
            sourceType = TextSourceType.ACCESSIBILITY
        )
        val text2 = UTPTestDataBuilder.createMockUniversalText(
            "Second text",
            sourceType = TextSourceType.OCR
        )
        
        val merged = mockNormalizer.merge(listOf(text1, text2))
        
        assertTrue(merged.rawText.contains("First text"))
        assertTrue(merged.rawText.contains("Second text"))
        assertEquals(TextSourceType.HYBRID, merged.sourceType)
    }
    
    @Test
    fun `test duplicate removal`() = runTest {
        val tokens = listOf(
            UTPTestDataBuilder.createMockToken("hello", index = 0),
            UTPTestDataBuilder.createMockToken("world", index = 1),
            UTPTestDataBuilder.createMockToken("hello", index = 2) // duplicate
        )
        val text = UTPTestDataBuilder.createMockUniversalText(
            "hello world hello",
            tokens
        )
        
        val deduplicated = mockNormalizer.removeDuplicates(text)
        
        assertEquals(2, deduplicated.tokens.size)
        assertEquals("hello", deduplicated.tokens[0].text)
        assertEquals("world", deduplicated.tokens[1].text)
    }
    
    @Test
    fun `test formatting normalization`() = runTest {
        val text = UTPTestDataBuilder.createMockUniversalText(
            "  Hello    World  \n\n  Test  "
        )
        
        val normalized = mockNormalizer.normalizeFormatting(text)
        
        assertEquals("Hello World Test", normalized.rawText)
    }
    
    @Test
    fun `test token ordering by reading sequence`() = runTest {
        val tokens = listOf(
            UTPTestDataBuilder.createMockToken("bottom", x = 0, y = 100, index = 0),
            UTPTestDataBuilder.createMockToken("top", x = 0, y = 0, index = 1)
        )
        val text = UTPTestDataBuilder.createMockUniversalText(
            "bottom top",
            tokens
        )
        
        val ordered = mockNormalizer.orderByReadingSequence(text)
        
        assertEquals("top", ordered.tokens[0].text)
        assertEquals("bottom", ordered.tokens[1].text)
    }
    
    @Test
    fun `test noise filtering`() = runTest {
        val tokens = listOf(
            UTPTestDataBuilder.createMockToken("hello", index = 0),
            UTPTestDataBuilder.createMockToken("ab", index = 1), // too short
            UTPTestDataBuilder.createMockToken("world", index = 2)
        )
        val text = UTPTestDataBuilder.createMockUniversalText(
            "hello ab world",
            tokens
        )
        
        val filtered = mockNormalizer.filterNoise(text)
        
        assertEquals(2, filtered.tokens.size)
        assertFalse(filtered.tokens.any { it.text == "ab" })
    }
    
    @Test
    fun `test full normalization pipeline`() = runTest {
        val sources = listOf(
            UTPTestDataBuilder.createMockUniversalText("Text one"),
            UTPTestDataBuilder.createMockUniversalText("Text two")
        )
        
        val normalized = mockNormalizer.normalize(sources)
        
        assertNotNull(normalized)
        assertEquals(TextSourceType.HYBRID, normalized.sourceType)
    }
    
    // ========================================
    // UniversalText Model Tests
    // ========================================
    
    @Test
    fun `test UniversalText creation with all fields`() {
        val text = UTPTestDataBuilder.createMockUniversalText(
            "Sample text",
            sourceType = TextSourceType.ACCESSIBILITY
        )
        
        assertEquals("Sample text", text.rawText)
        assertEquals(TextSourceType.ACCESSIBILITY, text.sourceType)
        assertNotNull(text.tokens)
        assertNotNull(text.positions)
        assertTrue(text.timestamp > 0)
    }
    
    @Test
    fun `test Token model with all properties`() {
        val token = UTPTestDataBuilder.createMockToken(
            text = "hello",
            x = 10,
            y = 20,
            width = 50,
            height = 30,
            confidence = 0.95f,
            index = 0
        )
        
        assertEquals("hello", token.text)
        assertEquals(10, token.position.left)
        assertEquals(20, token.position.top)
        assertEquals(60, token.position.right) // x + width
        assertEquals(50, token.position.bottom) // y + height
        assertEquals(0.95f, token.confidence, 0.01f)
        assertEquals(0, token.index)
    }
    
    @Test
    fun `test TextMetadata fields`() {
        val metadata = TextMetadata(
            title = "Test Document",
            url = "https://example.com",
            language = "vi",
            confidence = 0.9f,
            pageNumber = 1,
            totalPages = 10
        )
        
        assertEquals("Test Document", metadata.title)
        assertEquals("https://example.com", metadata.url)
        assertEquals("vi", metadata.language)
        assertEquals(0.9f, metadata.confidence, 0.01f)
        assertEquals(1, metadata.pageNumber)
        assertEquals(10, metadata.totalPages)
    }
    
    @Test
    fun `test TextDetectionResult sealed class`() {
        val successResult = TextDetectionResult.Success(
            UTPTestDataBuilder.createMockUniversalText()
        )
        assertTrue(successResult is TextDetectionResult.Success)
        
        val errorResult = TextDetectionResult.Error("Test error")
        assertTrue(errorResult is TextDetectionResult.Error)
        assertEquals("Test error", errorResult.message)
        
        val emptyResult = TextDetectionResult.Empty
        assertTrue(emptyResult is TextDetectionResult.Empty)
    }
    
    // ========================================
    // Integration Tests
    // ========================================
    
    @Test
    fun `test full pipeline from detection to normalization`() = runTest {
        // Detection
        val detectionResult = mockDetector.extractFromAccessibility()
        assertTrue(detectionResult is TextDetectionResult.Success)
        
        // Normalization
        val success = detectionResult as TextDetectionResult.Success
        val normalized = mockNormalizer.normalize(listOf(success.universalText))
        
        assertNotNull(normalized)
        assertTrue(normalized.rawText.isNotEmpty())
    }
    
    @Test
    fun `test pipeline with test helper`() = runTest {
        val universalText = testHelper.simulatePipeline(
            "Hello World Test",
            TextSourceType.ACCESSIBILITY
        )
        
        assertEquals("Hello World Test", universalText.rawText)
        assertEquals(TextSourceType.ACCESSIBILITY, universalText.sourceType)
        assertTrue(universalText.tokens.isNotEmpty())
    }
    
    // ========================================
    // Test Helper Utilities Tests
    // ========================================
    
    @Test
    fun `test bounding box verification`() {
        val text = UTPTestDataBuilder.createMockUniversalText("Test")
        val isValid = testHelper.verifyBoundingBoxes(text)
        
        assertTrue(isValid)
    }
    
    @Test
    fun `test word counting`() {
        val count = testHelper.countWords("Hello world test")
        assertEquals(3, count)
        
        val countWithExtraSpaces = testHelper.countWords("Hello   world  test  ")
        assertEquals(3, countWithExtraSpaces)
    }
    
    @Test
    fun `test finding token at position`() {
        val text = UTPTestDataBuilder.createMockUniversalText("Hello world")
        
        val token1 = testHelper.findTokenAtPosition(text, 0) // "Hello"
        assertNotNull(token1)
        assertEquals("Hello", token1?.text)
        
        val token2 = testHelper.findTokenAtPosition(text, 6) // "world"
        assertNotNull(token2)
        assertEquals("world", token2?.text)
    }
    
    // ========================================
    // Performance Tests
    // ========================================
    
    @Test
    fun `test performance measurement`() = runTest {
        val performanceHelper = UTPPerformanceTestHelper()
        val context = UTPTestDataBuilder.createMockDetectionContext(
            hasAccessibility = true
        )
        
        val metrics = performanceHelper.measurePipelinePerformance(
            mockDetector,
            mockNormalizer,
            context
        )
        
        assertTrue(metrics.detectionTimeMs >= 0)
        assertTrue(metrics.normalizationTimeMs >= 0)
        assertTrue(metrics.totalTimeMs >= 0)
        assertTrue(metrics.textLength > 0)
        assertTrue(metrics.throughput >= 0)
    }
    
    @Test
    fun `test TTS mock engine`() = runTest {
        val ttsEngine = MockTTSEngine()
        val ranges = mutableListOf<Pair<Int, Int>>()
        
        ttsEngine.addRangeListener { start, end ->
            ranges.add(start to end)
        }
        
        ttsEngine.simulateSpeech("Hello world", wordsPerSecond = 10)
        
        assertTrue(ranges.isNotEmpty())
        assertEquals(2, ranges.size) // "Hello" and "world"
        
        ttsEngine.stop()
    }
    
    // ========================================
    // Configuration Tests
    // ========================================
    
    @Test
    fun `test TextDetectionConfig default values`() {
        val config = TextDetectionConfig()
        
        assertTrue(config.enableAccessibility)
        assertTrue(config.enableOCR)
        assertTrue(config.enableWebFetch)
        assertFalse(config.enableContinuousOCR)
        assertEquals(2000L, config.continuousOCRInterval)
        assertEquals(MergeStrategy.SMART, config.mergeStrategy)
    }
    
    @Test
    fun `test NormalizationConfig custom values`() {
        val config = NormalizationConfig(
            removeDuplicates = false,
            normalizeFormatting = false,
            orderByReading = false,
            filterNoise = false,
            tokenizeBy = TokenizeMode.SENTENCE,
            mapPositions = false,
            minConfidence = 0.8f
        )
        
        assertFalse(config.removeDuplicates)
        assertFalse(config.normalizeFormatting)
        assertEquals(TokenizeMode.SENTENCE, config.tokenizeBy)
        assertEquals(0.8f, config.minConfidence, 0.01f)
    }
}
