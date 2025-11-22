package com.example.voicereaderapp.ui.pdfreader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereaderapp.data.remote.model.OCRWord
import com.example.voicereaderapp.data.remote.model.WordTiming
import com.example.voicereaderapp.domain.model.DocumentType
import com.example.voicereaderapp.domain.model.ReadingDocument
import com.example.voicereaderapp.domain.repository.OCRRepository
import com.example.voicereaderapp.domain.repository.TTSRepository
import com.example.voicereaderapp.domain.usecase.AskRagUseCase
import com.example.voicereaderapp.domain.usecase.GetDocumentByIdUseCase
import com.example.voicereaderapp.domain.usecase.GetVoiceSettingsUseCase
import com.example.voicereaderapp.domain.usecase.SaveDocumentUseCase
import com.example.voicereaderapp.utils.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.voicereaderapp.domain.model.ChatMessage
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
    val documentId: String? = null,
    val documentTitle: String? = null,

    // OCR State
    val ocrText: String? = null,
    val ocrWords: List<OCRWord> = emptyList(),
    val isOCRProcessing: Boolean = false,
    val ocrImageWidth: Int = 0,
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
    val currentPlaybackPosition: Long = 0,

    // --- CHATBOT STATE ---
    val chatMessages: List<ChatMessage> = emptyList(),
    val isChatLoading: Boolean = false
)

/**
 * ViewModel for PDF Viewer with OCR, TTS, and Chatbot
 */
