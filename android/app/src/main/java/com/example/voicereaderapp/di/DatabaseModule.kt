package com.example.voicereaderapp.di

import android.content.Context
import androidx.room.Room
import com.example.voicereaderapp.data.local.dao.DocumentDao
import com.example.voicereaderapp.data.local.database.MIGRATION_1_2
import com.example.voicereaderapp.data.local.database.MIGRATION_2_3
import com.example.voicereaderapp.data.local.database.VoiceReaderDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing database-related dependencies.
 * Configures Room database and DAOs for dependency injection.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    /**
     * Provides singleton instance of VoiceReaderDatabase.
     * Configures Room database with app context.
     *
     * @param context Application context
     * @return VoiceReaderDatabase instance
     */
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): VoiceReaderDatabase {
        return Room.databaseBuilder(
            context,
            VoiceReaderDatabase::class.java,
            VoiceReaderDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }

    /**
     * Provides DocumentDao from database.
     *
     * @param database VoiceReaderDatabase instance
     * @return DocumentDao instance
     */
    @Provides
    @Singleton
    fun provideDocumentDao(database: VoiceReaderDatabase): DocumentDao {
        return database.documentDao()
    }
}
