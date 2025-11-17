package com.example.voicereaderapp.ui.index

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereaderapp.domain.model.ReadingDocument
import com.example.voicereaderapp.domain.usecase.GetAllDocumentsUseCase
import com.example.voicereaderapp.domain.usecase.SaveDocumentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Index screen.
 * Manages navigation state between different tabs.
 */
@HiltViewModel
class IndexViewModel @Inject constructor(
    private val getAllDocumentsUseCase: GetAllDocumentsUseCase,
    private val saveDocumentUseCase: SaveDocumentUseCase
) : ViewModel() {
    private val _selectedTab = MutableStateFlow(0)

    /**
     * Current selected tab index.
     * 0 = PDF Reader, 1 = Scanner, 2 = Live Reader, 3 = Settings
     */
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()
    private val _documents = MutableStateFlow<List<ReadingDocument>>(emptyList())
    val documents: StateFlow<List<ReadingDocument>> = _documents.asStateFlow()

    init {
        observeDocuments()
    }

    private fun observeDocuments() {
        viewModelScope.launch {
            getAllDocumentsUseCase().collect { docs ->
                _documents.value = docs
            }
        }
    }
    /**
     * Selects a tab by index.
     *
     * @param index Tab index to select
     */
    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun saveImportedDocument(document: ReadingDocument) {
        viewModelScope.launch {
            saveDocumentUseCase(document)
        }
    }
}
