package com.example.voicereaderapp.di

import com.example.voicereaderapp.data.repository.DocumentRepositoryImpl
import com.example.voicereaderapp.data.repository.VoiceSettingsRepositoryImpl
import com.example.voicereaderapp.domain.repository.DocumentRepository
import com.example.voicereaderapp.domain.repository.VoiceSettingsRepository
import com.example.voicereaderapp.data.repository.OCRRepositoryImpl
import com.example.voicereaderapp.data.repository.TTSRepositoryImpl
import com.example.voicereaderapp.domain.repository.OCRRepository
import com.example.voicereaderapp.domain.repository.TTSRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing repository implementations.
 * Binds repository interfaces to their concrete implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    /**
     * Binds DocumentRepository interface to its implementation.
     * Provides singleton instance for document data management.
     *
     * @param repositoryImpl Concrete implementation of DocumentRepository
     * @return DocumentRepository interface
     */
    @Binds
    @Singleton
    abstract fun bindDocumentRepository(
        repositoryImpl: DocumentRepositoryImpl
    ): DocumentRepository

    /**
     * Binds VoiceSettingsRepository interface to its implementation.
     * Provides singleton instance for voice settings management.
     *
     * @param repositoryImpl Concrete implementation of VoiceSettingsRepository
     * @return VoiceSettingsRepository interface
     */
    @Binds
    @Singleton
    abstract fun bindVoiceSettingsRepository(
        repositoryImpl: VoiceSettingsRepositoryImpl
    ): VoiceSettingsRepository

    @Binds
    @Singleton
    abstract fun bindOCRRepository(
        repositoryImpl: OCRRepositoryImpl
    ): OCRRepository

    @Binds
    @Singleton
    abstract fun bindTTSRepository(
        repositoryImpl: TTSRepositoryImpl
    ): TTSRepository
}
