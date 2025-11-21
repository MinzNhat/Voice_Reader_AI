package com.example.voicereaderapp.data.local.entity


import com.example.voicereaderapp.data.local.dao.NoteDao
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAllNotes()

    fun getNotesByDocumentId(documentId: String): Flow<List<NoteEntity>> =
        noteDao.getNotesByDocumentId(documentId)

    fun getNoteById(noteId: Long): Flow<NoteEntity?> = noteDao.getNoteById(noteId)

    suspend fun insertNote(note: NoteEntity) {
        noteDao.insertNote(note)
    }

    suspend fun updateNote(note: NoteEntity) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNoteById(noteId: Long) {
        noteDao.deleteNoteById(noteId)
    }

    suspend fun deleteNotesByDocumentId(documentId: String) {
        noteDao.deleteNotesByDocumentId(documentId)
    }
}
