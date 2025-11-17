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

    // Document Info
    val documentTitle: String? = null,

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
    private val ttsRepository: TTSRepository,
    private val saveDocumentUseCase: com.example.voicereaderapp.domain.usecase.SaveDocumentUseCase,
    private val getDocumentByIdUseCase: com.example.voicereaderapp.domain.usecase.GetDocumentByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PDFViewerUiState())
    val uiState: StateFlow<PDFViewerUiState> = _uiState.asStateFlow()

    private var currentDocumentId: String? = null

    /**
     * Load a saved PDF document from database
     * Used when opening from Continue Listening
     */
    fun loadSavedDocument(documentId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val document = getDocumentByIdUseCase(documentId)
                if (document == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Document not found"
                    )
                    return@launch
                }

                currentDocumentId = documentId

                // Set the saved OCR text and document info
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    documentTitle = document.title,
                    ocrText = document.content
                )

                // Generate audio from saved text
                generateSpeech()

            } catch (e: Exception) {
                android.util.Log.e("PDFViewerViewModel", "Failed to load saved document", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load document: ${e.message}"
                )
            }
        }
    }

    /**
     * Perform OCR on a file (PDF or image)
     */
    fun performOCR(file: File, originalFilename: String? = null) {
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

                    // Automatically save document after successful OCR
                    // Use original filename if provided, otherwise use file.name
                    val filename = originalFilename ?: file.name
                    saveDocumentAfterOCR(filename, result.data.text)
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
     * Save the document to database after OCR completes
     */
    private fun saveDocumentAfterOCR(fileName: String, extractedText: String) {
        viewModelScope.launch {
            try {
                // Generate unique ID if not already set
                if (currentDocumentId == null) {
                    currentDocumentId = "pdf_${System.currentTimeMillis()}"
                }

                // Preserve original filename (just remove extension if present)
                val cleanTitle = fileName
                    .removeSuffix(".pdf")
                    .removeSuffix(".PDF")
                    .trim()
                    .takeIf { it.isNotBlank() } ?: "Imported PDF"

                val document = com.example.voicereaderapp.domain.model.ReadingDocument(
                    id = currentDocumentId!!,
                    title = cleanTitle,
                    content = extractedText,
                    type = com.example.voicereaderapp.domain.model.DocumentType.PDF,
                    createdAt = System.currentTimeMillis(),
                    lastReadPosition = 0
                )

                saveDocumentUseCase(document)
                android.util.Log.d("PDFViewerViewModel", "✅ Document saved: ${document.title} (${document.id})")
            } catch (e: Exception) {
                android.util.Log.e("PDFViewerViewModel", "❌ Failed to save document", e)
                _uiState.value = _uiState.value.copy(
                    error = "Document saved but Continue Listening may not update: ${e.message}"
                )
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

                    // Automatically save document after successful OCR
                    saveDocumentAfterOCR(file.name, result.data.text)
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

    /**
     * Seek to a specific word index
     * Converts word index to time position and seeks audio
     * Continues playing if audio was already playing
     */
    fun seekToWordIndex(wordIndex: Int) {
        val timings = _uiState.value.wordTimings
        if (timings.isEmpty() || wordIndex < 0 || wordIndex >= timings.size) return

        val wasPlaying = _uiState.value.isPlaying
        val targetTiming = timings[wordIndex]

        ttsRepository.seekTo(targetTiming.startMs)

        _uiState.value = _uiState.value.copy(
            currentWordIndex = wordIndex,
            currentPlaybackPosition = targetTiming.startMs
        )

        // Resume playback if it was playing before seek
        if (wasPlaying && !ttsRepository.isPlaying()) {
            ttsRepository.resumeAudio()
        }
    }

    /**
     * Rewind audio by 10 seconds
     * Continues playing if audio was already playing
     */
    fun rewind() {
        val audioBase64 = _uiState.value.audioBase64
        if (audioBase64 == null) return

        val wasPlaying = _uiState.value.isPlaying
        val currentPos = ttsRepository.getCurrentPosition()
        val newPos = (currentPos - 10000).coerceAtLeast(0L)

        ttsRepository.seekTo(newPos)

        // Find corresponding word index
        val timings = _uiState.value.wordTimings
        val newWordIndex = timings.indexOfLast { it.startMs <= newPos }

        _uiState.value = _uiState.value.copy(
            currentWordIndex = newWordIndex,
            currentPlaybackPosition = newPos
        )

        // Resume playback if it was playing before rewind
        if (wasPlaying && !ttsRepository.isPlaying()) {
            ttsRepository.resumeAudio()
        }
    }

    /**
     * Forward audio by 10 seconds
     * Continues playing if audio was already playing
     */
    fun forward() {
        val audioBase64 = _uiState.value.audioBase64
        if (audioBase64 == null) return

        val wasPlaying = _uiState.value.isPlaying
        val currentPos = ttsRepository.getCurrentPosition()
        val duration = ttsRepository.getDuration()
        val newPos = (currentPos + 10000).coerceAtMost(duration)

        ttsRepository.seekTo(newPos)

        // Find corresponding word index
        val timings = _uiState.value.wordTimings
        val newWordIndex = timings.indexOfLast { it.startMs <= newPos }

        _uiState.value = _uiState.value.copy(
            currentWordIndex = newWordIndex,
            currentPlaybackPosition = newPos
        )

        // Resume playback if it was playing before forward
        if (wasPlaying && !ttsRepository.isPlaying()) {
            ttsRepository.resumeAudio()
        }
    }

    /**
     * Seek to a specific position based on slider fraction (0.0 to 1.0)
     */
    fun seekToFraction(fraction: Float) {
        val timings = _uiState.value.wordTimings
        if (timings.isEmpty()) return

        val targetIndex = (fraction * timings.size).toInt()
            .coerceIn(0, timings.size - 1)

        seekToWordIndex(targetIndex)
    }

    override fun onCleared() {
        super.onCleared()
        ttsRepository.stopAudio()
    }
}
