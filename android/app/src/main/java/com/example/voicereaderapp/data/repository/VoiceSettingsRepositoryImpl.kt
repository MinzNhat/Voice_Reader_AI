package com.example.voicereaderapp.data.repository

import com.example.voicereaderapp.data.local.preferences.VoiceSettingsPreferences
import com.example.voicereaderapp.domain.model.VoiceSettings
import com.example.voicereaderapp.domain.repository.VoiceSettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of VoiceSettingsRepository.
 * Manages voice settings persistence using DataStore preferences.
 *
 * @property preferences DataStore preferences manager
 */
@Singleton
class VoiceSettingsRepositoryImpl @Inject constructor(
    private val preferences: VoiceSettingsPreferences
) : VoiceSettingsRepository {
    /**
     * Retrieves current voice settings as a Flow.
     *
     * @return Flow emitting VoiceSettings
     */
    override fun getVoiceSettings(): Flow<VoiceSettings> {
        return preferences.getVoiceSettings()
    }

    /**
     * Saves new voice settings.
     *
     * @param settings VoiceSettings to be saved
     */
    override suspend fun saveVoiceSettings(settings: VoiceSettings) {
        preferences.saveVoiceSettings(settings)
    }

    /**
     * Updates only the reading speed.
     *
     * @param speed New speed value
     */
    override suspend fun updateSpeed(speed: Float) {
        preferences.updateSpeed(speed)
    }

    /**
     * Updates only the voice pitch.
     *
     * @param pitch New pitch value
     */
    override suspend fun updatePitch(pitch: Float) {
        preferences.updatePitch(pitch)
    }
}
