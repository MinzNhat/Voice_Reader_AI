package com.example.voicereaderapp.di

import android.content.Context
import com.example.voicereaderapp.data.local.preferences.VoiceSettingsPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing DataStore preferences dependencies.
 * Configures DataStore for managing app preferences.
 */
@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {
    /**
     * Provides singleton instance of VoiceSettingsPreferences.
     * Manages voice settings persistence using DataStore.
     *
     * @param context Application context
     * @return VoiceSettingsPreferences instance
     */
    @Provides
    @Singleton
    fun provideVoiceSettingsPreferences(
        @ApplicationContext context: Context
    ): VoiceSettingsPreferences {
        return VoiceSettingsPreferences(context)
    }
}
