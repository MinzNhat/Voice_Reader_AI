package com.example.voicereaderapp.domain.usecase

import com.example.voicereaderapp.domain.model.VoiceSettings
import com.example.voicereaderapp.domain.repository.VoiceSettingsRepository
import javax.inject.Inject

/**
 * Use case for updating voice settings.
 * Validates settings before saving to ensure they are within acceptable ranges.
 *
 * @property voiceSettingsRepository Repository managing voice settings
 */
class UpdateVoiceSettingsUseCase @Inject constructor(
    private val voiceSettingsRepository: VoiceSettingsRepository
) {
    /**
     * Executes the use case to update voice settings.
     * Validates speed and pitch values before saving.
     *
     * @param settings New VoiceSettings to be saved
     * @throws IllegalArgumentException if validation fails
     */
    suspend operator fun invoke(settings: VoiceSettings) {
        // Validate settings
        require(settings.speed in 0.5f..2.0f) { "Speed must be between 0.5 and 2.0" }
        require(settings.pitch in 0.5f..2.0f) { "Pitch must be between 0.5 and 2.0" }
        
        voiceSettingsRepository.saveVoiceSettings(settings)
    }
}
