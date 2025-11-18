package com.example.voicereaderapp.domain.model

/**
 * Domain model representing voice reading settings.
 * Contains configuration for text-to-speech functionality.
 *
 * @property voiceId Unique identifier for the selected voice (e.g., "matt", "minseo")
 * @property speed Reading speed (0.5 to 2.0, where 1.0 is normal speed)
 * @property pitch Voice pitch level (0.5 to 2.0, where 1.0 is normal pitch)
 * @property language Language code (e.g., "ko-KR", "en-US")
 * @property theme Theme mode (LIGHT, DARK, SYSTEM)
 */
data class VoiceSettings(
    val voiceId: String = "matt",  // Default to Matt (English male voice)
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val language: String = "en-US",  // Default to English
    val theme: ThemeMode = ThemeMode.SYSTEM  // Default to system theme
) {
    companion object {
        /**
         * Returns default voice settings.
         */
        fun default(): VoiceSettings {
            return VoiceSettings(
                voiceId = "matt",
                speed = 1.0f,
                pitch = 1.0f,
                language = "en-US",
                theme = ThemeMode.SYSTEM
            )
        }

        /**
         * Returns voice settings for Korean with default voice (Minseo).
         */
        fun korean(): VoiceSettings {
            return VoiceSettings(
                voiceId = "nminseo",
                speed = 1.0f,
                pitch = 1.0f,
                language = "ko-KR",
                theme = ThemeMode.SYSTEM
            )
        }
    }
}

/**
 * Enum representing different theme modes.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}
