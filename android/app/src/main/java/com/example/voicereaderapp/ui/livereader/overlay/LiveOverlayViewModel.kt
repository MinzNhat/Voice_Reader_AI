package com.example.voicereaderapp.ui.livereader.overlay

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.voicereaderapp.data.remote.model.WordTiming
import com.example.voicereaderapp.domain.repository.TTSRepository
import com.example.voicereaderapp.domain.repository.OCRRepository
import com.example.voicereaderapp.domain.usecase.GetVoiceSettingsUseCase
import com.example.voicereaderapp.domain.usecase.UpdateVoiceSettingsUseCase
import com.example.voicereaderapp.domain.model.TTSVoice
import com.example.voicereaderapp.domain.model.VoiceGender
import com.example.voicereaderapp.domain.service.ScreenReaderAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import javax.inject.Inject
import android.util.Log
import com.example.voicereaderapp.data.remote.model.OCRResponse
import com.example.voicereaderapp.data.remote.model.OCRWord
import java.io.File
import com.example.voicereaderapp.utils.Result
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs


// Deprecated: Use TTSVoice from domain model instead
@Deprecated("Use TTSVoice enum with proper voice IDs")
enum class VoiceConfig {Male, FeMale}

// Helper class để chứa kết quả sau khi merge
data class MergedOCRResult(
    val textToSpeak: String,
    val newWords: List<OCRWord>
)

/**
 * State manager for Live Overlay Service
 * Integrated with existing VoiceSettings and backend TTS
 *
 * Note: Not a ViewModel because it's used in a Service context
 * Uses CoroutineScope with SupervisorJob for proper lifecycle management
 */
