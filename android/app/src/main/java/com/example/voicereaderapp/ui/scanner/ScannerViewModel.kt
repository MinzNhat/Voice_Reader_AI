package com.example.voicereaderapp.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereaderapp.domain.model.DocumentType
import com.example.voicereaderapp.domain.model.ReadingDocument
import com.example.voicereaderapp.domain.repository.OCRRepository
import com.example.voicereaderapp.domain.repository.TTSRepository
import com.example.voicereaderapp.domain.usecase.SaveDocumentUseCase
import com.example.voicereaderapp.utils.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * UI state for Scanner screen.
 *
 * @property isProcessing Whether OCR is processing
 * @property extractedText Extracted text from scanned image
 * @property error Error message if any
 * @property audioBase64 Generated audio in base64 format
 * @property isGeneratingAudio Whether TTS is generating audio
 * @property documentId ID of the saved document
 */
data class ScannerUiState(
    val isProcessing: Boolean = false,
    val extractedText: String? = null,
    val error: String? = null,
    val audioBase64: String? = null,
    val isGeneratingAudio: Boolean = false,
    val documentId: String? = null
)

/**
 * ViewModel for Scanner screen.
 * Manages image scanning, OCR processing, and TTS generation.
 *
 * @property saveDocumentUseCase Use case for saving scanned documents
 * @property ocrRepository Repository for OCR text extraction
 * @property ttsRepository Repository for TTS audio generation
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val saveDocumentUseCase: SaveDocumentUseCase,
    private val ocrRepository: OCRRepository,
    private val ttsRepository: TTSRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScannerUiState())
    
    /**
     * Current UI state of the scanner screen.
     */
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    /**
     * Resets the UI state to initial values.
     * Call this when entering the scanner screen.
     */
    fun resetState() {
        _uiState.value = ScannerUiState()
        android.util.Log.d("ScannerViewModel", "State reset")
    }

    /**
     * Opens camera for taking a photo.
     * TODO: Implement camera integration
     */
    fun openCamera() {
        // TODO: Implement camera intent
        _uiState.value = _uiState.value.copy(isProcessing = true)
    }

    /**
     * Opens gallery for selecting an image.
     * TODO: Implement gallery integration
     */
    fun openGallery() {
        // TODO: Implement gallery intent
        _uiState.value = _uiState.value.copy(isProcessing = true)
    }

    /**
     * Processes scanned image and extracts text using OCR.
     * After successful OCR, automatically generates TTS audio.
     *
     * @param imagePath Path to the scanned image
     */
    fun processImage(imagePath: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isProcessing = true,
                    error = null
                )

                val imageFile = File(imagePath)
                if (!imageFile.exists()) {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        error = "Image file not found"
                    )
                    return@launch
                }

                // Step 1: Perform OCR
                android.util.Log.d("ScannerViewModel", "Starting OCR on image: ${imageFile.name}")
                when (val ocrResult = ocrRepository.performOCR(imageFile)) {
                    is Result.Success -> {
                        val extractedText = ocrResult.data.text
                        android.util.Log.d("ScannerViewModel", "OCR successful, extracted ${extractedText.length} characters")

                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            extractedText = extractedText
                        )

                        // Step 2: Save document with extracted text
                        val documentId = saveDocument(imageFile.name, extractedText)

                        // Step 3: Generate TTS audio automatically
                        if (extractedText.isNotBlank()) {
                            generateSpeech(extractedText, documentId)
                        }
                    }
                    is Result.Error -> {
                        android.util.Log.e("ScannerViewModel", "OCR failed: ${ocrResult.exception.message}")
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            error = "OCR failed: ${ocrResult.exception.message}"
                        )
                    }
                    is Result.Loading -> {
                        // Already in loading state
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("ScannerViewModel", "Error processing image", e)
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    /**
     * Saves the extracted text as a document.
     * Returns the document ID for later use.
     */
    private suspend fun saveDocument(fileName: String, extractedText: String): String {
        return try {
            // Clean up filename
            val cleanTitle = fileName
                .removeSuffix(".jpg")
                .removeSuffix(".JPG")
                .removeSuffix(".jpeg")
                .removeSuffix(".JPEG")
                .removeSuffix(".png")
                .removeSuffix(".PNG")
                .trim()
                .takeIf { it.isNotBlank() } ?: "Scanned Image"

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val documentId = "img_$timestamp"

            val document = ReadingDocument(
                id = documentId,
                title = cleanTitle,
                content = extractedText,
                type = DocumentType.IMAGE,
                createdAt = System.currentTimeMillis(),
                lastReadPosition = 0,
                voiceId = "matt",  // Default voice
                language = "en-US",  // Default language
                speed = 1.0f
            )

            saveDocumentUseCase(document)
            _uiState.value = _uiState.value.copy(documentId = documentId)

            android.util.Log.d("ScannerViewModel", "Document saved with ID: $documentId")
            documentId
        } catch (e: Exception) {
            android.util.Log.e("ScannerViewModel", "Failed to save document", e)
            _uiState.value = _uiState.value.copy(
                error = "Failed to save document: ${e.message}"
            )
            ""
        }
    }

    /**
     * Generates TTS audio from extracted text.
     */
    private fun generateSpeech(text: String, documentId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isGeneratingAudio = true,
                    error = null
                )

                android.util.Log.d("ScannerViewModel", "Generating TTS audio with voice: matt, language: en-US")

                // Generate TTS audio with default voice (matt) and English language
                when (val audioResult = ttsRepository.generateSpeech(
                    text = text,
                    speaker = "matt",
                    language = "en-US"
                )) {
                    is Result.Success -> {
                        val audioBase64 = audioResult.data

                        android.util.Log.d("ScannerViewModel", "TTS audio generated successfully")

                        _uiState.value = _uiState.value.copy(
                            isGeneratingAudio = false,
                            audioBase64 = audioBase64
                        )

                        // Note: Audio caching is handled by the reader screen when user plays it
                    }
                    is Result.Error -> {
                        android.util.Log.e("ScannerViewModel", "TTS failed: ${audioResult.exception.message}")
                        _uiState.value = _uiState.value.copy(
                            isGeneratingAudio = false,
                            error = "TTS generation failed: ${audioResult.exception.message}"
                        )
                    }
                    is Result.Loading -> {
                        // Already in loading state
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ScannerViewModel", "Error generating speech", e)
                _uiState.value = _uiState.value.copy(
                    isGeneratingAudio = false,
                    error = "Error generating speech: ${e.message}"
                )
            }
        }
    }
}
