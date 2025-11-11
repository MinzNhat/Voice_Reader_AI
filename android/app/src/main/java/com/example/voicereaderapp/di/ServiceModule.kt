package com.example.voicereaderapp.di

import com.example.voicereaderapp.data.service.TextToSpeechServiceImpl
import com.example.voicereaderapp.domain.service.TextToSpeechService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing service dependencies.
 * Binds service interfaces to their implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    /**
     * Binds TextToSpeechService interface to its implementation.
     * Provides singleton instance for TTS functionality.
     *
     * @param serviceImpl Concrete implementation of TextToSpeechService
     * @return TextToSpeechService interface
     */
    @Binds
    @Singleton
    abstract fun bindTextToSpeechService(
        serviceImpl: TextToSpeechServiceImpl
    ): TextToSpeechService
}
