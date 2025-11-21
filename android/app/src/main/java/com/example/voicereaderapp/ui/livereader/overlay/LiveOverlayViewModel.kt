package com.example.voicereaderapp.ui.livereader.overlay

import com.example.voicereaderapp.domain.repository.TTSRepository
import com.example.voicereaderapp.domain.repository.OCRRepository
import com.example.voicereaderapp.domain.usecase.GetVoiceSettingsUseCase
import com.example.voicereaderapp.domain.usecase.UpdateVoiceSettingsUseCase
import com.example.voicereaderapp.domain.model.TTSVoice
import com.example.voicereaderapp.domain.model.VoiceGender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import javax.inject.Inject

// Deprecated: Use TTSVoice from domain model instead
@Deprecated("Use TTSVoice enum with proper voice IDs")
enum class VoiceConfig {Male, FeMale}

/**
 * State manager for Live Overlay Service
 * Integrated with existing VoiceSettings and backend TTS
 *
 * Note: Not a ViewModel because it's used in a Service context
 * Uses CoroutineScope with SupervisorJob for proper lifecycle management
 */
class LiveOverlayViewModel @Inject constructor(
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
        _isReading.value = !_isReading.value
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

}
