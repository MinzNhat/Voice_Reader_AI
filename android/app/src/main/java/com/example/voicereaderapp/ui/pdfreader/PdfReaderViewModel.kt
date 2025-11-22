package com.example.voicereaderapp.ui.pdfreader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereaderapp.domain.model.ReadingDocument
import com.example.voicereaderapp.domain.usecase.GetAllDocumentsUseCase
import com.example.voicereaderapp.domain.model.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * UI state for PDF Reader screen.
 *
 * @property isLoading Loading indicator state
 * @property documents List of available documents
 * @property selectedDocumentId Currently selected document ID
 * @property error Error message if any
 */
data class PdfReaderUiState(
    val isLoading: Boolean = false,
    val documents: List<ReadingDocument> = emptyList(),
    val selectedDocumentId: String? = null,
    val error: String? = null
)

/**
 * ViewModel for PDF Reader screen.
 * Manages document list and reading state.
 *
 * @property getAllDocumentsUseCase Use case for fetching documents
 */
@HiltViewModel
class PdfReaderViewModel @Inject constructor(
    private val getAllDocumentsUseCase: GetAllDocumentsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(PdfReaderUiState(isLoading = true))

    /**
     * Current UI state of the PDF reader screen.
     */
    val uiState: StateFlow<PdfReaderUiState> = _uiState.asStateFlow()

    init {
        loadDocuments()
    }

    /**
     * Loads all documents from repository.
     */
    private fun loadDocuments() {
        viewModelScope.launch {
            getAllDocumentsUseCase().collect { documents ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    documents = documents
                )
            }
        }
    }

    /**
     * Opens a document for reading.
     *
     * @param documentId ID of the document to open
     */
    fun openDocument(documentId: String) {
        _uiState.value = _uiState.value.copy(selectedDocumentId = documentId)
        // TODO: Navigate to document reading screen
    }
}
