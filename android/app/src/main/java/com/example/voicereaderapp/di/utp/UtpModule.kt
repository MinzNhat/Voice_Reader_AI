package com.example.voicereaderapp.di.utp

import android.content.Context
import android.speech.tts.TextToSpeech
import com.example.voicereaderapp.data.repository.utp.ContentStorageRepositoryImpl
import com.example.voicereaderapp.data.local.dao.utp.SavedContentDao
import com.example.voicereaderapp.data.repository.utp.TextDetectorRepositoryImpl
import com.example.voicereaderapp.data.repository.utp.backend.NoopBackendOcrClient
import com.example.voicereaderapp.data.repository.utp.backend.BackendOcrClient
import com.example.voicereaderapp.data.repository.utp.TextNormalizerRepositoryImpl
import com.example.voicereaderapp.data.repository.utp.TtsHighlightRepositoryImpl
import com.example.voicereaderapp.domain.repository.utp.ContentStorageRepository
import com.example.voicereaderapp.domain.repository.utp.TextDetectorRepository
import com.example.voicereaderapp.domain.repository.utp.TextNormalizerRepository
import com.example.voicereaderapp.domain.repository.utp.TtsHighlightRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.Locale
import javax.inject.Singleton

/**
 * Hilt module for Universal Text Pipeline
 */
@Module
@InstallIn(SingletonComponent::class)
object UtpModule {
    
    @Provides
    @Singleton
    fun provideTextToSpeech(
        @ApplicationContext context: Context
    ): TextToSpeech {
        return TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Set Vietnamese language
                // tts.language = Locale("vi", "VN")
            }
        }
    }
    
    @Provides
    @Singleton
    fun provideTextDetectorRepository(
        @ApplicationContext context: Context,
        backendOcrClient: BackendOcrClient
    ): TextDetectorRepository {
        return TextDetectorRepositoryImpl(context, backendOcrClient)
    }
    
    @Provides
    @Singleton
    fun provideTextNormalizerRepository(): TextNormalizerRepository {
        return TextNormalizerRepositoryImpl()
    }
    
    @Provides
    @Singleton
    fun provideTtsHighlightRepository(
        tts: TextToSpeech
    ): TtsHighlightRepository {
        return TtsHighlightRepositoryImpl(tts)
    }
    
    @Provides
    @Singleton
    fun provideContentStorageRepository(
        savedContentDao: SavedContentDao
    ): ContentStorageRepository {
        // Use the DAO-backed implementation as the canonical storage provider
        return ContentStorageRepositoryImpl(savedContentDao)
    }

    @Provides
    @Singleton
    fun provideBackendOcrClient(): BackendOcrClient {
        // Default no-op; replace with a real Retrofit/OkHttp client implementation
        return NoopBackendOcrClient()
    }
}
