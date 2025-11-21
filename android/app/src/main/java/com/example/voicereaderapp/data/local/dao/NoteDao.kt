package com.example.voicereaderapp.data.local.dao

import androidx.room.*
import com.example.voicereaderapp.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes_table ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes_table WHERE documentId = :documentId AND documentId IS NOT NULL AND documentId != '' ORDER BY createdAt DESC")
    fun getNotesByDocumentId(documentId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes_table WHERE id = :noteId")
    fun getNoteById(noteId: Long): Flow<NoteEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM notes_table WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Long)

    @Query("DELETE FROM notes_table WHERE documentId = :documentId")
    suspend fun deleteNotesByDocumentId(documentId: String)
}
