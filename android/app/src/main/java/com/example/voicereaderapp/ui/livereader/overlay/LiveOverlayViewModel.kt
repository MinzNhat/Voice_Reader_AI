package com.example.voicereaderapp.ui.livereader.overlay

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class VoiceConfig {Male, FeMale}

// để open cho review, review xong thì xóa
open class LiveOverlayViewModel : ViewModel() {
    // Chế độ tương tác Mặc định là false (không tương tác được)
    private val _isInteractive = MutableStateFlow(false)
    val isInteractive: StateFlow<Boolean> = _isInteractive

    private val _isExpanded = MutableStateFlow(false)
    val isExpanded: StateFlow<Boolean> = _isExpanded


    // Overlay đang đọc hay pause
    private val _isReading = MutableStateFlow(false)
    val isReading: StateFlow<Boolean> = _isReading

    // Tốc độ đọc (1.0 = normal)
    private val _speed = MutableStateFlow(1.0f)
    val speed: StateFlow<Float> = _speed

    // Giọng đọc hiện tại
    private val _voiceConfig = MutableStateFlow(VoiceConfig.Male)
    val voiceConfig: StateFlow<VoiceConfig> = _voiceConfig

    // Show note overlay
    private val _isNoteOverlayVisible = MutableStateFlow(false)
    val isNoteOverlayVisible: StateFlow<Boolean> = _isNoteOverlayVisible

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
        _isReading.value = !_isReading.value
    }

    fun setSpeed(newSpeed: Float){
        _speed.value = newSpeed.coerceIn(0.2f, 3.0f)
    }

    fun setVoice(newVoice: VoiceConfig){
        _voiceConfig.value = newVoice
    }

    fun showNoteOverlay() {
        _isNoteOverlayVisible.value = true
    }

    fun hideNoteOverlay() {
        _isNoteOverlayVisible.value = false
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

}
