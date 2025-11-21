package com.example.voicereaderapp.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.voicereaderapp.data.local.dao.NoteDao
import com.example.voicereaderapp.data.local.entity.NoteEntity
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
    entities = [DocumentEntity::class, NoteEntity::class],
    version = 4,
    exportSchema = false
)
abstract class VoiceReaderDatabase : RoomDatabase() {
    /**
     * Returns the DAO for document operations.
     *
     * @return DocumentDao instance
     */
    abstract fun documentDao(): DocumentDao

    abstract fun noteDao(): NoteDao

    companion object {
        // @Volatile đảm bảo giá trị của INSTANCE luôn được cập nhật và
        // hiển thị cho tất cả các luồng thực thi.
        @Volatile
        private var INSTANCE: VoiceReaderDatabase? = null

        const val DATABASE_NAME = "voice_reader_db"

        fun getDatabase(context: Context): VoiceReaderDatabase {
            // Trả về INSTANCE nếu nó đã tồn tại.
            // Nếu chưa, khởi tạo nó trong một khối synchronized để đảm bảo an toàn luồng.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VoiceReaderDatabase::class.java,
                    DATABASE_NAME
                )
                    // For development: just drop and recreate on any schema changes
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Tạo bảng mới cho NoteEntity
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notes_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `lastModified` INTEGER NOT NULL)"
                )

                // Thêm cột audioCacheJson vào bảng documents
                db.execSQL("ALTER TABLE documents ADD COLUMN audioCacheJson TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 3 - không có thay đổi schema, chỉ để force refresh
                // Tất cả schema đã đúng ở version 2
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to notes_table for document linking
                db.execSQL("ALTER TABLE notes_table ADD COLUMN documentId TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE notes_table ADD COLUMN documentTitle TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE notes_table ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                // Update createdAt with lastModified values for existing notes
                db.execSQL("UPDATE notes_table SET createdAt = lastModified WHERE createdAt = 0")
            }
        }
    }
}
