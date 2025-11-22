package com.example.voicereaderapp.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereaderapp.domain.model.DocumentType
import com.example.voicereaderapp.domain.usecase.AskRagUseCase
import com.example.voicereaderapp.domain.usecase.GetDocumentByIdUseCase
import com.example.voicereaderapp.domain.usecase.UpdateReadPositionUseCase
import com.example.voicereaderapp.domain.model.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReaderUiState(
    val isLoading: Boolean = true,
    val documentTitle: String = "Đang tải...",
    val documentContent: String = "",
    val documentType: DocumentType = DocumentType.LIVE_SCREEN,
    val isPlaying: Boolean = false,
    val currentWordIndex: Int = -1,
    val error: String? = null,
    val chatMessages: List<ChatMessage> = emptyList(), // List tin nhắn
    val isChatLoading: Boolean = false
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    // TODO: Inject UseCases để lấy dữ liệu tài liệu
    private val updateReadPositionUseCase: UpdateReadPositionUseCase,
    private val getDocumentByIdUseCase: GetDocumentByIdUseCase,
    private val ttsManager: TtsManager,
    private val askRagUseCase: AskRagUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState

    private var currentDocumentId: String? = null

    fun loadDocument(documentId: String) {
        currentDocumentId = documentId

        viewModelScope.launch {
            try {
                _uiState.value = ReaderUiState(isLoading = true)

                val document = getDocumentByIdUseCase(documentId)

                if (document == null) {
                    _uiState.value = ReaderUiState(
                        isLoading = false,
                        error = "Không tìm thấy tài liệu"
                    )
                    return@launch
                }

                _uiState.value = ReaderUiState(
                    isLoading = false,
                    documentTitle = document.title,
                    documentContent = document.content,
                    documentType = document.type,
                    currentWordIndex = document.lastReadPosition
                )

                ttsManager.initialize { spokenIndex ->
                    _uiState.value = _uiState.value.copy(currentWordIndex = spokenIndex)

                    // Lưu vào DB
                    currentDocumentId?.let { id ->
                        viewModelScope.launch {
                            updateReadPositionUseCase(id, spokenIndex)
                        }
                    }
                }

            } catch (e: Exception) {
                _uiState.value = ReaderUiState(
                    isLoading = false,
                    error = "Lỗi khi tải tài liệu"
                )
            }
        }
    }

    fun togglePlayPause() {
        val play = !_uiState.value.isPlaying
        _uiState.value = _uiState.value.copy(isPlaying = play)

        if (play) startReadingTts()
        else ttsManager.stop()
    }

    // Hàm này chỉ giả lập việc đọc, trong thực tế sẽ dùng TTS Engine
    private fun startReadingSimulation(documentId: String) {
        viewModelScope.launch {
            val words = _uiState.value.documentContent.split(" ")
            while (_uiState.value.isPlaying && _uiState.value.currentWordIndex < words.size - 1) {
                val newIndex = _uiState.value.currentWordIndex + 1
                _uiState.value = _uiState.value.copy(currentWordIndex = newIndex)
                updateReadPositionUseCase(documentId, newIndex)
                delay(300) // Giả lập tốc độ đọc
            }
            // Tự động dừng khi đọc xong
            if (_uiState.value.currentWordIndex >= words.size - 1) {
                _uiState.value = _uiState.value.copy(isPlaying = false)
            }
        }
    }

    private fun startReadingTts() {
        val words = _uiState.value.documentContent.split(" ")

        ttsManager.stop()
        ttsManager.speak(words, _uiState.value.currentWordIndex)
    }

    fun rewind() {
        val newIndex = (_uiState.value.currentWordIndex - 15).coerceAtLeast(0)
        applyJump(newIndex)
    }

    fun forward() {
        val words = _uiState.value.documentContent.split(" ")
        val newIndex = (_uiState.value.currentWordIndex + 15).coerceAtMost(words.size - 1)
        applyJump(newIndex)
    }

    fun jumpTo(index: Int) = applyJump(index)

    private fun applyJump(index: Int) {
        val words = _uiState.value.documentContent.split(" ")

        val bounded = index.coerceIn(0, words.size - 1)

        _uiState.value = _uiState.value.copy(currentWordIndex = bounded)

        // Lưu progress
        currentDocumentId?.let { id ->
            viewModelScope.launch {
                updateReadPositionUseCase(id, bounded)
            }
        }

        if (_uiState.value.isPlaying) startReadingTts()
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()   // ⭐ Dọn TextToSpeech khi ViewModel bị hủy
    }

    fun askAi(question: String) {
        viewModelScope.launch {
            // 1. Thêm câu hỏi user vào UI ngay lập tức
            val currentMessages = _uiState.value.chatMessages.toMutableList()
            currentMessages.add(ChatMessage(question, true))

            _uiState.value = _uiState.value.copy(
                chatMessages = currentMessages,
                isChatLoading = true
            )

            // 2. Gọi API hỏi AI
            askRagUseCase(question)
                .onSuccess { answer ->
                    // Thêm câu trả lời bot vào UI
                    val updatedMessages = _uiState.value.chatMessages.toMutableList()
                    updatedMessages.add(ChatMessage(answer, false))

                    _uiState.value = _uiState.value.copy(
                        chatMessages = updatedMessages,
                        isChatLoading = false
                    )
                }
                .onFailure {
                    // Xử lý lỗi (ví dụ bot trả lời câu mặc định)
                    val updatedMessages = _uiState.value.chatMessages.toMutableList()
                    updatedMessages.add(ChatMessage("Xin lỗi, server đang bận.", false))

                    _uiState.value = _uiState.value.copy(
                        chatMessages = updatedMessages,
                        isChatLoading = false
                    )
                }
        }
    }
}
