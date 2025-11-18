package com.example.voicereaderapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes_table") // Đặt tên cho bảng trong database
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    val content: String,

    val lastModified: Long = System.currentTimeMillis()
)
