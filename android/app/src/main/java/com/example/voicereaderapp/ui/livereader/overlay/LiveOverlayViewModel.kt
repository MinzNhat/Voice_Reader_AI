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
        // Khi mở rộng, lớp phủ chính cần có khả năng tương tác để bắt sự kiện chạm
        setOverlayInteractive(true)
    }

    fun collapseOverlay() {
        _isExpanded.value = false
        // Khi thu gọn, ta không cần lớp phủ tương tác nữa
        setOverlayInteractive(false)
    }

    fun setOverlayInteractive(interactive: Boolean) {
        // Chỉ thay đổi nếu giá trị mới khác giá trị cũ để tránh cập nhật thừa
        if (_isInteractive.value != interactive) {
            _isInteractive.value = interactive
        }
    }

    // ----------------------------------- implementation for live reader ocr --------------------------------------

    private var isAutoScrolling = false
    private var lastSegmentWords: List<OCRWord> = emptyList() // Lưu list từ của lần chụp trước
    private var globalWordIndex = 0 // Index highlight toàn cục

    // Danh sách toàn bộ các từ đang hiển thị trên màn hình (có tọa độ bbox)
    private val _currentPageWords = MutableStateFlow<List<OCRWord>>(emptyList())
    val currentPageWords: StateFlow<List<OCRWord>> = _currentPageWords

    // Index cục bộ trên màn hình (để biết đang đọc từ thứ mấy trên màn hình này)
    private val _currentLocalIndex = MutableStateFlow(-1)
    val currentLocalIndex: StateFlow<Int> = _currentLocalIndex

    private var hasScrolledCurrentPage = false


    // Alg để ghép văn bản
    /**
     * 1. Lấy đoạn cuối của text cũ (tính từ vị trí 70% trở đi).
     * 2. Tìm đoạn đó ở đầu text mới.
     * 3. Loại bỏ phần trùng lặp và nối phần còn lại.
     */
    private fun getUniqueNewWords(oldWords: List<OCRWord>, newWords: List<OCRWord>): List<OCRWord> {
        if (oldWords.isEmpty()) return newWords

        // Dùng coerceAtLeast(1) để đảm bảo ít nhất có 1 từ nếu list quá ngắn
        val anchorSize = (oldWords.size * 0.5).toInt().coerceAtLeast(1)

        // Lấy list text của anchor (50% cuối cùng)
        val anchorWords = oldWords.takeLast(anchorSize).map { it.text }

        // Giới hạn vùng tìm kiếm: Chỉ quét trong 50% đầu của list mới
        val searchLimit = (newWords.size * 0.5).toInt().coerceAtLeast(anchorSize)

        // Quét newWords để tìm anchor
        for (i in 0..searchLimit) {
            if (isSubListMatch(newWords, i, anchorWords)) {
                val splitIndex = i + anchorSize

                if (splitIndex < newWords.size) {
                    Log.d("LiveReader", "✅ MERGE: Match found at $i. Taking new words from index $splitIndex.")
                    return newWords.subList(splitIndex, newWords.size)
                } else {
                    Log.d("LiveReader", "⚠️ MERGE: Full overlap (Trùng hoàn toàn).")
                    return emptyList()
                }
            }
        }

        Log.d("LiveReader", "⚠️ MERGE: No overlap found (Không tìm thấy 30% đuôi). Appending all.")
        return newWords
    }

    /**
     * Helper: So sánh list con
     */
    private fun isSubListMatch(fullList: List<OCRWord>, startIndex: Int, pattern: List<String>): Boolean {
        if (startIndex + pattern.size > fullList.size) return false
        for (j in pattern.indices) {
            // So sánh chính xác text (có thể thêm trim() nếu cần)
            if (fullList[startIndex + j].text != pattern[j]) {
                return false
            }
        }
        return true
    }

    // Hàm được gọi từ UI khi bấm Play Live Read
    fun startLiveReading() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            _isReading.value = true
            lastSegmentWords = emptyList()
            _fullText.value = ""
            globalWordIndex = 0
            isAutoScrolling = false
            Log.d("CAPTURE_PROC", " start capture")
            captureAndProcess()
        }
    }

    fun onUserInteracted() {
        // User chạm vào màn hình -> Tạm dừng
        if (isReading.value) {
            Log.d("LiveReader", "User interacted -> Pausing Live Reader")
            toggleReading() // Pause TTS
            isAutoScrolling = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun captureAndProcess() {
        val service = ScreenReaderAccessibilityService.instance ?: return
        Log.d("CAPTURE_PROC", "📸 Capturing...")

        service.captureScreen { bitmap ->
            scope.launch {
                var imageFile: File? = null
                try {
                    imageFile = saveBitmapToFile(bitmap)
                    val result = ocrRepository.performOCR(imageFile)

                    if (result is Result.Success) {
                        val currentRawWords = result.data.words
                        val contentWords = currentRawWords.filter { it.bbox.y1 > 100 }

                        if (isContentDuplicate(lastSegmentWords, contentWords)) {
                            Log.d("LiveReader", "🛑 Duplicate content detected. Stop reading.")

                            _isReading.value = false
                            ttsRepository.stopAudio()
                            isAutoScrolling = false

                            return@launch
                        }

                        _currentPageWords.value = contentWords
                        val uniqueWords = getUniqueNewWords(lastSegmentWords, contentWords)
                        lastSegmentWords = contentWords // Cập nhật last segment

                        if (uniqueWords.isNotEmpty()) {
                            val textSegment = uniqueWords.joinToString(" ") { it.text }
                            val prefix = if (_fullText.value.isEmpty()) "" else " "
                            _fullText.value += prefix + textSegment

                            val fullScreenText = contentWords.joinToString(" ") { it.text }
                            playSegment(fullScreenText, contentWords)
                        } else {
                            Log.d("LiveReader", "⚠️ No new words found. Stopping.")
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

    // --- [HÀM MỚI] Kiểm tra độ giống nhau giữa 2 list từ ---
    private fun isContentDuplicate(oldWords: List<OCRWord>, newWords: List<OCRWord>): Boolean {
        if (oldWords.isEmpty() || newWords.isEmpty()) return false

        // Nếu số lượng từ chênh lệch quá nhiều -> Chắc chắn khác nhau
        if (Math.abs(oldWords.size - newWords.size) > 5) return false

        // So sánh 5 từ đầu và 5 từ cuối để kiểm tra nhanh
        val checkCount = 5.coerceAtMost(oldWords.size)

        val oldHead = oldWords.take(checkCount).joinToString { it.text }
        val newHead = newWords.take(checkCount).joinToString { it.text }

        val oldTail = oldWords.takeLast(checkCount).joinToString { it.text }
        val newTail = newWords.takeLast(checkCount).joinToString { it.text }

        // Nếu cả đầu và đuôi đều giống nhau -> Trùng lặp 99%
        return oldHead == newHead && oldTail == newTail
    }

    // ------------------------------------------------------------------------------
    // 3. PLAYBACK & SCROLL
    // ------------------------------------------------------------------------------

    private suspend fun playSegment(
        textToSpeak: String,                // Text gửi đi TTS
        wordsOnScreen: List<OCRWord>        // List từ có BBox để vẽ/scroll
    ) {
        val voiceId = _selectedVoiceId.value
        Log.d("SPEAKING", textToSpeak)
        // 1. Gọi API TTS
        hasScrolledCurrentPage = false
        isAutoScrolling = false

        val audioResult = ttsRepository.generateSpeech(textToSpeak, voiceId)

        if (audioResult is Result.Success) {
            val timingResult = ttsRepository.getWordTimings(textToSpeak)
            val rawTimings = if (timingResult is Result.Success) timingResult.data.timings else emptyList()

            if (_isReading.value) {
                ttsRepository.playAudio(
                    base64Audio = audioResult.data,
                    playbackSpeed = _speed.value,
                    onProgress = { currentMs ->
                        val calibratedMs = (currentMs * 1f).toLong()
                        val index = rawTimings.indexOfLast {
                            calibratedMs >= it.startMs && calibratedMs < it.endMs
                        }

                        if (index != -1 && index < wordsOnScreen.size) {
                            _currentLocalIndex.value = index

                            val currentY = wordsOnScreen[index].bbox.y1
                            checkAndTriggerScroll(index, wordsOnScreen.size, currentY)
                        }
                    },
                    onComplete = {
                        _currentLocalIndex.value = -1
                    }
                )
            }
        }
    }

    private fun checkAndTriggerScroll(
        currentIndex: Int,
        totalWords: Int,
        currentY: Float
    ) {
        if (!isAutoScrolling && !hasScrolledCurrentPage && totalWords > 0 && currentIndex >= (totalWords * 0.5)) {
            Log.d("LiveReader", "🎯 Trigger Scroll at index $currentIndex / $totalWords")
            performScroll(currentY)
        }
    }

    private fun performScroll(currentY: Float?) {
        val service = ScreenReaderAccessibilityService.instance ?: return

        hasScrolledCurrentPage = true
        isAutoScrolling = true

        val displayMetrics = context.resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels.toFloat()
        val screenWidth = displayMetrics.widthPixels.toFloat()
        val centerX = screenWidth / 2f

        val swipeStartY = screenHeight * 0.4f

        val targetDistance = if (currentY != null) {
            val topMargin = 400f
            (currentY - topMargin).coerceAtLeast(0f)
        } else {
            screenHeight * 0.4f
        }

        var swipeEndY = swipeStartY - targetDistance

        if (swipeEndY < 100f) {
            swipeEndY = 100f
        }

        if (swipeEndY >= swipeStartY) {
            Log.d("LiveReader", "🛑 Cannot scroll further (Target unreachable).")
            scope.launch {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    captureAndProcess()
                }
            }
            return
        }

        Log.d("LiveReader", "Smart Scroll: currentY=$currentY. Swipe: $swipeStartY -> $swipeEndY")

        service.performScroll(centerX, swipeStartY, swipeEndY)

        scope.launch {
            // Chờ animation vuốt hoàn tất (500ms)
            delay(500)

            if (_isReading.value) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Log.d("LiveReader", "📸 Scroll done. Capturing next segment...")
                    captureAndProcess()
                }
            } else {
                // Chỉ reset nếu người dùng chủ động Pause
                isAutoScrolling = false
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
