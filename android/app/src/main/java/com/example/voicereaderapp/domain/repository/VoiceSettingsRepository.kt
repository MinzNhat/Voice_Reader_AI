package com.example.voicereaderapp.domain.repository

import com.example.voicereaderapp.domain.model.VoiceSettings
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing voice settings.
 * Handles persistence and retrieval of user's voice preferences.
 */
interface VoiceSettingsRepository {
    /**
     * Retrieves current voice settings as a Flow.
     * Emits updates whenever settings change.
     *
     * @return Flow emitting current VoiceSettings
     */
    fun getVoiceSettings(): Flow<VoiceSettings>

    /**
     * Saves new voice settings.
     *
     * @param settings The VoiceSettings to be saved
     */
    suspend fun saveVoiceSettings(settings: VoiceSettings)

    /**
     * Updates the reading speed only.
     *
     * @param speed New speed value (0.5 to 2.0)
     */
    suspend fun updateSpeed(speed: Float)

    /**
     * Updates the voice pitch only.
     *
     * @param pitch New pitch value (0.5 to 2.0)
     */
    suspend fun updatePitch(pitch: Float)
}
