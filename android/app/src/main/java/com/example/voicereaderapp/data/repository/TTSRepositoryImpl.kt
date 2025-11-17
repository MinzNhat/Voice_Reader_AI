package com.example.voicereaderapp.data.repository

import android.media.MediaPlayer
import android.util.Base64
import com.example.voicereaderapp.data.remote.api.VoiceReaderAPI
import com.example.voicereaderapp.data.remote.model.TTSRequest
import com.example.voicereaderapp.data.remote.model.TimingRequest
import com.example.voicereaderapp.data.remote.model.TimingResponse
import com.example.voicereaderapp.domain.repository.TTSRepository
import com.example.voicereaderapp.utils.Result
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

/**
 * Implementation of TTS repository
 * Handles TTS API calls and audio playback
 */
class TTSRepositoryImpl(
    private val api: VoiceReaderAPI
) : TTSRepository {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    override suspend fun generateSpeech(
        text: String,
        speaker: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = TTSRequest(text, speaker)
                val response = api.generateTTS(request)

                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!.audio)
                } else {
                    Result.Error(Exception("TTS failed: ${response.message()}"))
                }

            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun getWordTimings(text: String): Result<TimingResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = TimingRequest(text)
                val response = api.getWordTimings(request)

                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error(Exception("Timing failed: ${response.message()}"))
                }

            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun playAudio(
        base64Audio: String,
        playbackSpeed: Float,
        onProgress: (Long) -> Unit,
        onComplete: () -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Stop any existing playback
                stopAudio()

                // Decode base64 to bytes
                val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)

                // Create temp file
                val tempFile = File.createTempFile("tts_audio", ".mp3")
                tempFile.deleteOnExit()

                // Write audio to temp file
                FileOutputStream(tempFile).use { fos ->
                    fos.write(audioBytes)
                }

                // Initialize MediaPlayer
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(tempFile.absolutePath)
                    prepare()

                    // Set playback speed (API 23+)
                    try {
                        playbackParams = playbackParams.setSpeed(playbackSpeed.coerceIn(0.5f, 2.0f))
                    } catch (e: Exception) {
                        // Fallback for older devices
                        e.printStackTrace()
                    }

                    setOnCompletionListener {
                        stopAudio()
                        onComplete()
                    }

                    start()
                }

                // Start progress tracking
                progressJob = CoroutineScope(Dispatchers.Main).launch {
                    while (mediaPlayer?.isPlaying == true) {
                        val currentPosition = mediaPlayer?.currentPosition?.toLong() ?: 0
                        onProgress(currentPosition)
                        delay(50) // Update every 50ms
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                stopAudio()
            }
        }
    }

    override fun stopAudio() {
        progressJob?.cancel()
        progressJob = null

        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }

    override fun pauseAudio() {
        mediaPlayer?.apply {
            if (isPlaying) {
                pause()
            }
        }
    }

    override fun resumeAudio() {
        mediaPlayer?.apply {
            if (!isPlaying) {
                start()
            }
        }
    }

    override fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    override fun setPlaybackSpeed(speed: Float) {
        try {
            mediaPlayer?.playbackParams = mediaPlayer?.playbackParams?.setSpeed(
                speed.coerceIn(0.5f, 2.0f)
            ) ?: return
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
