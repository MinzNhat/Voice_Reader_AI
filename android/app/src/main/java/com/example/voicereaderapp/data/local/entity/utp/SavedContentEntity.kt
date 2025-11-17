package com.example.voicereaderapp.data.local.entity.utp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_contents")
data class SavedContentEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val rawText: String,
    val tags: String, // Comma-separated
    val savedAt: Long,
    val readCount: Int = 0,
    val lastReadAt: Long? = null
)
