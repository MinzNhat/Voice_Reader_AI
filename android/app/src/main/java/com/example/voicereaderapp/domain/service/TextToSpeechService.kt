package com.example.voicereaderapp.domain.service

import kotlinx.coroutines.flow.Flow

/**
 * Interface for Text-to-Speech service.
 * Defines operations for reading text aloud with voice synthesis.
 * Designed for accessibility support for visually impaired users.
 */
interface TextToSpeechService {
    /**
     * Initializes the TTS engine.
     *
     * @return Flow emitting initialization status
     */
    fun initialize(): Flow<TtsInitStatus>

    /**
     * Speaks the given text using current voice settings.
     *
     * @param text Text to be spoken
     * @param queueMode Queue mode (FLUSH or ADD)
     * @param priority Speech priority for accessibility
     */
    suspend fun speak(
        text: String,
        queueMode: QueueMode = QueueMode.FLUSH,
        priority: SpeechPriority = SpeechPriority.NORMAL
    )

    /**
     * Stops current speech immediately.
     */
    fun stop()

    /**
     * Pauses speech if supported by engine.
     */
    fun pause()

    /**
     * Resumes paused speech.
     */
    fun resume()

    /**
     * Checks if TTS is currently speaking.
     *
     * @return true if speaking, false otherwise
     */
    fun isSpeaking(): Boolean

    /**
     * Sets speech rate (speed).
     *
     * @param rate Speech rate (0.5 to 2.0, where 1.0 is normal)
     */
    fun setRate(rate: Float)

    /**
     * Sets speech pitch.
     *
     * @param pitch Speech pitch (0.5 to 2.0, where 1.0 is normal)
     */
    fun setPitch(pitch: Float)

    /**
     * Sets the language for speech.
     *
     * @param languageCode Language code (e.g., "vi-VN", "en-US")
     * @return true if language is available, false otherwise
     */
    suspend fun setLanguage(languageCode: String): Boolean

    /**
     * Gets available voices for the current language.
     *
     * @return List of available voice IDs
     */
    fun getAvailableVoices(): List<String>

    /**
     * Releases TTS resources.
     */
    fun shutdown()
}

/**
 * TTS initialization status.
 */
enum class TtsInitStatus {
    SUCCESS,
    ERROR,
    LOADING
}

/**
 * Queue mode for speech.
 */
enum class QueueMode {
    FLUSH,  // Interrupts current speech
    ADD     // Adds to queue
}

/**
 * Speech priority for accessibility.
 */
enum class SpeechPriority {
    HIGH,      // Urgent notifications
    NORMAL,    // Regular content
    LOW        // Background info
}
