package com.example.voicereaderapp.ui.livereader.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.voicereaderapp.data.local.entity.NoteEntity
import com.example.voicereaderapp.data.local.entity.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import okhttp3.Dispatcher


class NoteViewModel(private val noteRepository: NoteRepository) : ViewModel() {

    val notes = noteRepository.getAllNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun getNoteById(noteId: Long) = noteRepository.getNoteById(noteId)

    fun saveNote(id: Long?, title: String, content: String) {
        viewModelScope.launch {
            val noteToSave = NoteEntity(
                id = id ?: 0, // Nếu id null thì Room sẽ tự tạo mới
                title = if (title.isBlank()) "Untitled Note" else title,
                content = content,
                lastModified = System.currentTimeMillis()
            )
            // Room sẽ tự biết là insert hay update dựa vào id
            noteRepository.insertNote(noteToSave)
        }
    }

    fun deleteNote(noteId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            noteRepository.deleteNoteById(noteId)
        }
    }
}
