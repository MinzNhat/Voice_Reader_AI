package com.example.voicereaderapp.domain.repository

import com.example.voicereaderapp.data.remote.model.TimingResponse
import com.example.voicereaderapp.utils.Result

/**
 * Repository interface for Text-to-Speech operations
 */
interface TTSRepository {

    /**
     * Generate speech from text
     * @param text Text to convert to speech
     * @param speaker Speaker voice name (default: "matt" - male English)
     * @param language Language code (default: "en-US")
     * @return Result containing base64 encoded MP3 audio
     */
    suspend fun generateSpeech(
        text: String,
        speaker: String = "matt",
        language: String = "en-US"
    ): Result<String>

    /**
     * Get word-level timing for text
     * Used for real-time word highlighting during playback
     * @param text Text to calculate timing for
     * @return Result containing timing information for each word
     */
    suspend fun getWordTimings(text: String): Result<TimingResponse>

    /**
     * Play audio from base64 encoded MP3
     * @param base64Audio Base64 encoded audio data
     * @param playbackSpeed Playback speed (0.5x to 2.0x)
     * @param onProgress Callback for playback progress (milliseconds)
     * @param onComplete Callback when playback completes
     */
    suspend fun playAudio(
        base64Audio: String,
        playbackSpeed: Float = 1.0f,
        onProgress: (Long) -> Unit = {},
        onComplete: () -> Unit = {}
    )

    /**
     * Set playback speed for current audio
     * @param speed Speed multiplier (0.5x to 2.0x)
     */
    fun setPlaybackSpeed(speed: Float)

    /**
     * Stop current audio playback
     */
    fun stopAudio()

    /**
     * Pause current audio playback
     */
    fun pauseAudio()

    /**
     * Resume paused audio playback
     */
    fun resumeAudio()

    /**
     * Check if audio is currently playing
     */
    fun isPlaying(): Boolean

    /**
     * Seek to a specific position in the audio
     * @param positionMs Position in milliseconds
     */
    fun seekTo(positionMs: Long)

    /**
     * Get current playback position
     * @return Current position in milliseconds
     */
    fun getCurrentPosition(): Long

    /**
     * Get total audio duration
     * @return Total duration in milliseconds
     */
    fun getDuration(): Long
}
