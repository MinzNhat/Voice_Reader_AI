package com.example.voicereaderapp.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migration from version 1 to 2.
 * Adds voice configuration columns to the documents table.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add voiceId column (nullable)
        database.execSQL(
            "ALTER TABLE documents ADD COLUMN voiceId TEXT DEFAULT NULL"
        )

        // Add language column (nullable)
        database.execSQL(
            "ALTER TABLE documents ADD COLUMN language TEXT DEFAULT NULL"
        )

        // Add speed column (nullable)
        database.execSQL(
            "ALTER TABLE documents ADD COLUMN speed REAL DEFAULT NULL"
        )
    }
}

/**
 * Database migration from version 2 to 3.
 * Adds audio caching column to store multiple audio versions (one per voice+language).
 * This prevents repeated expensive TTS API calls while supporting multiple voices.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add audioCacheJson column for cached TTS audio (multiple versions per voice)
        // Format: { "matt_en-US": { "audio": "base64...", "timings": [...] }, "anna_en-US": {...} }
        database.execSQL(
            "ALTER TABLE documents ADD COLUMN audioCacheJson TEXT DEFAULT NULL"
        )
    }
}
