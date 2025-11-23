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

    // Session management
    private val playbackSessionId = AtomicLong(0)

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
    fun toggleReading() {
        Log.d("LiveReader", _fullText.value)
        scope.launch {
            // Trường hợp 1: Đang quét (Scanning) -> Người dùng muốn dừng quét
            if (_isScanning.value && _isReading.value) {
                Log.d("LiveReader", "User cancelled scanning.")
                _isScanning.value = false // Cờ này false sẽ làm vòng lặp scan dừng lại
                _isReading.value = false
                _fullText.value = ""
                return@launch
            }

            // Trường hợp 2: Đang đọc (TTS playing) -> Pause
            if (_isReading.value) {
                _isReading.value = false
                ttsRepository.pauseAudio()
                Log.d("LiveReader", "Paused reading.")
            }
            // Trường hợp 3: Đang Pause/Idle
            else {
                if (_fullText.value.isNotEmpty()) {
                    // Đã có text (đang pause) -> Resume
                    _isReading.value = true
                    collapseOverlay()
                    Log.d("LiveReader", "Resuming playback.")
                    // Nếu repo support resume thì tốt, không thì play lại từ text
                    // Ở đây gọi playFullText để đơn giản hóa, logic resume sâu hơn nằm ở Repository
                    playFullText(_fullText.value, resume = true)
                } else {
                    // Chưa có text -> Bắt đầu Scan toàn bộ
                    Log.d("LiveReader", "Starting full scan.")
                    collapseOverlay()
                    startLiveReading() // Hàm này giờ sẽ chạy logic scan toàn bộ
                }
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
        hideNoteOverlay()
        setOverlayInteractive(false)
    }

    fun setOverlayInteractive(interactive: Boolean) {
        if (_isInteractive.value != interactive) {
            _isInteractive.value = interactive
        }
    }

    // ----------------------------------- implementation for live reader ocr --------------------------------------

    // Trạng thái MỚI: Đang quét OCR (Scan mode)
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    // Words collected
    private val _collectedWords = MutableStateFlow<List<OCRWord>>(emptyList())

    private var isAutoScrolling = false
    private var lastSegmentWords: List<OCRWord> = emptyList()
    private var globalWordIndex = 0 // Index highlight toàn cục

    private val _currentPageWords = MutableStateFlow<List<OCRWord>>(emptyList())
    val currentPageWords: StateFlow<List<OCRWord>> = _currentPageWords

    private val _currentLocalIndex = MutableStateFlow(-1)
    val currentLocalIndex: StateFlow<Int> = _currentLocalIndex

    val displayMetrics = context.resources.displayMetrics
    val screenHeight = displayMetrics.heightPixels.toFloat()

    fun startLiveReading() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            _isScanning.value = true
            _isReading.value = false
            _fullText.value = ""
            _currentIndex.value = 0
            lastSegmentWords = emptyList()
            collapseOverlay() // Thu gọn overlay để chụp ảnh

            scanLoopRecursive()
        }
    }

    // Alg để ghép văn bản
    /**
     * 1. Lấy đoạn cuối của text cũ (tính từ vị trí 70% trở đi).
     * 2. Tìm đoạn đó ở đầu text mới.
     * 3. Loại bỏ phần trùng lặp và nối phần còn lại.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun scanLoopRecursive() {
        // Kiểm tra cờ hủy
        if (!_isScanning.value) return

        val service = ScreenReaderAccessibilityService.instance
        if (service == null) {
            _isScanning.value = false
            return
        }

        // Delay nhẹ để UI ổn định sau khi cuộn hoặc thu gọn
        scope.launch {
            delay(1500)
            service.captureScreen { bitmap ->
                processBitmapForScan(bitmap)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun processBitmapForScan(bitmap: Bitmap) {
        scope.launch {
            var imageFile: File? = null
            try {
                imageFile = saveBitmapToFile(bitmap)
                val result = ocrRepository.performOCR(imageFile)

                if (result is Result.Success) {
                    val rawWords = result.data.words
                    // Lọc header/footer: chỉ lấy text từ y=150 trở xuống
                    val contentWords = rawWords.filter { it.bbox.y1 > 200 && it.bbox.y3 < screenHeight*0.83 }

                    // 1. Kiểm tra hết trang (Trùng lặp toàn cục)
                    if (isGlobalDuplicate(lastSegmentWords, contentWords)) {
                        Log.d("LiveReader", "🛑 Duplicate detected -> End of Page.")
                        finishScanAndRead()
                        return@launch
                    }

                    // 2. Ghép văn bản (Merge)
                    val uniqueWords = findOverlapAndMerge(lastSegmentWords, contentWords)
                    lastSegmentWords = contentWords

                    if (uniqueWords.size < 4) {
                        Log.d("LiveReader", "🛑 Too few new words found (${uniqueWords.size}). Assuming End of Page.")
                        // (Tùy chọn) Vẫn nối nốt mấy chữ cuối này vào rồi dừng
                        if (uniqueWords.isNotEmpty()) {
                            val textSegment = uniqueWords.joinToString(" ") { it.text }
                            _fullText.value += " " + textSegment
                        }
                        finishScanAndRead()
                        return@launch
                    }

                    if (uniqueWords.isNotEmpty()) {
                        val textSegment = uniqueWords.joinToString(" ") { it.text }
                        val prefix = if (_fullText.value.isEmpty()) "" else " "
                        _fullText.value += prefix + textSegment

                        Log.d("LiveReader", "✅ Scanned: ${textSegment.take(30)}...")

                        // 3. Cuộn trang
                        val scrolled = performScroll()
                        if (scrolled) {
                            // Cuộn thành công -> Đệ quy quét tiếp
                            scanLoopRecursive()
                        } else {
                            // Không cuộn được -> Hết trang
                            finishScanAndRead()
                        }
                    } else {
                        // Merge ra rỗng (trùng phần đuôi) -> Thử cuộn tiếp xem còn gì không
                        Log.d("LiveReader", "⚠️ Merge empty. Forcing scroll.")
                        if (performScroll()) {
                            scanLoopRecursive()
                        } else {
                            finishScanAndRead()
                        }
                    }
                } else {
                    Log.e("LiveReader", "OCR Failed")
                    finishScanAndRead() // Hoặc retry tùy logic
                }
            } catch (e: Exception) {
                Log.e("LiveReader", "Scan Error", e)
                finishScanAndRead()
            } finally {
                imageFile?.delete()
                bitmap.recycle()
            }
        }
    }

    private fun finishScanAndRead() {
        if (!_isScanning.value) return

        _isScanning.value = false
        Log.d("LiveReader", "🏁 Scan finished. Full text length: ${_fullText.value.length}")

        if (_fullText.value.isNotBlank()) {
            // Tự động đọc sau khi scan xong
            playFullText(_fullText.value, resume = false)
        }
    }

    fun resetText() {
        scope.launch {
            // 1. Dừng mọi hoạt động đang chạy
            _isReading.value = false
            _isScanning.value = false
            ttsRepository.stopAudio()

            // 2. Reset dữ liệu văn bản
            _fullText.value = ""
            _currentIndex.value = 0

            // 3. QUAN TRỌNG: Reset bộ nhớ đệm của thuật toán Merge
            // Nếu không reset cái này, lần scan tiếp theo sẽ bị so sánh với văn bản cũ -> Gây lỗi ghép sai.
            lastSegmentWords = emptyList()
            _collectedWords.value = emptyList()
            _currentPageWords.value = emptyList()

            // 4. Thu gọn overlay để sẵn sàng cho lần scan mới
            collapseOverlay()

            Log.d("LiveReader", "♻️ Reset text & state completed.")
        }
    }

    private fun playFullText(text: String, resume: Boolean) {
        _isReading.value = true
        val currentSession = playbackSessionId.incrementAndGet()
        Log.d("LiveReader", "▶️ Playing full text: ${text}")

        // Hiển thị NoteOverlay để người dùng thấy text đã scan (tùy chọn)
        showNoteOverlay()

        scope.launch {
            // Nếu resume và repo có hỗ trợ resume thông minh thì dùng, ở đây giả lập play lại
            // Nếu muốn play lại từ đầu đoạn scan thì dùng text gốc
            val result = ttsRepository.generateSpeech(text, _selectedVoiceId.value)

            if (result is Result.Success) {
                ttsRepository.playAudio(
                    base64Audio = result.data,
                    playbackSpeed = _speed.value,
                    onProgress = { currentMs ->
                        if (playbackSessionId.get() != currentSession) return@playAudio
                        // Logic update highlight index nếu cần
                        // _currentIndex.value = ...
                    },
                    onComplete = {
                        if (playbackSessionId.get() == currentSession) {
                            _isReading.value = false
                            _currentIndex.value = 0
                        }
                    }
                )
            } else {
                Log.e("LiveReader", "TTS Gen Failed")
                _isReading.value = false
            }
        }
    }


    // --- HEAVY LOGIC HELPERS ---

    /**
     * Check trùng lặp toàn cục.
     * Dùng khi scroll thất bại, hình ảnh không đổi.
     */
    private fun isGlobalDuplicate(oldWords: List<OCRWord>, newWords: List<OCRWord>): Boolean {
        if (oldWords.isEmpty()) return false
        if (newWords.isEmpty()) return true

        // Nếu số lượng từ chênh lệch quá nhiều (> 20 từ) -> Khác nhau
        if (kotlin.math.abs(oldWords.size - newWords.size) > 20) return false

        val oldStr = oldWords.joinToString("") { normalize(it.text) }
        val newStr = newWords.joinToString("") { normalize(it.text) }

        val similarity = calculateSimilarity(oldStr, newStr)

        // [TĂNG ĐỘ NHẠY] Giảm ngưỡng xuống 0.6 (60%).
        // Nếu 2 trang giống nhau 60% (do OCR sai nhiều) -> Vẫn coi là trang cũ.
        return similarity > 0.85
    }

    /**
     * Thuật toán ghép chữ SIÊU NẶNG:
     * 1. Anchor cực lớn: 60% cuối trang cũ.
     * 2. Chấp nhận sai số lớn: Similarity > 0.55 (55%).
     */

    private fun normalize(s: String) = s.lowercase().replace(Regex("[^a-z0-9]"), "")

    private fun calculateSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        val longer = if (s1.length > s2.length) s1 else s2
        val shorter = if (s1.length > s2.length) s2 else s1
        if (longer.isEmpty()) return 0.0
        val l = longer.take(500)
        val s = shorter.take(500)
        return (l.length - levenshtein(l, s)) / l.length.toDouble()
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

    private fun findOverlapAndMerge(oldWords: List<OCRWord>, newWords: List<OCRWord>): List<OCRWord> {
        if (oldWords.isEmpty()) return newWords

        // Chuẩn hóa text để so sánh (chỉ lấy chữ cái thường và số)
        fun simple(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

        val oldTextList = oldWords.map { simple(it.text) }
        val newTextList = newWords.map { simple(it.text) }

        // Vùng tìm kiếm: Chỉ xét 50 từ cuối trang cũ và 50 từ đầu trang mới (Optimization)
        val checkRange =  (oldTextList.size*0.5).toInt()
        val oldTailStart = (oldTextList.size - checkRange).coerceAtLeast(0)
        val oldTail = oldTextList.subList(oldTailStart, oldTextList.size)

        val newHeadEnd = checkRange.coerceAtMost(newTextList.size)
        val newHead = newTextList.subList(0, newHeadEnd)

        // Tìm độ dài chồng lặp lớn nhất (Max Overlap)
        var maxOverlapCount = 0

        // Quét overlap: Giả sử overlap i từ.
        // So sánh i từ cuối của Old với i từ đầu của New.
        for (i in minOf(oldTail.size, newHead.size) downTo 3) { // Ít nhất trùng 3 từ liên tiếp mới tính
            val subOld = oldTail.subList(oldTail.size - i, oldTail.size)
            val subNew = newHead.subList(0, i)

            if (subOld == subNew) {
                maxOverlapCount = i
                break // Tìm thấy overlap lớn nhất rồi thì dừng ngay
            }
        }

        if (maxOverlapCount > 0) {
            Log.d("Merge", "🔥 CUT at index $maxOverlapCount (Matched words: ${newWords.take(maxOverlapCount).map { it.text }})")
            // Cắt bỏ phần trùng ở đầu trang mới
            return newWords.subList(maxOverlapCount, newWords.size)
        }

        // FALLBACK: Nếu không tìm thấy overlap chính xác từng từ (do OCR sai 1-2 ký tự)
        // Ta dùng Fuzzy Match cho cả đoạn chuỗi dài
        val oldString = oldTail.joinToString("")
        val newString = newHead.joinToString("")

        // Logic "Lùi dần": Thử cắt dần chuỗi New để xem có khớp đuôi Old không
        // (Đơn giản hóa: Nếu không khớp chính xác, ta thà lấy thừa một chút còn hơn mất chữ,
        // hoặc trả về toàn bộ nếu tin rằng scroll đã đi qua trang mới hoàn toàn).

        // Ở đây, với scrollRatio = 0.85f (rất lớn), khả năng cao là KHÔNG có trùng lặp
        // hoặc trùng lặp rất ít. Nếu không bắt được overlap chính xác, ta trả về toàn bộ.
        Log.d("Merge", "⚠️ No exact overlap found. Assuming continuous text.")
        return newWords
    }


    private fun performScroll(): Boolean {
        val service = ScreenReaderAccessibilityService.instance ?: return false

//        val displayMetrics = context.resources.displayMetrics
//        val screenHeight = displayMetrics.heightPixels.toFloat()
        val centerX = displayMetrics.widthPixels.toFloat() / 2f

        // --- CẤU HÌNH ĐỘ SCROLL TẠI ĐÂY ---
        // 0.75f = Cuộn đi 75% chiều cao màn hình.
        // Giữ lại ~25% nội dung cũ để thuật toán heavySmartMerge tìm được điểm nối.
        // Nếu thấy merge hay bị sai/mất chữ, hãy GIẢM số này xuống (vd: 0.6f).
        // Nếu muốn quét nhanh hơn, hãy TĂNG số này lên (tối đa 0.85f).
        val scrollRatio = 0.4f

        // Điểm bắt đầu vuốt (Gần đáy màn hình - khoảng 90%)
        val swipeStartY = screenHeight * 0.6f

        // Tính khoảng cách cần vuốt
        val scrollDistance = screenHeight * scrollRatio

        // Điểm kết thúc vuốt (Kéo ngón tay lên trên)
        var swipeEndY = swipeStartY - scrollDistance


        if (swipeEndY < 100f) swipeEndY = 100f

        Log.d("LiveReader", "🔄 Performing Scroll: Ratio=$scrollRatio | $swipeStartY -> $swipeEndY")


        return service.performScroll(centerX, swipeStartY, swipeEndY)
    }

    private suspend fun saveBitmapToFile(bitmap: Bitmap): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "temp_ocr.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
        return@withContext file
    }
}
