package com.example.voicereaderapp.ui.test

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.voicereaderapp.domain.model.utp.*
import com.example.voicereaderapp.domain.repository.utp.*
import com.example.voicereaderapp.domain.utp.test.*
import android.view.accessibility.AccessibilityNodeInfo
import android.graphics.Rect
import android.content.Context
import com.example.voicereaderapp.data.repository.utp.backend.NoopBackendOcrClient
import com.example.voicereaderapp.data.repository.utp.TextDetectorRepositoryImpl
import kotlinx.coroutines.launch

/**
 * UTP Test Screen
 * Interactive UI for manually testing Universal Text Pipeline components
 */
@Composable
fun UTPTestScreen() {
    val scope = rememberCoroutineScope()
    var testResults by remember { mutableStateOf<List<TestResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    val mockDetector = remember { MockTextDetectorRepository() }
    val mockNormalizer = remember { MockTextNormalizerRepository() }
    val testHelper = remember { UTPTestHelper() }
    val performanceHelper = remember { UTPPerformanceTestHelper() }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Text(
            "UTP Testing Interface",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            "Universal Text Pipeline - Test all layers",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
        
        // Test Categories
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Layer 1: Detection Tests
            item {
                TestCategoryHeader("Layer 1: Text Detection")
            }
            
            items(getLayer1Tests()) { test ->
                TestButton(
                    test = test,
                    isLoading = isLoading,
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val result = when (test.id) {
                                "accessibility_real_test" -> runRealAccessibilityTest(context, "Hello world from accessibility")
                                "accessibility_multiline_test" -> runRealAccessibilityTest(context, "Hello world\nThis is a multiline test")
                                else -> runTest(test, mockDetector, mockNormalizer, testHelper, performanceHelper)
                            }
                            testResults = listOf(result) + testResults
                            isLoading = false
                        }
                    }
                )
            }
            
            // Layer 2: Normalization Tests
            item {
                TestCategoryHeader("Layer 2: Text Normalization")
            }
            
            items(getLayer2Tests()) { test ->
                TestButton(
                    test = test,
                    isLoading = isLoading,
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val result = runTest(test, mockDetector, mockNormalizer, testHelper, performanceHelper)
                            testResults = listOf(result) + testResults
                            isLoading = false
                        }
                    }
                )
            }
            
            // Integration & Performance Tests
            item {
                TestCategoryHeader("Integration & Performance")
            }
            
            items(getIntegrationTests()) { test ->
                TestButton(
                    test = test,
                    isLoading = isLoading,
                    onClick = {
                        scope.launch {
                            isLoading = true
                            val result = runTest(test, mockDetector, mockNormalizer, testHelper, performanceHelper)
                            testResults = listOf(result) + testResults
                            isLoading = false
                        }
                    }
                )
            }
        }
        
        // Results Section
        if (testResults.isNotEmpty()) {
            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            Text(
                "Test Results",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(testResults) { result ->
                    TestResultCard(result)
                }
            }
            
            Button(
                onClick = { testResults = emptyList() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Clear Results")
            }
        }
    }
}

