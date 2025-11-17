package com.example.voicereaderapp.utils

import com.example.voicereaderapp.domain.model.VoiceSettings

/**
 * Mapper utility for voice settings.
 * Provides mapping between different voice configurations and language codes.
 */
object VoiceMapper {
    /**
     * Map of language codes to display names.
     */
    val languageDisplayNames = mapOf(
        "vi-VN" to "Tiếng Việt",
        "en-US" to "English (US)",
        "en-GB" to "English (UK)",
        "ja-JP" to "Japanese",
        "ko-KR" to "Korean",
        "zh-CN" to "Chinese (Simplified)",
        "zh-TW" to "Chinese (Traditional)"
    )

    /**
     * Map of voice IDs to their display names for Vietnamese.
     */
    val vietnameseVoices = mapOf(
        "vi-vn-x-vie-local" to "Nam Miền Bắc",
        "vi-vn-x-vif-local" to "Nữ Miền Bắc",
        "vi-vn-x-vid-local" to "Nam Miền Nam",
        "vi-vn-x-vig-local" to "Nữ Miền Nam"
    )

    /**
     * Gets display name for a language code.
     *
     * @param languageCode Language code (e.g., "vi-VN")
     * @return Display name of the language
     */
    fun getLanguageDisplayName(languageCode: String): String {
        return languageDisplayNames[languageCode] ?: languageCode
    }

    /**
     * Gets voice display name for a voice ID.
     *
     * @param voiceId Voice identifier
     * @return Display name of the voice
     */
    fun getVoiceDisplayName(voiceId: String): String {
        return vietnameseVoices[voiceId] ?: voiceId
    }

    /**
     * Validates voice settings.
     *
     * @param settings VoiceSettings to validate
     * @return true if valid, false otherwise
     */
    fun validateSettings(settings: VoiceSettings): Boolean {
        return settings.speed in Constants.MIN_SPEED..Constants.MAX_SPEED &&
               settings.pitch in Constants.MIN_PITCH..Constants.MAX_PITCH &&
               settings.language.isNotBlank()
    }

    /**
     * Creates default voice settings.
     *
     * @return Default VoiceSettings instance
     */
    fun createDefaultSettings(): VoiceSettings {
        return VoiceSettings(
            voiceId = "",
            speed = Constants.DEFAULT_SPEED,
            pitch = Constants.DEFAULT_PITCH,
            language = Constants.DEFAULT_LANGUAGE
        )
    }
}
