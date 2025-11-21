package com.example.voicereaderapp.ui.livereader.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereaderapp.data.local.entity.NoteEntity
import com.example.voicereaderapp.data.local.entity.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject


@HiltViewModel
class NoteViewModel @Inject constructor(private val noteRepository: NoteRepository) : ViewModel() {

    val notes = noteRepository.getAllNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun getNotesByDocumentId(documentId: String) = noteRepository.getNotesByDocumentId(documentId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun getNoteById(noteId: Long) = noteRepository.getNoteById(noteId)

    fun saveNote(
        id: Long?,
        title: String,
        content: String,
        documentId: String? = null,
        documentTitle: String? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (id == null || id == 0L) {
                // Creating new note
                val noteToSave = NoteEntity(
                    id = 0,
                    title = if (title.isBlank()) (documentTitle ?: "Untitled Note") else title,
                    content = content,
                    documentId = documentId,
                    documentTitle = documentTitle,
                    createdAt = System.currentTimeMillis(),
                    lastModified = System.currentTimeMillis()
                )
                noteRepository.insertNote(noteToSave)
            } else {
                // Updating existing note - fetch original to preserve createdAt
                noteRepository.getNoteById(id).collect { existingNote ->
                    existingNote?.let {
                        val updatedNote = it.copy(
                            title = if (title.isBlank()) (documentTitle ?: "Untitled Note") else title,
                            content = content,
                            documentId = documentId,
                            documentTitle = documentTitle,
                            lastModified = System.currentTimeMillis()
                            // createdAt is preserved from existingNote
                        )
                        noteRepository.updateNote(updatedNote)
                    }
                }
            }
        }
    }

    fun renameNote(noteId: Long, newTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val note = noteRepository.getNoteById(noteId)
            note.collect { existingNote ->
                existingNote?.let {
                    val updatedNote = it.copy(
                        title = newTitle,
                        lastModified = System.currentTimeMillis()
                    )
                    noteRepository.updateNote(updatedNote)
                }
            }
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            noteRepository.deleteNoteById(noteId)
        }
    }
}
