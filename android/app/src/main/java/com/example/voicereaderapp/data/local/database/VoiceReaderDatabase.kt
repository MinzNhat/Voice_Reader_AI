package com.example.voicereaderapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.voicereaderapp.data.local.dao.DocumentDao
import com.example.voicereaderapp.data.local.entity.DocumentEntity

/**
 * Room database class for the Voice Reader app.
 * Defines the database configuration and serves as the main access point
 * for the underlying SQLite database.
 *
 * @property documentDao Provides access to document-related database operations
 */
@Database(
    entities = [DocumentEntity::class],
    version = 1,
    exportSchema = false
)
abstract class VoiceReaderDatabase : RoomDatabase() {
    /**
     * Returns the DAO for document operations.
     *
     * @return DocumentDao instance
     */
    abstract fun documentDao(): DocumentDao

    companion object {
        const val DATABASE_NAME = "voice_reader_db"
    }
}
