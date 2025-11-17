package com.example.voicereaderapp.ui.index

import androidx.lifecycle.ViewModel
<<<<<<< HEAD
import androidx.lifecycle.viewModelScope
import com.example.voicereaderapp.domain.model.ReadingDocument
import com.example.voicereaderapp.domain.usecase.GetAllDocumentsUseCase
import com.example.voicereaderapp.domain.usecase.SaveDocumentUseCase
=======
>>>>>>> origin/cd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
<<<<<<< HEAD
import kotlinx.coroutines.launch
=======
>>>>>>> origin/cd
import javax.inject.Inject

/**
 * ViewModel for the Index screen.
 * Manages navigation state between different tabs.
 */
@HiltViewModel
<<<<<<< HEAD
class IndexViewModel @Inject constructor(
    private val getAllDocumentsUseCase: GetAllDocumentsUseCase,
    private val saveDocumentUseCase: SaveDocumentUseCase
) : ViewModel() {
    private val _selectedTab = MutableStateFlow(0)

=======
class IndexViewModel @Inject constructor() : ViewModel() {
    private val _selectedTab = MutableStateFlow(0)
    
>>>>>>> origin/cd
    /**
     * Current selected tab index.
     * 0 = PDF Reader, 1 = Scanner, 2 = Live Reader, 3 = Settings
     */
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()
<<<<<<< HEAD
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
=======

>>>>>>> origin/cd
    /**
     * Selects a tab by index.
     *
     * @param index Tab index to select
     */
    fun selectTab(index: Int) {
        _selectedTab.value = index
    }
<<<<<<< HEAD

    fun saveImportedDocument(document: ReadingDocument) {
        viewModelScope.launch {
            saveDocumentUseCase(document)
        }
    }
=======
>>>>>>> origin/cd
}
