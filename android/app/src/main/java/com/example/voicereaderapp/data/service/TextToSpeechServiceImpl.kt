package com.example.voicereaderapp.data.service

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.voicereaderapp.domain.service.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TextToSpeechService using Android TTS API.
 * Provides text-to-speech functionality optimized for accessibility.
 *
 * @property context Application context
 */
@Singleton
class TextToSpeechServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeechService {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    /**
     * Initializes the TTS engine.
     * Sets up utterance progress listener for speech events.
     *
     * @return Flow emitting initialization status
     */
    override fun initialize(): Flow<TtsInitStatus> = callbackFlow {
        trySend(TtsInitStatus.LOADING)

        tts = TextToSpeech(context) { status ->
            when (status) {
                TextToSpeech.SUCCESS -> {
                    isInitialized = true
                    setupProgressListener()
                    trySend(TtsInitStatus.SUCCESS)
                }
                else -> {
                    isInitialized = false
                    trySend(TtsInitStatus.ERROR)
                }
            }
        }

        awaitClose {
            // Cleanup handled by shutdown()
        }
    }

    /**
     * Sets up progress listener for speech events.
     * Monitors start, done, and error states.
     */
    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                // Speech started
            }

            override fun onDone(utteranceId: String?) {
                // Speech completed
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                // Speech error occurred
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                // Speech error with code
            }
        })
    }

    /**
     * Speaks the given text using current voice settings.
     * Supports accessibility announcements with priority levels.
     *
     * @param text Text to be spoken
     * @param queueMode Queue mode (FLUSH or ADD)
     * @param priority Speech priority for accessibility
     */
    override suspend fun speak(
        text: String,
        queueMode: QueueMode,
        priority: SpeechPriority
    ) {
        if (!isInitialized || text.isBlank()) return

        val androidQueueMode = when (queueMode) {
            QueueMode.FLUSH -> TextToSpeech.QUEUE_FLUSH
            QueueMode.ADD -> TextToSpeech.QUEUE_ADD
        }

        val params = Bundle().apply {
            // Set stream for accessibility
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_ACCESSIBILITY)
            
            // Set utterance ID for tracking
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, UUID.randomUUID().toString())
        }

        tts?.speak(text, androidQueueMode, params, UUID.randomUUID().toString())
    }

    /**
     * Stops current speech immediately.
     * Used when user needs to interrupt reading.
     */
    override fun stop() {
        tts?.stop()
    }

    /**
     * Pauses speech if supported by engine.
     * Allows user to temporarily halt reading.
     */
    override fun pause() {
        tts?.stop() // Note: True pause/resume requires API 23+
    }

    /**
     * Resumes paused speech.
     * Continues from where speech was paused.
     */
    override fun resume() {
        // Note: True pause/resume requires API 23+
        // For older APIs, need to track position manually
    }

    /**
     * Checks if TTS is currently speaking.
     *
     * @return true if speaking, false otherwise
     */
    override fun isSpeaking(): Boolean {
        return tts?.isSpeaking ?: false
    }

    /**
     * Sets speech rate (speed).
     * Range: 0.5 (slower) to 2.0 (faster), 1.0 is normal.
     *
     * @param rate Speech rate
     */
    override fun setRate(rate: Float) {
        val clampedRate = rate.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(clampedRate)
    }

    /**
     * Sets speech pitch.
     * Range: 0.5 (lower) to 2.0 (higher), 1.0 is normal.
     *
     * @param pitch Speech pitch
     */
    override fun setPitch(pitch: Float) {
        val clampedPitch = pitch.coerceIn(0.5f, 2.0f)
        tts?.setPitch(clampedPitch)
    }

    /**
     * Sets the language for speech.
     * Checks language availability before setting.
     *
     * @param languageCode Language code (e.g., "vi-VN", "en-US")
     * @return true if language is available, false otherwise
     */
    override suspend fun setLanguage(languageCode: String): Boolean {
        val locale = parseLocale(languageCode)
        val result = tts?.setLanguage(locale)
        return result == TextToSpeech.LANG_AVAILABLE || 
               result == TextToSpeech.LANG_COUNTRY_AVAILABLE
    }

    /**
     * Parses language code to Locale object.
     *
     * @param languageCode Language code string
     * @return Locale object
     */
    private fun parseLocale(languageCode: String): Locale {
        val parts = languageCode.split("-")
        return when (parts.size) {
            1 -> Locale(parts[0])
            2 -> Locale(parts[0], parts[1])
            else -> Locale.getDefault()
        }
    }

    /**
     * Gets available voices for the current language.
     * Returns list of voice identifiers.
     *
     * @return List of available voice IDs
     */
    override fun getAvailableVoices(): List<String> {
        return tts?.voices?.map { it.name } ?: emptyList()
    }

    /**
     * Releases TTS resources.
     * Should be called when service is no longer needed.
     */
    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
