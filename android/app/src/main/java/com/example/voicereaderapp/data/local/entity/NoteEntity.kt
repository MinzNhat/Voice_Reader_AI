package com.example.voicereaderapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes_table")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    val content: String,

    val documentId: String? = null,  // Link to document (null for live scan notes)

    val documentTitle: String? = null,  // Store document name for display

    val createdAt: Long = System.currentTimeMillis(),

    val lastModified: Long = System.currentTimeMillis()
)
