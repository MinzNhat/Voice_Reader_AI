package com.example.voicereaderapp.domain.model

/**
 * Domain model representing voice reading settings.
 * Contains configuration for text-to-speech functionality.
 *
 * @property voiceId Unique identifier for the selected voice
 * @property speed Reading speed (0.5 to 2.0, where 1.0 is normal speed)
 * @property pitch Voice pitch level (0.5 to 2.0, where 1.0 is normal pitch)
 * @property language Language code (e.g., "vi-VN", "en-US")
 */
data class VoiceSettings(
    val voiceId: String = "",
    val speed: Float = 1.0f,
    val pitch: Float = 1.0f,
    val language: String = "vi-VN"
)
