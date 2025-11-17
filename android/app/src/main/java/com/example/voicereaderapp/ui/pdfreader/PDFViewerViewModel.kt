package com.example.voicereaderapp.ui.pdfreader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereaderapp.data.remote.model.OCRWord
import com.example.voicereaderapp.data.remote.model.WordTiming
import com.example.voicereaderapp.domain.repository.OCRRepository
import com.example.voicereaderapp.domain.repository.TTSRepository
import com.example.voicereaderapp.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * UI State for PDF Viewer with OCR and TTS
 */
data class PDFViewerUiState(
    val isLoading: Boolean = false,
    val error: String? = null,

    // OCR State
    val ocrText: String? = null,
    val ocrWords: List<OCRWord> = emptyList(),
    val isOCRProcessing: Boolean = false,
    val ocrImageWidth: Int = 0,  // CRITICAL: OCR image dimensions for coordinate scaling
    val ocrImageHeight: Int = 0,

    // TTS State
    val audioBase64: String? = null,
    val wordTimings: List<WordTiming> = emptyList(),
    val isGeneratingAudio: Boolean = false,
    val isPlaying: Boolean = false,
    val selectedSpeaker: String = "matt",
    val playbackSpeed: Float = 1.0f,

    // Real-time Highlighting State
    val currentWordIndex: Int = -1,
    val currentPlaybackPosition: Long = 0
)

/**
 * ViewModel for PDF Viewer with OCR and TTS
 * Integrates PDF rendering, OCR processing, TTS generation, and real-time highlighting
 */
@HiltViewModel
class PDFViewerViewModel @Inject constructor(
    private val ocrRepository: OCRRepository,
    private val ttsRepository: TTSRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PDFViewerUiState())
    val uiState: StateFlow<PDFViewerUiState> = _uiState.asStateFlow()

    /**
     * Perform OCR on a file (PDF or image)
     */
    fun performOCR(file: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isOCRProcessing = true,
                error = null
            )

            when (val result = ocrRepository.performOCR(file)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isOCRProcessing = false,
                        ocrText = result.data.text,
                        ocrWords = result.data.words,
                        ocrImageWidth = result.data.imageWidth,
                        ocrImageHeight = result.data.imageHeight
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isOCRProcessing = false,
                        error = result.exception.message ?: "OCR failed"
                    )
                }
                is Result.Loading -> {
                    // Already in loading state
                }
            }
        }
    }

    /**
     * Perform OCR on a cropped region of an image
     */
    fun performOCRWithCrop(
        file: File,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isOCRProcessing = true,
                error = null
            )

            when (val result = ocrRepository.performOCRWithCrop(file, x, y, width, height)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isOCRProcessing = false,
                        ocrText = result.data.text,
                        ocrWords = result.data.words,
                        ocrImageWidth = result.data.imageWidth,
                        ocrImageHeight = result.data.imageHeight
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isOCRProcessing = false,
                        error = result.exception.message ?: "OCR crop failed"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Generate speech from extracted text
     * Also fetches word timings for highlighting
     * Default speaker: "matt" (male English voice)
     */
    fun generateSpeech(speaker: String = "matt") {
        val text = _uiState.value.ocrText ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGeneratingAudio = true,
                error = null
            )

            // Generate TTS audio
            when (val audioResult = ttsRepository.generateSpeech(text, speaker)) {
                is Result.Success -> {
                    // Get word timings
                    when (val timingResult = ttsRepository.getWordTimings(text)) {
                        is Result.Success -> {
                            _uiState.value = _uiState.value.copy(
                                isGeneratingAudio = false,
                                audioBase64 = audioResult.data,
                                wordTimings = timingResult.data.timings
                            )
                        }
                        is Result.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isGeneratingAudio = false,
                                audioBase64 = audioResult.data,
                                error = "Timing calculation failed"
                            )
                        }
                        is Result.Loading -> {}
                    }
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isGeneratingAudio = false,
                        error = audioResult.exception.message ?: "TTS generation failed"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    /**
     * Play generated audio with real-time word highlighting
     */
    fun playAudio() {
        val audioBase64 = _uiState.value.audioBase64 ?: return
        val timings = _uiState.value.wordTimings
        val speed = _uiState.value.playbackSpeed

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPlaying = true)

            ttsRepository.playAudio(
                base64Audio = audioBase64,
                playbackSpeed = speed,
                onProgress = { currentMs ->
                    // Update current playback position
                    _uiState.value = _uiState.value.copy(
                        currentPlaybackPosition = currentMs
                    )

                    // Find current word based on timing
                    val currentWordIndex = timings.indexOfLast { timing ->
                        currentMs >= timing.startMs && currentMs < timing.endMs
                    }

                    if (currentWordIndex != _uiState.value.currentWordIndex) {
                        _uiState.value = _uiState.value.copy(
                            currentWordIndex = currentWordIndex
                        )
                    }
                },
                onComplete = {
                    _uiState.value = _uiState.value.copy(
                        isPlaying = false,
                        currentWordIndex = -1,
                        currentPlaybackPosition = 0
                    )
                }
            )
        }
    }

    /**
     * Pause audio playback
     */
    fun pauseAudio() {
        ttsRepository.pauseAudio()
        _uiState.value = _uiState.value.copy(isPlaying = false)
    }

    /**
     * Resume audio playback
     */
    fun resumeAudio() {
        ttsRepository.resumeAudio()
        _uiState.value = _uiState.value.copy(isPlaying = true)
    }

    /**
     * Stop audio playback
     */
    fun stopAudio() {
        ttsRepository.stopAudio()
        _uiState.value = _uiState.value.copy(
            isPlaying = false,
            currentWordIndex = -1,
            currentPlaybackPosition = 0
        )
    }

    /**
     * Set playback speed
     */
    fun setPlaybackSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
        ttsRepository.setPlaybackSpeed(speed)
    }

    /**
     * Set speaker voice
     */
    fun setSpeaker(speaker: String) {
        _uiState.value = _uiState.value.copy(
            selectedSpeaker = speaker,
            audioBase64 = null // Clear old audio when voice changes
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        ttsRepository.stopAudio()
    }
}