@Composable
private fun TestCategoryHeader(title: String) {
    Text(
        title,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun TestButton(
    test: TestCase,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(test.name, fontWeight = FontWeight.Medium)
                Text(
                    test.description,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun TestResultCard(result: TestResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.success) 
                Color(0xFF4CAF50).copy(alpha = 0.1f) 
            else 
                Color(0xFFF44336).copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    result.testName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    if (result.success) "✓ PASS" else "✗ FAIL",
                    color = if (result.success) Color(0xFF4CAF50) else Color(0xFFF44336),
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                result.message,
                fontSize = 12.sp,
                color = Color.Gray
            )
            
            if (result.details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    result.details,
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
            
            Text(
                "Completed in ${result.durationMs}ms",
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ========================================
// Test Cases
// ========================================

private fun getLayer1Tests() = listOf(
    TestCase(
        id = "accessibility_test",
        name = "Test Accessibility Extractor",
        description = "Extract text from accessibility service",
        category = TestCategory.LAYER1
    ),
    TestCase(
        id = "ocr_test",
        name = "Test OCR Engine (Backend delegated)",
        description = "Extract text from image using backend OCR client (Noop by default)",
        category = TestCategory.LAYER1
    ),
    TestCase(
        id = "web_test",
        name = "Test Web Fetcher",
        description = "Extract text from web URL",
        category = TestCategory.LAYER1
    ),
    TestCase(
        id = "continuous_ocr_test",
        name = "Test Continuous OCR",
        description = "Test screen monitoring OCR stream",
        category = TestCategory.LAYER1
    ),
    TestCase(
        id = "accessibility_real_test",
        name = "Test Accessibility (Real Extractor)",
        description = "Run the real Accessibility extractor on a test node",
        category = TestCategory.LAYER1
    ),
    TestCase(
        id = "accessibility_multiline_test",
        name = "Test Accessibility (Multiline)",
        description = "Run Accessibility extractor on a multiline node",
        category = TestCategory.LAYER1
    ),
)

private fun getLayer2Tests() = listOf(
    TestCase(
        id = "normalize_test",
        name = "Test Text Normalizer",
        description = "Normalize and tokenize text",
        category = TestCategory.LAYER2
    ),
    TestCase(
        id = "merge_test",
        name = "Test Text Merger",
        description = "Merge multiple text sources",
        category = TestCategory.LAYER2
    ),
    TestCase(
        id = "dedup_test",
        name = "Test Duplicate Removal",
        description = "Remove duplicate tokens",
        category = TestCategory.LAYER2
    )
)

private fun getIntegrationTests() = listOf(
    TestCase(
        id = "pipeline_test",
        name = "Test Full Pipeline",
        description = "Test detection + normalization",
        category = TestCategory.INTEGRATION
    ),
    TestCase(
        id = "performance_test",
        name = "Test Performance",
        description = "Measure pipeline performance",
        category = TestCategory.PERFORMANCE
    ),
    TestCase(
        id = "tts_test",
        name = "Test TTS + Highlight",
        description = "Test text-to-speech synchronization",
        category = TestCategory.INTEGRATION
    )
)

// ========================================
// Test Execution
// ========================================

private suspend fun runTest(
    test: TestCase,
    detector: MockTextDetectorRepository,
    normalizer: MockTextNormalizerRepository,
    helper: UTPTestHelper,
    performanceHelper: UTPPerformanceTestHelper
): TestResult {
    val startTime = System.currentTimeMillis()
    
    return try {
        when (test.id) {
            "accessibility_test" -> testAccessibility(detector)
            "ocr_test" -> testOCR(detector)
            "web_test" -> testWeb(detector)
            "continuous_ocr_test" -> testContinuousOCR(detector)
            "normalize_test" -> testNormalize(normalizer)
            "merge_test" -> testMerge(normalizer)
            "dedup_test" -> testDedup(normalizer)
            "pipeline_test" -> testPipeline(helper)
            "performance_test" -> testPerformance(performanceHelper, detector, normalizer)
            "tts_test" -> testTTS()
            else -> TestResult(
                testName = test.name,
                success = false,
                message = "Unknown test",
                details = "",
                durationMs = 0
            )
        }
    } catch (e: Exception) {
        TestResult(
            testName = test.name,
            success = false,
            message = "Test failed with exception: ${e.message}",
            details = e.stackTraceToString(),
            durationMs = System.currentTimeMillis() - startTime
        )
    }
}

private suspend fun testAccessibility(detector: MockTextDetectorRepository): TestResult {
    val startTime = System.currentTimeMillis()
    val result = detector.extractFromAccessibility()
    
    return when (result) {
        is TextDetectionResult.Success -> {
            val text = result.universalText
            TestResult(
                testName = "Accessibility Extractor",
                success = true,
                message = "Successfully extracted ${text.tokens.size} tokens",
                details = "Text: ${text.rawText}\nSource: ${text.sourceType}\nTokens: ${text.tokens.size}",
                durationMs = System.currentTimeMillis() - startTime
            )
        }
        is TextDetectionResult.Error -> TestResult(
            testName = "Accessibility Extractor",
            success = false,
            message = result.message,
            details = result.throwable?.message ?: "",
            durationMs = System.currentTimeMillis() - startTime
        )
        else -> TestResult(
            testName = "Accessibility Extractor",
            success = false,
            message = "No result",
            details = "",
            durationMs = System.currentTimeMillis() - startTime
        )
    }
}

private suspend fun testOCR(detector: MockTextDetectorRepository): TestResult {
    val startTime = System.currentTimeMillis()
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    val result = detector.extractFromOCR(OcrSource.Image(bitmap))
    
    return when (result) {
        is TextDetectionResult.Success -> {
            val text = result.universalText
            TestResult(
                testName = "OCR Engine",
                success = true,
                message = "OCR extracted ${text.tokens.size} tokens",
                details = "Text: ${text.rawText}\nConfidence: ${text.metadata.confidence}",
                durationMs = System.currentTimeMillis() - startTime
            )
        }
        else -> TestResult(
            testName = "OCR Engine",
            success = false,
            message = "OCR failed",
            details = "",
            durationMs = System.currentTimeMillis() - startTime
        )
    }
}

private suspend fun testWeb(detector: MockTextDetectorRepository): TestResult {
    val startTime = System.currentTimeMillis()
    val result = detector.extractFromWeb("https://example.com")
    
    return when (result) {
        is TextDetectionResult.Success -> {
            TestResult(
                testName = "Web Fetcher",
                success = true,
                message = "Web content extracted successfully",
                details = "URL: ${result.universalText.metadata.url}\nLength: ${result.universalText.rawText.length} chars",
                durationMs = System.currentTimeMillis() - startTime
            )
        }
        else -> TestResult(
            testName = "Web Fetcher",
            success = false,
            message = "Failed to fetch web content",
            details = "",
            durationMs = System.currentTimeMillis() - startTime
        )
    }
}

private suspend fun testContinuousOCR(detector: MockTextDetectorRepository): TestResult {
    val startTime = System.currentTimeMillis()
    val flow = detector.startContinuousOCR(interval = 1000L)
    
    // Collect first emission
    var success = false
    flow.collect { result ->
        success = result is TextDetectionResult.Success
        return@collect // Exit after first emission
    }
    
    detector.stopContinuousOCR()
    
    return TestResult(
        testName = "Continuous OCR",
        success = success,
        message = if (success) "Continuous OCR stream working" else "No results from stream",
        details = "Tested 1 frame",
        durationMs = System.currentTimeMillis() - startTime
    )
}

private suspend fun testNormalize(normalizer: MockTextNormalizerRepository): TestResult {
    val startTime = System.currentTimeMillis()
    val text = UTPTestDataBuilder.createMockUniversalText("  Hello   World  ")
    val normalized = normalizer.normalizeFormatting(text)
    
    return TestResult(
        testName = "Text Normalizer",
        success = normalized.rawText.trim() == "Hello World",
        message = "Normalization completed",
        details = "Before: '${text.rawText}'\nAfter: '${normalized.rawText}'",
        durationMs = System.currentTimeMillis() - startTime
    )
}

private suspend fun testMerge(normalizer: MockTextNormalizerRepository): TestResult {
    val startTime = System.currentTimeMillis()
    val text1 = UTPTestDataBuilder.createMockUniversalText("First")
    val text2 = UTPTestDataBuilder.createMockUniversalText("Second")
    val merged = normalizer.merge(listOf(text1, text2))
    
    return TestResult(
        testName = "Text Merger",
        success = merged.rawText.contains("First") && merged.rawText.contains("Second"),
        message = "Merged ${merged.tokens.size} tokens from 2 sources",
        details = "Result: ${merged.rawText}\nSource: ${merged.sourceType}",
        durationMs = System.currentTimeMillis() - startTime
    )
}

private suspend fun testDedup(normalizer: MockTextNormalizerRepository): TestResult {
    val startTime = System.currentTimeMillis()
    val tokens = listOf(
        UTPTestDataBuilder.createMockToken("hello", index = 0),
        UTPTestDataBuilder.createMockToken("world", index = 1),
        UTPTestDataBuilder.createMockToken("hello", index = 2)
    )
    val text = UTPTestDataBuilder.createMockUniversalText("hello world hello", tokens)
    val deduped = normalizer.removeDuplicates(text)
    
    return TestResult(
        testName = "Duplicate Removal",
        success = deduped.tokens.size == 2,
        message = "Removed ${text.tokens.size - deduped.tokens.size} duplicates",
        details = "Before: ${text.tokens.size} tokens\nAfter: ${deduped.tokens.size} tokens",
        durationMs = System.currentTimeMillis() - startTime
    )
}

private suspend fun testPipeline(helper: UTPTestHelper): TestResult {
    val startTime = System.currentTimeMillis()
    val universalText = helper.simulatePipeline("Hello World Test")
    
    val validBoundingBoxes = helper.verifyBoundingBoxes(universalText)
    
    return TestResult(
        testName = "Full Pipeline",
        success = universalText.tokens.isNotEmpty() && validBoundingBoxes,
        message = "Pipeline completed successfully",
        details = "Text: ${universalText.rawText}\nTokens: ${universalText.tokens.size}\nBounding boxes valid: $validBoundingBoxes",
        durationMs = System.currentTimeMillis() - startTime
    )
}

private suspend fun testPerformance(
    performanceHelper: UTPPerformanceTestHelper,
    detector: MockTextDetectorRepository,
    normalizer: MockTextNormalizerRepository
): TestResult {
    val startTime = System.currentTimeMillis()
    val context = UTPTestDataBuilder.createMockDetectionContext(hasAccessibility = true)
    val metrics = performanceHelper.measurePipelinePerformance(detector, normalizer, context)
    
    return TestResult(
        testName = "Performance Test",
        success = metrics.totalTimeMs < 1000, // Should be under 1 second
        message = "Performance metrics collected",
        details = """
            Detection: ${metrics.detectionTimeMs}ms
            Normalization: ${metrics.normalizationTimeMs}ms
            Total: ${metrics.totalTimeMs}ms
            Throughput: ${"%.2f".format(metrics.throughput)} chars/sec
        """.trimIndent(),
        durationMs = System.currentTimeMillis() - startTime
    )
}

private suspend fun testTTS(): TestResult {
    val startTime = System.currentTimeMillis()
    val ttsEngine = MockTTSEngine()
    val ranges = mutableListOf<Pair<Int, Int>>()
    
    ttsEngine.addRangeListener { start, end ->
        ranges.add(start to end)
    }
    
    ttsEngine.simulateSpeech("Hello world", wordsPerSecond = 10)
    ttsEngine.stop()
    
    return TestResult(
        testName = "TTS + Highlight",
        success = ranges.size == 2,
        message = "TTS highlighted ${ranges.size} words",
        details = "Ranges: ${ranges.joinToString(", ") { "(${it.first}, ${it.second})" }}",
        durationMs = System.currentTimeMillis() - startTime
    )
}

private suspend fun runRealAccessibilityTest(context: Context, nodeText: String): TestResult {
    val startTime = System.currentTimeMillis()
    try {
        val detector = TextDetectorRepositoryImpl(context, NoopBackendOcrClient())
        // Construct a simple AccessibilityNodeInfo for testing
        val root = AccessibilityNodeInfo.obtain()
        root.className = "android.widget.TextView"
        root.text = nodeText
        val bounds = Rect(0, 0, 300, 50)
        root.setBoundsInScreen(bounds)

        val result = detector.extractFromAccessibility(root)
        return when (result) {
            is TextDetectionResult.Success -> {
                val ut = result.universalText
                val mismatch = ut.tokens.any { t ->
                    val start = t.startIndex
                    val end = t.endIndex
                    if (start < 0 || end < 0 || start >= end || end > ut.rawText.length) return@any true
                    val substr = ut.rawText.substring(start, end)
                    substr != t.text
                }

                TestResult(
                    testName = "Real Accessibility Extractor",
                    success = ut.tokens.isNotEmpty() && !mismatch,
                    message = "Extracted ${ut.tokens.size} tokens",
                    details = "Raw: ${ut.rawText}\nTokens: ${ut.tokens.size}",
                    durationMs = System.currentTimeMillis() - startTime
                )
            }
            is TextDetectionResult.Error -> TestResult(
                testName = "Real Accessibility Extractor",
                success = false,
                message = result.message,
                details = result.throwable?.message ?: "",
                durationMs = System.currentTimeMillis() - startTime
            )
            else -> TestResult(
                testName = "Real Accessibility Extractor",
                success = false,
                message = "No result",
                details = "",
                durationMs = System.currentTimeMillis() - startTime
            )
        }
    } catch (e: Exception) {
        return TestResult(
            testName = "Real Accessibility Extractor",
            success = false,
            message = "Exception: ${e.message}",
            details = e.stackTraceToString(),
            durationMs = System.currentTimeMillis() - startTime
        )
    }
}

// ========================================
// Data Classes
// ========================================

data class TestCase(
    val id: String,
    val name: String,
    val description: String,
    val category: TestCategory
)

enum class TestCategory {
    LAYER1,
    LAYER2,
    INTEGRATION,
    PERFORMANCE
}

data class TestResult(
    val testName: String,
    val success: Boolean,
    val message: String,
    val details: String,
    val durationMs: Long
)