@HiltViewModel
class PDFViewerViewModel @Inject constructor(
    private val ocrRepository: OCRRepository,
    private val ttsRepository: TTSRepository,
    private val saveDocumentUseCase: SaveDocumentUseCase,
    private val getDocumentByIdUseCase: GetDocumentByIdUseCase,
    private val getVoiceSettingsUseCase: GetVoiceSettingsUseCase,
    private val askRagUseCase: AskRagUseCase // UseCase để hỏi AI
) : ViewModel() {

    private val _uiState = MutableStateFlow(PDFViewerUiState())
    val uiState: StateFlow<PDFViewerUiState> = _uiState.asStateFlow()

    private var currentDocumentId: String? = null
    private val gson = Gson()

    init {
        // Load global voice settings
        viewModelScope.launch {
            getVoiceSettingsUseCase().collect { settings ->
                val currentState = _uiState.value

                val voiceToUse = if (settings.useMainVoiceForAll) settings.mainVoiceId else {
                    if (currentState.selectedSpeaker == "matt" && currentState.selectedLanguage == "en-US") settings.voiceId else currentState.selectedSpeaker
                }

                val speedToUse = if (settings.useMainSpeedForAll) settings.mainSpeed else {
                    if (currentState.playbackSpeed == 1.0f) settings.speed else currentState.playbackSpeed
                }

                val languageToUse = if (settings.useMainVoiceForAll) {
                    when (settings.mainVoiceId) {
                        "nminseo", "nshasha", "nmovie", "nmammon" -> "ko-KR"
                        "danna", "clara", "matt" -> "en-US"
                        else -> settings.language
                    }
                } else {
                    if (currentState.selectedLanguage == "en-US" && currentState.selectedSpeaker == "matt") settings.language else currentState.selectedLanguage
                }

                _uiState.value = currentState.copy(
                    selectedSpeaker = voiceToUse,
                    selectedLanguage = languageToUse,
                    playbackSpeed = speedToUse
                )
            }
        }
    }

    // --- CACHING LOGIC ---

    private fun getCacheKey(voiceId: String, language: String): String {
        return "${voiceId}_${language}"
    }

    private fun getAudioFromCache(document: ReadingDocument, voiceId: String, language: String): AudioCacheEntry? {
        val cacheJson = document.audioCacheJson ?: return null
        return try {
            val cacheKey = getCacheKey(voiceId, language)
            val type = object : TypeToken<Map<String, AudioCacheEntry>>() {}.type
            val cacheMap = gson.fromJson<Map<String, AudioCacheEntry>>(cacheJson, type)
            cacheMap[cacheKey]
        } catch (e: Exception) {
            null
        }
    }

    private fun saveAudioToCache(audioBase64: String, wordTimings: List<WordTiming>, voiceId: String, language: String) {
        if (audioBase64.isEmpty() || wordTimings.isEmpty()) return

        currentDocumentId?.let { docId ->
            viewModelScope.launch {
                try {
                    val document = getDocumentByIdUseCase(docId) ?: return@launch
                    val type = object : TypeToken<Map<String, AudioCacheEntry>>() {}.type
                    val existingJson = document.audioCacheJson

                    val existingCacheMap: Map<String, AudioCacheEntry> = try {
                        if (existingJson.isNullOrEmpty()) emptyMap()
                        else gson.fromJson(existingJson, type) ?: emptyMap()
                    } catch (e: Exception) { emptyMap() }

                    val cacheKey = getCacheKey(voiceId, language)
                    val newCacheMap = existingCacheMap.toMutableMap()
                    newCacheMap[cacheKey] = AudioCacheEntry(audio = audioBase64, timings = wordTimings)

                    val updatedDoc = document.copy(audioCacheJson = gson.toJson(newCacheMap))
                    saveDocumentUseCase(updatedDoc)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // --- MAIN LOGIC ---

    fun loadSavedDocument(documentId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val document = getDocumentByIdUseCase(documentId)

                if (document == null) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Document not found")
                    return@launch
                }

                currentDocumentId = documentId
                _uiState.value = _uiState.value.copy(documentId = documentId)

                val globalSettings = getVoiceSettingsUseCase().first()
                val voiceId = if (globalSettings.useMainVoiceForAll) globalSettings.mainVoiceId else (document.voiceId ?: _uiState.value.selectedSpeaker)
                val speed = if (globalSettings.useMainSpeedForAll) globalSettings.mainSpeed else (document.speed ?: _uiState.value.playbackSpeed)
                val language = if (globalSettings.useMainVoiceForAll) {
                    when (globalSettings.mainVoiceId) {
                        "nminseo", "nshasha", "nmovie", "nmammon" -> "ko-KR"
                        else -> "en-US"
                    }
                } else (document.language ?: _uiState.value.selectedLanguage)

                // Check cache
                val cachedAudio = getAudioFromCache(document, voiceId, language)

                if (cachedAudio != null && cachedAudio.audio.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        documentTitle = document.title,
                        ocrText = document.content,
                        selectedSpeaker = voiceId,
                        selectedLanguage = language,
                        playbackSpeed = speed,
                        audioBase64 = cachedAudio.audio,
                        wordTimings = cachedAudio.timings,
                        currentWordIndex = document.lastReadPosition,
                        currentPlaybackPosition = if (document.lastReadPosition >= 0 && document.lastReadPosition < cachedAudio.timings.size) cachedAudio.timings[document.lastReadPosition].startMs else 0L
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        documentTitle = document.title,
                        ocrText = document.content,
                        selectedSpeaker = voiceId,
                        selectedLanguage = language,
                        playbackSpeed = speed,
                        currentWordIndex = document.lastReadPosition,
                        audioBase64 = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun performOCR(file: File, originalFilename: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOCRProcessing = true, error = null)

            when (val result = ocrRepository.performOCR(file)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isOCRProcessing = false,
                        ocrText = result.data.text,
                        ocrWords = result.data.words,
                        ocrImageWidth = result.data.imageWidth,
                        ocrImageHeight = result.data.imageHeight
                    )
                    val filename = originalFilename ?: file.name
                    saveDocumentAfterOCR(filename, result.data.text)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isOCRProcessing = false, error = result.exception.message)
                }
                is Result.Loading -> {}
            }
        }
    }

    private fun saveDocumentAfterOCR(fileName: String, extractedText: String) {
        viewModelScope.launch {
            try {
                val extension = fileName.substringAfterLast('.', "").lowercase()
                val type = if (extension == "pdf") DocumentType.PDF else DocumentType.IMAGE

                if (currentDocumentId == null) {
                    currentDocumentId = "${if(type == DocumentType.IMAGE) "img" else "pdf"}_${System.currentTimeMillis()}"
                }

                val cleanTitle = fileName.substringBeforeLast('.')
                val document = ReadingDocument(
                    id = currentDocumentId!!,
                    title = cleanTitle,
                    content = extractedText,
                    type = type,
                    createdAt = System.currentTimeMillis(),
                    lastReadPosition = 0,
                    voiceId = _uiState.value.selectedSpeaker,
                    language = _uiState.value.selectedLanguage,
                    speed = _uiState.value.playbackSpeed
                )
                saveDocumentUseCase(document)
                _uiState.value = _uiState.value.copy(documentId = currentDocumentId, documentTitle = cleanTitle)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun performOCRWithCrop(file: File, x: Int, y: Int, width: Int, height: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOCRProcessing = true, error = null)
            when (val result = ocrRepository.performOCRWithCrop(file, x, y, width, height)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isOCRProcessing = false,
                        ocrText = result.data.text,
                        ocrWords = result.data.words,
                        ocrImageWidth = result.data.imageWidth,
                        ocrImageHeight = result.data.imageHeight
                    )
                    saveDocumentAfterOCR(file.name, result.data.text)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isOCRProcessing = false, error = result.exception.message)
                }
                is Result.Loading -> {}
            }
        }
    }

    fun generateSpeech(speaker: String? = null, restorePosition: Int? = null, autoPlay: Boolean = false) {
        val text = _uiState.value.ocrText ?: return
        val selectedSpeaker = speaker ?: _uiState.value.selectedSpeaker

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingAudio = true, error = null)

            when (val audioResult = ttsRepository.generateSpeech(text, selectedSpeaker)) {
                is Result.Success -> {
                    when (val timingResult = ttsRepository.getWordTimings(text)) {
                        is Result.Success -> {
                            val audioBase64 = audioResult.data
                            val timings = timingResult.data.timings

                            _uiState.value = _uiState.value.copy(
                                isGeneratingAudio = false,
                                audioBase64 = audioBase64,
                                wordTimings = timings
                            )
                            saveAudioToCache(audioBase64, timings, selectedSpeaker, _uiState.value.selectedLanguage)

                            restorePosition?.let { pos ->
                                if (pos in timings.indices) {
                                    ttsRepository.seekTo(timings[pos].startMs)
                                    _uiState.value = _uiState.value.copy(currentWordIndex = pos, currentPlaybackPosition = timings[pos].startMs)
                                }
                            }
                            if (autoPlay) playAudio()
                        }
                        is Result.Error -> {
                            _uiState.value = _uiState.value.copy(isGeneratingAudio = false, error = "Timing failed")
                        }
                        is Result.Loading -> {}
                    }
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isGeneratingAudio = false, error = audioResult.exception.message)
                }
                is Result.Loading -> {}
            }
        }
    }

    fun playAudio() {
        val audioBase64 = _uiState.value.audioBase64 ?: return
        val timings = _uiState.value.wordTimings
        val speed = _uiState.value.playbackSpeed

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isPlaying = true, error = null)
                val startPos = _uiState.value.currentPlaybackPosition

                ttsRepository.playAudio(
                    base64Audio = audioBase64,
                    playbackSpeed = speed,
                    onProgress = { currentMs ->
                        _uiState.value = _uiState.value.copy(currentPlaybackPosition = currentMs)
                        val wordIndex = timings.indexOfLast { it.startMs <= currentMs && it.endMs > currentMs }
                        if (wordIndex != -1 && wordIndex != _uiState.value.currentWordIndex) {
                            _uiState.value = _uiState.value.copy(currentWordIndex = wordIndex)
                            if (wordIndex % 10 == 0) saveReadingPosition(wordIndex)
                        }
                    },
                    onComplete = {
                        saveReadingPosition(_uiState.value.currentWordIndex)
                        _uiState.value = _uiState.value.copy(isPlaying = false, currentWordIndex = -1, currentPlaybackPosition = 0)
                    }
                )
                if (startPos > 0) ttsRepository.seekTo(startPos)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isPlaying = false, error = e.message)
            }
        }
    }

    fun pauseAudio() {
        ttsRepository.pauseAudio()
        _uiState.value = _uiState.value.copy(isPlaying = false)
        saveReadingPosition(_uiState.value.currentWordIndex)
    }

    fun resumeAudio() {
        ttsRepository.resumeAudio()
        _uiState.value = _uiState.value.copy(isPlaying = true)
    }

    fun stopAudio() {
        ttsRepository.stopAudio()
        _uiState.value = _uiState.value.copy(isPlaying = false, currentWordIndex = -1, currentPlaybackPosition = 0)
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.value = _uiState.value.copy(playbackSpeed = speed)
        ttsRepository.setPlaybackSpeed(speed)
        currentDocumentId?.let { docId ->
            viewModelScope.launch {
                getDocumentByIdUseCase(docId)?.let {
                    saveDocumentUseCase(it.copy(speed = speed))
                }
            }
        }
    }

    fun setVoiceAndLanguage(voiceId: String, language: String) {
        currentDocumentId?.let { docId ->
            viewModelScope.launch {
                val doc = getDocumentByIdUseCase(docId) ?: return@launch
                val cached = getAudioFromCache(doc, voiceId, language)

                if (cached != null) {
                    _uiState.value = _uiState.value.copy(
                        selectedSpeaker = voiceId,
                        selectedLanguage = language,
                        audioBase64 = cached.audio,
                        wordTimings = cached.timings
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        selectedSpeaker = voiceId,
                        selectedLanguage = language,
                        audioBase64 = null
                    )
                    if (_uiState.value.ocrText != null) generateSpeech(voiceId)
                }
                saveDocumentUseCase(doc.copy(voiceId = voiceId, language = language))
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun saveReadingPosition(wordIndex: Int) {
        currentDocumentId?.let { docId ->
            viewModelScope.launch {
                getDocumentByIdUseCase(docId)?.let {
                    saveDocumentUseCase(it.copy(lastReadPosition = wordIndex.coerceAtLeast(0)))
                }
            }
        }
    }

    fun seekToWordIndex(wordIndex: Int) {
        val timings = _uiState.value.wordTimings
        if (timings.isEmpty() || wordIndex !in timings.indices) return

        val target = timings[wordIndex]
        ttsRepository.seekTo(target.startMs)
        _uiState.value = _uiState.value.copy(currentWordIndex = wordIndex, currentPlaybackPosition = target.startMs)
        saveReadingPosition(wordIndex)
        if (_uiState.value.isPlaying && !ttsRepository.isPlaying()) ttsRepository.resumeAudio()
    }

    fun rewind() {
        val currentPos = ttsRepository.getCurrentPosition()
        val newPos = (currentPos - 5000).coerceAtLeast(0)
        ttsRepository.seekTo(newPos)

        val timings = _uiState.value.wordTimings
        val newIndex = timings.indexOfLast { it.startMs <= newPos }
        _uiState.value = _uiState.value.copy(currentWordIndex = newIndex, currentPlaybackPosition = newPos)
    }

    fun forward() {
        val currentPos = ttsRepository.getCurrentPosition()
        val newPos = currentPos + 5000
        ttsRepository.seekTo(newPos)

        val timings = _uiState.value.wordTimings
        val newIndex = timings.indexOfLast { it.startMs <= newPos }
        _uiState.value = _uiState.value.copy(currentWordIndex = newIndex, currentPlaybackPosition = newPos)
    }

    fun seekToFraction(fraction: Float) {
        val timings = _uiState.value.wordTimings
        if (timings.isEmpty()) return
        val index = (fraction * timings.size).toInt().coerceIn(timings.indices)
        seekToWordIndex(index)
    }

    override fun onCleared() {
        super.onCleared()
        saveReadingPosition(_uiState.value.currentWordIndex)
        ttsRepository.stopAudio()
    }

    // --- 🔥 CHATBOT FUNCTION ---
    fun askAi(question: String) {
        viewModelScope.launch {
            val currentList = _uiState.value.chatMessages.toMutableList()
            currentList.add(ChatMessage(question, true))

            _uiState.value = _uiState.value.copy(
                chatMessages = currentList,
                isChatLoading = true
            )

            askRagUseCase(question)
                .onSuccess { answer ->
                    val updatedList = _uiState.value.chatMessages.toMutableList()
                    updatedList.add(ChatMessage(answer, false))
                    _uiState.value = _uiState.value.copy(chatMessages = updatedList, isChatLoading = false)
                }
                .onFailure {
                    val updatedList = _uiState.value.chatMessages.toMutableList()
                    updatedList.add(ChatMessage("Lỗi: ${it.message}", false))
                    _uiState.value = _uiState.value.copy(chatMessages = updatedList, isChatLoading = false)
                }
        }
    }
}
