package com.example.voicereaderapp.di

import com.example.voicereaderapp.domain.repository.DocumentRepository
import com.example.voicereaderapp.domain.repository.VoiceSettingsRepository
import com.example.voicereaderapp.domain.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * Hilt module providing use case dependencies.
 * Use cases are scoped to ViewModel lifecycle.
 */
@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {
    /**
     * Provides GetAllDocumentsUseCase.
     * Retrieves all documents from repository.
     *
     * @param repository DocumentRepository instance
     * @return GetAllDocumentsUseCase instance
     */
    @Provides
    @ViewModelScoped
    fun provideGetAllDocumentsUseCase(
        repository: DocumentRepository
    ): GetAllDocumentsUseCase {
        return GetAllDocumentsUseCase(repository)
    }

    /**
     * Provides SaveDocumentUseCase.
     * Saves documents to repository.
     *
     * @param repository DocumentRepository instance
     * @return SaveDocumentUseCase instance
     */
    @Provides
    @ViewModelScoped
    fun provideSaveDocumentUseCase(
        repository: DocumentRepository
    ): SaveDocumentUseCase {
        return SaveDocumentUseCase(repository)
    }

    /**
     * Provides GetVoiceSettingsUseCase.
     * Retrieves voice settings from repository.
     *
     * @param repository VoiceSettingsRepository instance
     * @return GetVoiceSettingsUseCase instance
     */
    @Provides
    @ViewModelScoped
    fun provideGetVoiceSettingsUseCase(
        repository: VoiceSettingsRepository
    ): GetVoiceSettingsUseCase {
        return GetVoiceSettingsUseCase(repository)
    }

    /**
     * Provides UpdateVoiceSettingsUseCase.
     * Updates voice settings in repository.
     *
     * @param repository VoiceSettingsRepository instance
     * @return UpdateVoiceSettingsUseCase instance
     */
    @Provides
    @ViewModelScoped
    fun provideUpdateVoiceSettingsUseCase(
        repository: VoiceSettingsRepository
    ): UpdateVoiceSettingsUseCase {
        return UpdateVoiceSettingsUseCase(repository)
    }
<<<<<<< HEAD

    @Provides
    fun provideUpdateReadPositionUseCase(
        repository: DocumentRepository
    ): UpdateReadPositionUseCase {
        return UpdateReadPositionUseCase(repository)
    }

    @Provides
    fun provideGetDocumentByIdUseCase(
        repo: DocumentRepository
    ): GetDocumentByIdUseCase {
        return GetDocumentByIdUseCase(repo)
    }
=======
>>>>>>> origin/cd
}
