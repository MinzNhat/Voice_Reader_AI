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
 * @property useMainVoiceForAll Use the main voice for all documents
 * @property mainVoiceId Main voice ID to use for all documents
 * @property useMainSpeedForAll Use the main speed for all documents
 * @property mainSpeed Main speed to use for all documents
 * @property liveScanBarStyle Live scan bar UI style (EDGE_BAR, CIRCLE_BUTTON)
 */
data class VoiceSettings(
    val voiceId: String = "matt",  // Default to Matt (English male voice)
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val language: String = "en-US",  // Default to English
    val theme: ThemeMode = ThemeMode.SYSTEM,  // Default to system theme
    val useMainVoiceForAll: Boolean = false,  // Default: don't use main voice for all
    val mainVoiceId: String = "matt",  // Default main voice
    val useMainSpeedForAll: Boolean = false,  // Default: don't use main speed for all
    val mainSpeed: Float = 1.0f,  // Default main speed
    val liveScanBarStyle: LiveScanBarStyle = LiveScanBarStyle.CIRCLE_BUTTON  // Default to circle button
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
                theme = ThemeMode.SYSTEM,
                useMainVoiceForAll = false,
                mainVoiceId = "matt",
                useMainSpeedForAll = false,
                mainSpeed = 1.0f,
                liveScanBarStyle = LiveScanBarStyle.CIRCLE_BUTTON
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
                theme = ThemeMode.SYSTEM,
                useMainVoiceForAll = false,
                mainVoiceId = "nminseo",
                useMainSpeedForAll = false,
                mainSpeed = 1.0f,
                liveScanBarStyle = LiveScanBarStyle.CIRCLE_BUTTON
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

/**
 * Enum representing live scan bar UI styles.
 */
enum class LiveScanBarStyle {
    EDGE_BAR,       // Edge bar style (current default)
    CIRCLE_BUTTON   // Floating circle button style (like iOS AssistiveTouch)
}
