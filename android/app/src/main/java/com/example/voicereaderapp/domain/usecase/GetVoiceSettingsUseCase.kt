package com.example.voicereaderapp.domain.usecase

import com.example.voicereaderapp.domain.model.VoiceSettings
import com.example.voicereaderapp.domain.repository.VoiceSettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving voice settings.
 * Provides access to current voice configuration.
 *
 * @property voiceSettingsRepository Repository managing voice settings
 */
class GetVoiceSettingsUseCase @Inject constructor(
    private val voiceSettingsRepository: VoiceSettingsRepository
) {
    /**
     * Executes the use case to get voice settings.
     *
     * @return Flow emitting current VoiceSettings
     */
    operator fun invoke(): Flow<VoiceSettings> {
        return voiceSettingsRepository.getVoiceSettings()
    }
}