class LiveOverlayViewModel @Inject constructor(
    @ApplicationContext private val context: Context, // Inject Context để lưu file
    private val ttsRepository: TTSRepository,
    private val ocrRepository: OCRRepository,
    private val getVoiceSettingsUseCase: GetVoiceSettingsUseCase,
    private val updateVoiceSettingsUseCase: UpdateVoiceSettingsUseCase
) {
    // Coroutine scope for this manager (similar to viewModelScope)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    // Chế độ tương tác Mặc định là false (không tương tác được)
    private val _isInteractive = MutableStateFlow(false)
    val isInteractive: StateFlow<Boolean> = _isInteractive

    private val _isExpanded = MutableStateFlow(false)
    val isExpanded: StateFlow<Boolean> = _isExpanded

    // Overlay đang đọc hay pause
    private val _isReading = MutableStateFlow(false)
    val isReading: StateFlow<Boolean> = _isReading

    // Tốc độ đọc (1.0 = normal) - synced with VoiceSettings
    private val _speed = MutableStateFlow(1.0f)
    val speed: StateFlow<Float> = _speed

    // Giọng đọc hiện tại (deprecated, kept for UI compatibility)
    private val _voiceConfig = MutableStateFlow(VoiceConfig.Male)
    val voiceConfig: StateFlow<VoiceConfig> = _voiceConfig

    // Actual voice ID (e.g., "matt", "anna", "minseo")
    private val _selectedVoiceId = MutableStateFlow("matt")
    val selectedVoiceId: StateFlow<String> = _selectedVoiceId

    // Language code (e.g., "en-US", "ko-KR")
    private val _selectedLanguage = MutableStateFlow("en-US")
    val selectedLanguage: StateFlow<String> = _selectedLanguage

    // Main settings for global control
    private val _useMainVoiceForAll = MutableStateFlow(false)
    val useMainVoiceForAll: StateFlow<Boolean> = _useMainVoiceForAll

    private val _mainVoiceId = MutableStateFlow("matt")
    val mainVoiceId: StateFlow<String> = _mainVoiceId

    private val _useMainSpeedForAll = MutableStateFlow(false)
    val useMainSpeedForAll: StateFlow<Boolean> = _useMainSpeedForAll

    private val _mainSpeed = MutableStateFlow(1.0f)
    val mainSpeed: StateFlow<Float> = _mainSpeed

    // Theme mode
    private val _themeMode = MutableStateFlow(com.example.voicereaderapp.domain.model.ThemeMode.SYSTEM)
    val themeMode: StateFlow<com.example.voicereaderapp.domain.model.ThemeMode> = _themeMode

    init {
        // Load global voice settings from DataStore
        scope.launch {
            getVoiceSettingsUseCase().collect { settings ->
                // Update main settings flags
                _useMainVoiceForAll.value = settings.useMainVoiceForAll
                _mainVoiceId.value = settings.mainVoiceId
                _useMainSpeedForAll.value = settings.useMainSpeedForAll
                _mainSpeed.value = settings.mainSpeed
                _themeMode.value = settings.theme

                // Apply main voice/speed if enabled, otherwise use individual settings
                if (settings.useMainVoiceForAll) {
                    _selectedVoiceId.value = settings.mainVoiceId
                    val voice = TTSVoice.fromId(settings.mainVoiceId)
                    _voiceConfig.value = when (voice?.gender) {
                        VoiceGender.MALE -> VoiceConfig.Male
                        VoiceGender.FEMALE -> VoiceConfig.FeMale
                        else -> VoiceConfig.Male
                    }
                } else {
                    _selectedVoiceId.value = settings.voiceId
                    val voice = TTSVoice.fromId(settings.voiceId)
                    _voiceConfig.value = when (voice?.gender) {
                        VoiceGender.MALE -> VoiceConfig.Male
                        VoiceGender.FEMALE -> VoiceConfig.FeMale
                        else -> VoiceConfig.Male
                    }
                }

                if (settings.useMainSpeedForAll) {
                    _speed.value = settings.mainSpeed
                } else {
                    _speed.value = settings.speed
                }

                _selectedLanguage.value = settings.language
            }
        }
    }

    /**
     * Cleanup resources when service is destroyed
     * Call this from Service.onDestroy()
     */
    fun cleanup() {
        scope.cancel()
        ttsRepository.stopAudio()
    }

    // Show note overlay
    private val _isNoteOverlayVisible = MutableStateFlow(false)
    val isNoteOverlayVisible: StateFlow<Boolean> = _isNoteOverlayVisible

    // Show settings overlay
    private val _isSettingsOverlayVisible = MutableStateFlow(false)
    val isSettingsOverlayVisible: StateFlow<Boolean> = _isSettingsOverlayVisible

    // ---------------------------- VOICE PAD (NHẤN GIỮ TỪ 1S TRỞ LÊN) ----------------------------------

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening
    fun onVoiceListeningStart() {
        // logic khi user nhấn voice
        _isListening.value = true
    }

    fun onVoiceListeningEnd() {
        // kết thúc voice
        _isListening.value = false
    }

    // ----------------------------     PLAYBACK CONTROL        ---------------------------------

    fun rewind() {
        // Rewind by 5 seconds (5000 milliseconds)
        val currentPos = ttsRepository.getCurrentPosition()
        val newPos = (currentPos - 5000).coerceAtLeast(0)
        ttsRepository.seekTo(newPos)
    }

    fun forward() {
        // Forward by 5 seconds (5000 milliseconds)
        val currentPos = ttsRepository.getCurrentPosition()
        val duration = ttsRepository.getDuration()
        val newPos = (currentPos + 5000).coerceAtMost(duration)
        ttsRepository.seekTo(newPos)
    }

    // ----------------------------     HIGHTLIGHT ĐỌC TỪNG CHỮ        ---------------------------------

    // toàn bộ văn bản đang đọc
    private val _fullText = MutableStateFlow("")
    val fullText : StateFlow<String> = _fullText

    // idx đang được HL
    private val _currentIndex = MutableStateFlow(0)
    val currentIndex : StateFlow<Int> = _currentIndex

    fun setReadingText(newText: String){
        _fullText.value = newText
        _currentIndex.value = 0
    }

    fun updateHighlightIdx(index: Int){
        _currentIndex.value = index.coerceIn(0, _fullText.value.length)
    }

    fun getLines(): List<String> {
        return _fullText.value.split("\n")
    }

    // ----------------------------         các hàm gọi từ UI          ----------------------------------
    fun toggleReading(){
        scope.launch {
            if (_isReading.value) {
                // Đang đọc -> Pause
                _isReading.value = false
                delay(50) // delay 0.2s
                collapseOverlay()
                delay(200) // delay 0.2s
                _currentIndex.value = -1
                ttsRepository.pauseAudio()
                isAutoScrolling = false
            } else {
                // Đang pause -> Resume
                _isReading.value = true
                delay(50) // delay 0.2s
                collapseOverlay()
                delay(200) // delay 0.2s
                Log.d("LIVE_READING", "Resume reading")
                startLiveReading()
            }
        }
    }

    fun setSpeed(newSpeed: Float){
        // API limit: 0.5x - 2.0x (NAVER Clova Voice API)
        val coercedSpeed = newSpeed.coerceIn(0.5f, 2.0f)
        _speed.value = coercedSpeed

        // Save to DataStore
        scope.launch {
            try {
                val currentSettings = getVoiceSettingsUseCase().first()
                updateVoiceSettingsUseCase(currentSettings.copy(speed = coercedSpeed))
                // Apply speed to current playback if playing
                ttsRepository.setPlaybackSpeed(coercedSpeed)
            } catch (e: Exception) {
                android.util.Log.e("LiveOverlayViewModel", "Failed to save speed", e)
            }
        }
    }

    fun setVoice(newVoice: VoiceConfig){
        _voiceConfig.value = newVoice

        // Convert to actual voice ID and save
        scope.launch {
            try {
                val currentSettings = getVoiceSettingsUseCase().first()
                val voiceId = when (newVoice) {
                    VoiceConfig.Male -> "matt"  // Default male voice
                    VoiceConfig.FeMale -> "danna"  // Default female voice (NAVER speaker ID)
                }
                _selectedVoiceId.value = voiceId
                updateVoiceSettingsUseCase(currentSettings.copy(voiceId = voiceId))
            } catch (e: Exception) {
                android.util.Log.e("LiveOverlayViewModel", "Failed to save voice", e)
            }
        }
    }

    /**
     * Set voice by actual voice ID (recommended)
     */
    fun setVoiceById(voiceId: String, language: String) {
        _selectedVoiceId.value = voiceId
        _selectedLanguage.value = language

        // Update deprecated VoiceConfig for UI
        val voice = TTSVoice.fromId(voiceId)
        _voiceConfig.value = when (voice?.gender) {
            VoiceGender.MALE -> VoiceConfig.Male
            VoiceGender.FEMALE -> VoiceConfig.FeMale
            else -> VoiceConfig.Male
        }

        // Save to DataStore
        scope.launch {
            try {
                val currentSettings = getVoiceSettingsUseCase().first()
                updateVoiceSettingsUseCase(
                    currentSettings.copy(voiceId = voiceId, language = language)
                )
            } catch (e: Exception) {
                android.util.Log.e("LiveOverlayViewModel", "Failed to save voice", e)
            }
        }
    }

    fun showNoteOverlay() {
        _isNoteOverlayVisible.value = true
    }

    fun hideNoteOverlay() {
        _isNoteOverlayVisible.value = false
    }

    fun showSettingsOverlay() {
        _isSettingsOverlayVisible.value = true
    }

    fun hideSettingsOverlay() {
        _isSettingsOverlayVisible.value = false
    }

    fun expandOverlay() {
        _isExpanded.value = true
        setOverlayInteractive(true)
    }

    fun collapseOverlay() {
        _isExpanded.value = false
        setOverlayInteractive(false)
    }

    fun setOverlayInteractive(interactive: Boolean) {
        if (_isInteractive.value != interactive) {
            _isInteractive.value = interactive
        }
    }

    // ----------------------------------- implementation for live reader ocr --------------------------------------

    private var isAutoScrolling = false
    private var lastSegmentWords: List<OCRWord> = emptyList()
    private var globalWordIndex = 0 // Index highlight toàn cục

    private val _currentPageWords = MutableStateFlow<List<OCRWord>>(emptyList())
    val currentPageWords: StateFlow<List<OCRWord>> = _currentPageWords

    private val _currentLocalIndex = MutableStateFlow(-1)
    val currentLocalIndex: StateFlow<Int> = _currentLocalIndex

    private var hasScrolledCurrentPage = false
    private val playbackSessionId = AtomicLong(0)


    fun startLiveReading() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            _isReading.value = true
            lastSegmentWords = emptyList()
            _fullText.value = ""
            isAutoScrolling = false
            hasScrolledCurrentPage = false
            captureAndProcess()
        }
    }

    // Alg để ghép văn bản
    /**
     * 1. Lấy đoạn cuối của text cũ (tính từ vị trí 70% trở đi).
     * 2. Tìm đoạn đó ở đầu text mới.
     * 3. Loại bỏ phần trùng lặp và nối phần còn lại.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun captureAndProcess() {
        val service = ScreenReaderAccessibilityService.instance ?: return

        service.captureScreen { bitmap ->
            scope.launch {
                var imageFile: File? = null
                try {
                    imageFile = saveBitmapToFile(bitmap)
                    val result = ocrRepository.performOCR(imageFile)

                    if (result is Result.Success) {
                        val rawWords = result.data.words
                        val contentWords = rawWords.filter { it.bbox.y1 > 200 }

                        if (isSmartDuplicate(lastSegmentWords, contentWords)) {
                            Log.d("LiveReader", "🛑 End of Page detected (Duplicate). Stopping.")
                            _isReading.value = false
                            ttsRepository.stopAudio()
                            isAutoScrolling = false
                            return@launch
                        }

                        val uniqueWords = smartMergeWords(lastSegmentWords, contentWords)
                        lastSegmentWords = contentWords

                        if (uniqueWords.isNotEmpty()) {
                            _currentPageWords.value = contentWords

                            val textSegment = uniqueWords.joinToString(" ") { it.text }
                            val prefix = if (_fullText.value.isEmpty()) "" else " "
                            _fullText.value += prefix + textSegment

                            val textToSpeak = uniqueWords.joinToString(" ") { it.text }

                            playSegment(textToSpeak, contentWords, uniqueWords)

                        } else {
                            Log.d("LiveReader", "⚠️ No new unique content found. Stopping.")
                            _isReading.value = false
                            isAutoScrolling = false
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LiveReader", "Error", e)
                } finally {
                    imageFile?.delete()
                    bitmap.recycle()
                }
            }
        }
    }

    // --- Helper Logic (Giữ nguyên thuật toán tốt từ trước) ---
    private fun isSmartDuplicate(oldWords: List<OCRWord>, newWords: List<OCRWord>): Boolean {
        if (oldWords.isEmpty() || newWords.isEmpty()) return false
        if (abs(oldWords.size - newWords.size) < 10) {
            val oldText = oldWords.joinToString(" ") { normalize(it.text) }
            val newText = newWords.joinToString(" ") { normalize(it.text) }
            return oldText == newText || calculateSimilarity(oldText, newText) > 0.9
        }
        return false
    }

    private fun smartMergeWords(oldWords: List<OCRWord>, newWords: List<OCRWord>): List<OCRWord> {
        if (oldWords.isEmpty()) return newWords
        val sampleSize = 10.coerceAtMost(oldWords.size)
        val sampleWords = oldWords.takeLast(sampleSize).map { normalize(it.text) }
        val searchLimit = (newWords.size * 0.6).toInt()
        for (i in 0..searchLimit) {
            if (isFuzzyMatch(newWords, i, sampleWords)) {
                val splitIndex = i + sampleSize
                return if (splitIndex < newWords.size) newWords.subList(splitIndex, newWords.size) else emptyList()
            }
        }
        return newWords
    }

    private fun isFuzzyMatch(fullList: List<OCRWord>, startIndex: Int, pattern: List<String>): Boolean {
        if (startIndex + pattern.size > fullList.size) return false
        var matchCount = 0
        for (j in pattern.indices) {
            if (normalize(fullList[startIndex + j].text) == pattern[j]) matchCount++
        }
        return (matchCount.toFloat() / pattern.size) > 0.7f
    }

    private fun normalize(s: String) = s.lowercase().replace(Regex("[^a-z0-9]"), "")

    private fun calculateSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        val longer = if (s1.length > s2.length) s1 else s2
        val shorter = if (s1.length > s2.length) s2 else s1
        return if (longer.isEmpty()) 0.0 else (longer.length - levenshtein(longer, shorter)) / longer.length.toDouble()
    }

    private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLen = lhs.length
        val rhsLen = rhs.length
        var costs = IntArray(rhsLen + 1) { it }
        for (i in 1..lhsLen) {
            val newCosts = IntArray(rhsLen + 1) { 0 }
            newCosts[0] = i
            for (j in 1..rhsLen) {
                val cost = if (lhs[i - 1] == rhs[j - 1]) 0 else 1
                newCosts[j] = minOf(newCosts[j - 1] + 1, costs[j] + 1, costs[j - 1] + cost)
            }
            costs = newCosts
        }
        return costs[rhsLen]
    }

    // ============================================================================================
    // PLAYBACK & SCROLL LOGIC [ĐÃ FIX LỖI SCROLL LIÊN TỤC]
    // ============================================================================================

    private suspend fun playSegment(
        textToSpeak: String,
        allWordsOnScreen: List<OCRWord>,
        uniqueWordsToRead: List<OCRWord>
    ) {
        val voiceId = _selectedVoiceId.value

        // 1. Tạo ID phiên mới. Các callback từ phiên cũ (TTS đang đọc dở) sẽ bị vô hiệu hóa.
        val currentSession = playbackSessionId.incrementAndGet()

        // 2. Reset trạng thái cuộn cho trang MỚI này
        hasScrolledCurrentPage = false
        isAutoScrolling = false

        if (textToSpeak.isBlank()) return

        val audioResult = ttsRepository.generateSpeech(textToSpeak, voiceId)

        if (audioResult is Result.Success) {
            val timingResult = ttsRepository.getWordTimings(textToSpeak)
            val rawTimings = if (timingResult is Result.Success) timingResult.data.timings else emptyList()

            if (_isReading.value) {
                ttsRepository.playAudio(
                    base64Audio = audioResult.data,
                    playbackSpeed = _speed.value,
                    onProgress = { currentMs ->
                        // [FIX] Kiểm tra Session ID: Nếu không khớp ID mới nhất -> Bỏ qua ngay lập tức
                        if (playbackSessionId.get() != currentSession) return@playAudio

                        val calibratedMs = (currentMs * 1f).toLong()
                        // 1. Tìm từ đang đọc trong danh sách uniqueWords
                        val uniqueIndex = rawTimings.indexOfLast {
                            calibratedMs >= it.startMs && calibratedMs < it.endMs
                        }

                        // 2. Map sang vị trí trên màn hình
                        if (uniqueIndex != -1 && uniqueIndex < uniqueWordsToRead.size) {
                            val wordBeingRead = uniqueWordsToRead[uniqueIndex]

                            // Tìm vị trí thật trên màn hình để highlight
                            val screenIndex = allWordsOnScreen.indexOfLast {
                                it.bbox == wordBeingRead.bbox && it.text == wordBeingRead.text
                            }

                            if (screenIndex != -1) {
                                _currentLocalIndex.value = screenIndex
                                val currentY = allWordsOnScreen[screenIndex].bbox.y1

                                checkAndTriggerScroll(currentY, screenIndex, allWordsOnScreen.size)
                            }
                        }
                    },
                    onComplete = {
                        if (playbackSessionId.get() == currentSession) {
                            _currentLocalIndex.value = -1
                        }
                    }
                )
            }
        }
    }

    private fun checkAndTriggerScroll(currentY: Float, currentIndex: Int, totalWords: Int) {
        val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()

        if (!isAutoScrolling && !hasScrolledCurrentPage &&
            currentIndex > (totalWords * 0.5) &&
            currentY > (screenHeight * 0.5)) {

            Log.d("LiveReader", "🎯 Triggering Scroll: Idx=$currentIndex/$totalWords, Y=$currentY")
            performScroll(currentY)
        }
    }

    private fun performScroll(currentY: Float) {
        val service = ScreenReaderAccessibilityService.instance ?: return

        // Khóa ngay lập tức
        hasScrolledCurrentPage = true
        isAutoScrolling = true

        val displayMetrics = context.resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels.toFloat()
        val centerX = displayMetrics.widthPixels.toFloat() / 2f

        // Vuốt từ 50% màn hình lên
        val swipeStartY = screenHeight * 0.5f

        // Đích đến: Đưa dòng chữ đang đọc lên vị trí cách top 200px
        val targetDistance = (currentY - 400f).coerceAtLeast(0f)
        var swipeEndY = swipeStartY - targetDistance

        // Chặn biên
        if (swipeEndY < 100f) swipeEndY = 100f

        if (swipeStartY - swipeEndY < 50f) {
            Log.d("LiveReader", "🛑 Distance too small (End of Page?). Skip scroll.")

            scope.launch {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    delay(500)
                    captureAndProcess()
                }
            }
            return
        }

        Log.d("LiveReader", "Scroling: $swipeStartY -> $swipeEndY")
        service.performScroll(centerX, swipeStartY, swipeEndY)

        scope.launch {
            delay(800)
            if (_isReading.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                captureAndProcess()
            }
        }
    }

    private suspend fun saveBitmapToFile(bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "temp_ocr.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
        return@withContext file
    }
}
