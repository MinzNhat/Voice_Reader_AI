package com.example.voicereaderapp.ui.pdfreader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereaderapp.data.remote.model.OCRWord
import com.example.voicereaderapp.data.remote.model.WordTiming
import com.example.voicereaderapp.domain.repository.OCRRepository
import com.example.voicereaderapp.domain.repository.TTSRepository
import com.example.voicereaderapp.utils.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Audio cache entry for a specific voice+language combination
 */
data class AudioCacheEntry(
    val audio: String,  // Base64 audio
    val timings: List<WordTiming>  // Word timings
)

/**
 * UI State for PDF Viewer with OCR and TTS
 */
data class PDFViewerUiState(
    val isLoading: Boolean = false,
    val error: String? = null,

    // Document Info
    val documentId: String? = null,  // Expose documentId for note-taking
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
    val selectedLanguage: String = "en-US",
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
    private val getDocumentByIdUseCase: com.example.voicereaderapp.domain.usecase.GetDocumentByIdUseCase,
    private val getVoiceSettingsUseCase: com.example.voicereaderapp.domain.usecase.GetVoiceSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PDFViewerUiState())
    val uiState: StateFlow<PDFViewerUiState> = _uiState.asStateFlow()

    private var currentDocumentId: String? = null
    private val gson = Gson()
    private var shouldAutoPlay = false

    /**
     * Get cache key for voice+language combination
     */
    private fun getCacheKey(voiceId: String, language: String): String {
        return "${voiceId}_${language}"
    }

    /**
     * Load audio cache for specific voice+language from document
     */
    private fun getAudioFromCache(document: com.example.voicereaderapp.domain.model.ReadingDocument, voiceId: String, language: String): AudioCacheEntry? {
        val cacheJson = document.audioCacheJson ?: return null
        return try {
            val cacheKey = getCacheKey(voiceId, language)
            val type = object : TypeToken<Map<String, AudioCacheEntry>>() {}.type
            val cacheMap = gson.fromJson<Map<String, AudioCacheEntry>>(cacheJson, type)
            cacheMap[cacheKey]
        } catch (e: Exception) {
            android.util.Log.e("PDFViewerViewModel", "Failed to parse audio cache", e)
            null
        }
    }

    /**
     * Save audio cache for specific voice+language to document
     * Preserves existing caches for other voices
     */
    private fun saveAudioToCache(audioBase64: String, wordTimings: List<WordTiming>, voiceId: String, language: String) {
        // Validate audio data before caching
        if (audioBase64.isEmpty()) {
            android.util.Log.e("PDFViewerViewModel", "❌ Cannot cache empty audio!")
            return
        }
        if (wordTimings.isEmpty()) {
            android.util.Log.e("PDFViewerViewModel", "❌ Cannot cache audio without timings!")
            return
        }

        currentDocumentId?.let { docId ->
            viewModelScope.launch {
                try {
                    android.util.Log.d("PDFViewerViewModel", "💾 Caching audio for document: $docId")
                    android.util.Log.d("PDFViewerViewModel", "   - Voice: $voiceId, Language: $language")
                    android.util.Log.d("PDFViewerViewModel", "   - Audio length: ${audioBase64.length}")
                    android.util.Log.d("PDFViewerViewModel", "   - Timings count: ${wordTimings.size}")

                    val document = getDocumentByIdUseCase(docId)
                    if (document == null) {
                        android.util.Log.e("PDFViewerViewModel", "❌ Document not found for caching: $docId")
                        return@launch
                    }

                    // Load existing cache map or create new one
                    val existingCacheMap = try {
                        val type = object : TypeToken<Map<String, AudioCacheEntry>>() {}.type
                        val existingJson = document.audioCacheJson
                        if (existingJson.isNullOrEmpty()) {
                            android.util.Log.d("PDFViewerViewModel", "   - No existing cache, creating new")
                            emptyMap()
                        } else {
                            android.util.Log.d("PDFViewerViewModel", "   - Loading existing cache (${existingJson.length} chars)")
                            gson.fromJson<Map<String, AudioCacheEntry>>(existingJson, type) ?: emptyMap()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PDFViewerViewModel", "   - Failed to parse existing cache, creating new", e)
                        emptyMap()
                    }

                    // Add/update cache for current voice
                    val cacheKey = getCacheKey(voiceId, language)
                    val newCacheMap = existingCacheMap.toMutableMap()
                    newCacheMap[cacheKey] = AudioCacheEntry(audio = audioBase64, timings = wordTimings)

                    android.util.Log.d("PDFViewerViewModel", "   - Cache now contains ${newCacheMap.size} voice(s): ${newCacheMap.keys.joinToString()}")

                    // Serialize back to JSON
                    val updatedCacheJson = gson.toJson(newCacheMap)
                    android.util.Log.d("PDFViewerViewModel", "   - Serialized cache size: ${updatedCacheJson.length} chars")

                    val updatedDoc = document.copy(
                        audioCacheJson = updatedCacheJson
                    )
                    saveDocumentUseCase(updatedDoc)
                    android.util.Log.d("PDFViewerViewModel", "✅ Audio successfully cached for $cacheKey")
                } catch (e: Exception) {
                    android.util.Log.e("PDFViewerViewModel", "❌ Failed to cache audio", e)
                }
            }
        } ?: run {
            android.util.Log.e("PDFViewerViewModel", "❌ Cannot cache audio - currentDocumentId is null!")
        }
    }

    init {
        // Load global voice settings and apply main voice/speed if enabled
        viewModelScope.launch {
            getVoiceSettingsUseCase().collect { settings ->
                val currentState = _uiState.value

                // Apply main voice if enabled, otherwise use current/document-specific voice
                val voiceToUse = if (settings.useMainVoiceForAll) {
                    settings.mainVoiceId
                } else {
                    // Keep current voice if already set, otherwise use global default
                    if (currentState.selectedSpeaker == "matt" && currentState.selectedLanguage == "en-US") {
                        settings.voiceId
                    } else {
                        currentState.selectedSpeaker
                    }
                }

                // Apply main speed if enabled, otherwise use current/document-specific speed
                val speedToUse = if (settings.useMainSpeedForAll) {
                    settings.mainSpeed
                } else {
                    // Keep current speed if already set, otherwise use global default
                    if (currentState.playbackSpeed == 1.0f) {
                        settings.speed
                    } else {
                        currentState.playbackSpeed
                    }
                }

                // Determine language based on voice (if main voice is used)
                val languageToUse = if (settings.useMainVoiceForAll) {
                    // Map main voice to language
                    when (settings.mainVoiceId) {
                        "nminseo", "nshasha", "nmovie", "nmammon" -> "ko-KR"
                        "danna", "clara", "matt" -> "en-US"
                        else -> settings.language
                    }
                } else {
                    // Keep current language if already set, otherwise use global default
                    if (currentState.selectedLanguage == "en-US" && currentState.selectedSpeaker == "matt") {
                        settings.language
                    } else {
                        currentState.selectedLanguage
                    }
                }

                _uiState.value = currentState.copy(
                    selectedSpeaker = voiceToUse,
                    selectedLanguage = languageToUse,
                    playbackSpeed = speedToUse
                )
            }
        }
    }

    /**
     * Load a saved PDF document from database
     * Used when opening from Continue Listening
     * Checks for cached audio to avoid repeated TTS API calls
     * Supports multiple cached versions (one per voice+language)
     */
    fun loadSavedDocument(documentId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                android.util.Log.d("PDFViewerViewModel", "📂 Loading saved document: $documentId")

                val document = getDocumentByIdUseCase(documentId)
                if (document == null) {
                    android.util.Log.e("PDFViewerViewModel", "❌ Document not found in database: $documentId")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Document not found"
                    )
                    return@launch
                }

                currentDocumentId = documentId

                // Update UI state with documentId for note-taking
                _uiState.value = _uiState.value.copy(documentId = documentId)

                android.util.Log.d("PDFViewerViewModel", "✅ Document loaded: ${document.title}")
                android.util.Log.d("PDFViewerViewModel", "   - Content length: ${document.content?.length ?: 0}")
                android.util.Log.d("PDFViewerViewModel", "   - Last position: ${document.lastReadPosition}")
                android.util.Log.d("PDFViewerViewModel", "   - Cache JSON length: ${document.audioCacheJson?.length ?: 0}")

                // Get current global settings to check main voice/speed flags
                val globalSettings = getVoiceSettingsUseCase().first()

                // Load document-specific voice settings, but respect main settings if enabled
                val voiceId = if (globalSettings.useMainVoiceForAll) {
                    globalSettings.mainVoiceId
                } else {
                    document.voiceId ?: _uiState.value.selectedSpeaker
                }

                val language = if (globalSettings.useMainVoiceForAll) {
                    // Map main voice to language
                    when (globalSettings.mainVoiceId) {
                        "nminseo", "nshasha", "nmovie", "nmammon" -> "ko-KR"
                        "danna", "clara", "matt" -> "en-US"
                        else -> document.language ?: _uiState.value.selectedLanguage
                    }
                } else {
                    document.language ?: _uiState.value.selectedLanguage
                }

                val speed = if (globalSettings.useMainSpeedForAll) {
                    globalSettings.mainSpeed
                } else {
                    document.speed ?: _uiState.value.playbackSpeed
                }

                android.util.Log.d("PDFViewerViewModel", "   - Voice settings: $voiceId, $language, speed=$speed")
                android.util.Log.d("PDFViewerViewModel", "   - Main voice/speed enabled: ${globalSettings.useMainVoiceForAll}/${globalSettings.useMainSpeedForAll}")

                // Check if audio is cached for this specific voice+language
                val cachedAudio = getAudioFromCache(document, voiceId, language)

                if (cachedAudio != null) {
                    // Validate cached audio
                    val audioLength = cachedAudio.audio.length
                    val timingsCount = cachedAudio.timings.size

                    android.util.Log.d("PDFViewerViewModel", "✅ Loading cached audio for ${voiceId}_${language}")
                    android.util.Log.d("PDFViewerViewModel", "   - Audio base64 length: $audioLength")
                    android.util.Log.d("PDFViewerViewModel", "   - Word timings count: $timingsCount")

                    if (audioLength == 0) {
                        android.util.Log.e("PDFViewerViewModel", "❌ Cached audio is empty! Will regenerate.")
                        // Fall through to regeneration
                    } else if (timingsCount == 0) {
                        android.util.Log.e("PDFViewerViewModel", "❌ Cached timings are empty! Will regenerate.")
                        // Fall through to regeneration
                    } else {
                        // Load cached audio - no TTS API call needed!
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            documentId = documentId,
                            documentTitle = document.title,
                            ocrText = document.content,
                            selectedSpeaker = voiceId,
                            selectedLanguage = language,
                            playbackSpeed = speed,
                            audioBase64 = cachedAudio.audio,
                            wordTimings = cachedAudio.timings,
                            currentWordIndex = document.lastReadPosition,
                            currentPlaybackPosition = if (document.lastReadPosition >= 0 && document.lastReadPosition < cachedAudio.timings.size) {
                                cachedAudio.timings[document.lastReadPosition].startMs
                            } else {
                                0L
                            }
                        )
                        android.util.Log.d("PDFViewerViewModel", "✅ Cached audio loaded successfully - READY TO PLAY")
                        return@launch
                    }
                }

                // No cached audio for this voice - need to generate it
                android.util.Log.d("PDFViewerViewModel", "⚠️ No cached audio for ${voiceId}_${language} - will call TTS API when user presses play")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    documentId = documentId,
                    documentTitle = document.title,
                    ocrText = document.content,
                    selectedSpeaker = voiceId,
                    selectedLanguage = language,
                    playbackSpeed = speed,
                    currentWordIndex = document.lastReadPosition,
                    audioBase64 = null // Explicitly set to null to trigger generation on play
                )

            } catch (e: Exception) {
                android.util.Log.e("PDFViewerViewModel", "❌ Failed to load saved document", e)
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
                // Detect file type from extension
                val fileExtension = fileName.substringAfterLast('.', "").lowercase()
                val documentType = when (fileExtension) {
                    "jpg", "jpeg", "png", "gif", "bmp", "webp" -> com.example.voicereaderapp.domain.model.DocumentType.IMAGE
                    "pdf" -> com.example.voicereaderapp.domain.model.DocumentType.PDF
                    "doc", "docx" -> com.example.voicereaderapp.domain.model.DocumentType.PDF // Treat DOCX as PDF for now
                    else -> com.example.voicereaderapp.domain.model.DocumentType.PDF // Default to PDF
                }

                // Generate unique ID if not already set
                if (currentDocumentId == null) {
                    val prefix = if (documentType == com.example.voicereaderapp.domain.model.DocumentType.IMAGE) "img" else "pdf"
                    currentDocumentId = "${prefix}_${System.currentTimeMillis()}"
                }

                // Preserve original filename (remove extension)
                val cleanTitle = fileName
                    .removeSuffix(".pdf")
                    .removeSuffix(".PDF")
                    .removeSuffix(".jpg")
                    .removeSuffix(".JPG")
                    .removeSuffix(".jpeg")
                    .removeSuffix(".JPEG")
                    .removeSuffix(".png")
                    .removeSuffix(".PNG")
                    .removeSuffix(".docx")
                    .removeSuffix(".DOCX")
                    .removeSuffix(".doc")
                    .removeSuffix(".DOC")
                    .trim()
                    .takeIf { it.isNotBlank() } ?: "Imported Document"

                val document = com.example.voicereaderapp.domain.model.ReadingDocument(
                    id = currentDocumentId!!,
                    title = cleanTitle,
                    content = extractedText,
                    type = documentType,
                    createdAt = System.currentTimeMillis(),
                    lastReadPosition = 0,
                    voiceId = _uiState.value.selectedSpeaker,
                    language = _uiState.value.selectedLanguage,
                    speed = _uiState.value.playbackSpeed
                )

                saveDocumentUseCase(document)

                // Update UI state with documentId so notes can be saved immediately
                _uiState.value = _uiState.value.copy(
                    documentId = currentDocumentId,
                    documentTitle = cleanTitle
                )

                android.util.Log.d("PDFViewerViewModel", "✅ Document saved: ${document.title} (${document.id}) - Type: ${documentType}")
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
     * Uses the currently selected speaker from UI state
     * Caches audio to database to avoid repeated API calls
     * @param restorePosition If provided, seeks to this word index after audio is generated
     * @param autoPlay If true, automatically starts playback after generation
     */
    fun generateSpeech(speaker: String? = null, restorePosition: Int? = null, autoPlay: Boolean = false) {
        val text = _uiState.value.ocrText ?: return
        // Use provided speaker or fall back to currently selected speaker
        val selectedSpeaker = speaker ?: _uiState.value.selectedSpeaker

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGeneratingAudio = true,
                error = null
            )

            android.util.Log.d("PDFViewerViewModel", "🔊 Calling TTS API...")

            // Generate TTS audio with the selected speaker
            when (val audioResult = ttsRepository.generateSpeech(text, selectedSpeaker)) {
                is Result.Success -> {
                    // Get word timings
                    when (val timingResult = ttsRepository.getWordTimings(text)) {
                        is Result.Success -> {
                            val audioBase64 = audioResult.data
                            val wordTimings = timingResult.data.timings

                            _uiState.value = _uiState.value.copy(
                                isGeneratingAudio = false,
                                audioBase64 = audioBase64,
                                wordTimings = wordTimings
                            )

                            // Save audio to database for caching (preserves other voice caches)
                            saveAudioToCache(audioBase64, wordTimings, selectedSpeaker, _uiState.value.selectedLanguage)

                            // If restorePosition is provided, seek to that position
                            restorePosition?.let { position ->
                                if (position > 0 && position < wordTimings.size) {
                                    // Seek to the saved position without starting playback
                                    val targetTiming = wordTimings[position]
                                    ttsRepository.seekTo(targetTiming.startMs)
                                    _uiState.value = _uiState.value.copy(
                                        currentWordIndex = position,
                                        currentPlaybackPosition = targetTiming.startMs
                                    )
                                    android.util.Log.d("PDFViewerViewModel", "Restored position: $position")
                                }
                            }

                            // Auto-play if requested
                            if (autoPlay) {
                                android.util.Log.d("PDFViewerViewModel", "Auto-playing after generation")
                                playAudio()
                            }
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
        val audioBase64 = _uiState.value.audioBase64 ?: run {
            android.util.Log.e("PDFViewerViewModel", "❌ Cannot play audio - audioBase64 is null")
            _uiState.value = _uiState.value.copy(
                error = "Audio not available. Please regenerate audio."
            )
            return
        }
        val timings = _uiState.value.wordTimings
        val speed = _uiState.value.playbackSpeed

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isPlaying = true, error = null)

                android.util.Log.d("PDFViewerViewModel", "🎵 Starting playback - audio length: ${audioBase64.length}, speed: $speed")

                // If we have a non-zero current position (from restore or seek), start from there
                val startPosition = _uiState.value.currentPlaybackPosition
                android.util.Log.d("PDFViewerViewModel", "📍 Start position: ${startPosition}ms")

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
                            // Save position every 10 words to avoid too many database writes
                            if (currentWordIndex % 10 == 0) {
                                saveReadingPosition(currentWordIndex)
                            }
                        }
                    },
                    onComplete = {
                        // Save final position before resetting
                        saveReadingPosition(_uiState.value.currentWordIndex)
                        _uiState.value = _uiState.value.copy(
                            isPlaying = false,
                            currentWordIndex = -1,
                            currentPlaybackPosition = 0
                        )
                        android.util.Log.d("PDFViewerViewModel", "✅ Playback completed")
                    }
                )

                // If we had a non-zero start position, seek to it after starting playback
                if (startPosition > 0) {
                    android.util.Log.d("PDFViewerViewModel", "⏩ Seeking to saved position: ${startPosition}ms")
                    ttsRepository.seekTo(startPosition)
                }

                android.util.Log.d("PDFViewerViewModel", "✅ Audio playback started successfully")

            } catch (e: Exception) {
                android.util.Log.e("PDFViewerViewModel", "❌ Audio playback failed", e)
                _uiState.value = _uiState.value.copy(
                    isPlaying = false,
                    error = "Failed to play audio: ${e.message}. Please try regenerating the audio."
                )
            }
        }
    }

    /**
     * Pause audio playback
     */
    fun pauseAudio() {
        ttsRepository.pauseAudio()
        _uiState.value = _uiState.value.copy(isPlaying = false)
        // Save position when pausing
        saveReadingPosition(_uiState.value.currentWordIndex)
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

        // Update the document in database with new speed
        currentDocumentId?.let { docId ->
            viewModelScope.launch {
                try {
                    val document = getDocumentByIdUseCase(docId)
                    document?.let {
                        val updatedDoc = it.copy(
                            voiceId = _uiState.value.selectedSpeaker,
                            language = _uiState.value.selectedLanguage,
                            speed = speed
                        )
                        saveDocumentUseCase(updatedDoc)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PDFViewerViewModel", "Failed to update document speed", e)
                }
            }
        }
    }

    /**
     * Set speaker voice and language
     * Checks if audio is cached for new voice, otherwise generates it
     * NEVER clears existing caches - preserves audio for all voices!
     */
    fun setVoiceAndLanguage(voiceId: String, language: String) {
        currentDocumentId?.let { docId ->
            viewModelScope.launch {
                try {
                    val document = getDocumentByIdUseCase(docId)
                    document?.let {
                        // Check if audio is cached for this voice+language
                        val cachedAudio = getAudioFromCache(it, voiceId, language)

                        if (cachedAudio != null) {
                            // Load cached audio - no TTS API call!
                            android.util.Log.d("PDFViewerViewModel", "✅ Voice changed to ${voiceId}_${language} - using cached audio (NO API CALL)")

                            _uiState.value = _uiState.value.copy(
                                selectedSpeaker = voiceId,
                                selectedLanguage = language,
                                audioBase64 = cachedAudio.audio,
                                wordTimings = cachedAudio.timings
                            )
                        } else {
                            // No cached audio for this voice - need to generate it
                            android.util.Log.d("PDFViewerViewModel", "⚠️ Voice changed to ${voiceId}_${language} - generating audio (API CALL)")

                            _uiState.value = _uiState.value.copy(
                                selectedSpeaker = voiceId,
                                selectedLanguage = language,
                                audioBase64 = null // Clear UI while generating
                            )

                            // Generate audio with new voice
                            if (_uiState.value.ocrText != null) {
                                generateSpeech(voiceId)
                            }
                        }

                        // Update the document with new voice settings (keeps all cached audio)
                        val updatedDoc = it.copy(
                            voiceId = voiceId,
                            language = language,
                            speed = _uiState.value.playbackSpeed
                            // audioCacheJson preserved - never cleared!
                        )
                        saveDocumentUseCase(updatedDoc)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PDFViewerViewModel", "Failed to update document voice settings", e)
                }
            }
        }
    }

    /**
     * Set speaker voice only (legacy support)
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
     * Save current reading position to database
     */
    private fun saveReadingPosition(wordIndex: Int) {
        currentDocumentId?.let { docId ->
            viewModelScope.launch {
                try {
                    val document = getDocumentByIdUseCase(docId)
                    document?.let {
                        val updatedDoc = it.copy(
                            lastReadPosition = wordIndex.coerceAtLeast(0)
                        )
                        saveDocumentUseCase(updatedDoc)
                        android.util.Log.d("PDFViewerViewModel", "Saved position: $wordIndex for doc: $docId")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PDFViewerViewModel", "Failed to save reading position", e)
                }
            }
        }
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

        // Save position after seeking
        saveReadingPosition(wordIndex)

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
        val newPos = (currentPos - 5000).coerceAtLeast(0L)

        ttsRepository.seekTo(newPos)

        // Find corresponding word index
        val timings = _uiState.value.wordTimings
        val newWordIndex = timings.indexOfLast { it.startMs <= newPos }

        _uiState.value = _uiState.value.copy(
            currentWordIndex = newWordIndex,
            currentPlaybackPosition = newPos
        )

        // Save position after rewinding
        saveReadingPosition(newWordIndex)

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
        val newPos = (currentPos + 5000).coerceAtMost(duration)

        ttsRepository.seekTo(newPos)

        // Find corresponding word index
        val timings = _uiState.value.wordTimings
        val newWordIndex = timings.indexOfLast { it.startMs <= newPos }

        _uiState.value = _uiState.value.copy(
            currentWordIndex = newWordIndex,
            currentPlaybackPosition = newPos
        )

        // Save position after forwarding
        saveReadingPosition(newWordIndex)

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
        // Position is saved in seekToWordIndex
    }

    override fun onCleared() {
        super.onCleared()
        // Save position before clearing
        saveReadingPosition(_uiState.value.currentWordIndex)
        ttsRepository.stopAudio()
    }
}
