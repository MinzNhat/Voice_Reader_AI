package com.example.voicereaderapp.data.repository

import android.media.MediaPlayer
import android.util.Base64
import com.example.voicereaderapp.data.remote.ApiService
import com.example.voicereaderapp.data.remote.dto.TtsRequest
import com.example.voicereaderapp.data.remote.dto.TtsResponse  // ADD THIS - DTO from API
import com.example.voicereaderapp.data.remote.model.TimingResponse
import com.example.voicereaderapp.data.remote.model.WordTiming
import com.example.voicereaderapp.domain.repository.TTSRepository
import com.example.voicereaderapp.utils.Result
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Implementation of TTS repository
 * Handles TTS API calls and audio playback
 */
class TTSRepositoryImpl @Inject constructor(
    private val api: ApiService
) : TTSRepository {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    override suspend fun generateSpeech(
        text: String,
        speaker: String,
        language: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("TTSRepository", "Generating speech - voice: $speaker, language: $language, text length: ${text.length}")

                // Use DTO TtsRequest with language parameter
                val request = TtsRequest(
                    text = text,
                    voice = speaker,
                    language = language
                )

                // Call synthesizeSpeech (not generateTTS)
                val response = api.synthesizeSpeech(request)

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.success && apiResponse.data != null) {
                        // Return base64 audio string
                        Result.Success(apiResponse.data.audio)
                    } else {
                        Result.Error(Exception("TTS failed: ${apiResponse.message ?: apiResponse.error ?: "Unknown error"}"))
                    }
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
                // TODO: Backend doesn't have timing endpoint yet
                // Generate approximate timings based on word count
                val words = text.split(Regex("\\s+"))
                val avgWordDuration = 350L // ~350ms per word (rough estimate)

                val timings = words.mapIndexed { index, word ->
                    WordTiming(
                        word = word,
                        index = index,
                        startMs = index * avgWordDuration,
                        endMs = (index + 1) * avgWordDuration
                    )
                }

                Result.Success(TimingResponse(timings))

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

                android.util.Log.d("TTSRepository", "🎵 Starting audio playback - base64 length: ${base64Audio.length}")

                // Decode base64 to bytes
                val audioBytes = try {
                    Base64.decode(base64Audio, Base64.DEFAULT)
                } catch (e: IllegalArgumentException) {
                    android.util.Log.e("TTSRepository", "❌ Failed to decode base64 audio", e)
                    throw Exception("Invalid audio format - Base64 decode failed", e)
                }

                android.util.Log.d("TTSRepository", "✅ Audio decoded - ${audioBytes.size} bytes")

                // Create temp file
                val tempFile = File.createTempFile("tts_audio", ".mp3")
                tempFile.deleteOnExit()

                // Write audio to temp file
                FileOutputStream(tempFile).use { fos ->
                    fos.write(audioBytes)
                }

                android.util.Log.d("TTSRepository", "✅ Audio written to temp file: ${tempFile.absolutePath}")

                // Initialize MediaPlayer
                mediaPlayer = MediaPlayer().apply {
                    try {
                        setDataSource(tempFile.absolutePath)
                        prepare()
                        android.util.Log.d("TTSRepository", "✅ MediaPlayer prepared - duration: ${duration}ms")
                    } catch (e: Exception) {
                        android.util.Log.e("TTSRepository", "❌ MediaPlayer preparation failed", e)
                        throw Exception("Failed to prepare audio playback", e)
                    }

                    // Set playback speed (API 23+)
                    try {
                        playbackParams = playbackParams.setSpeed(playbackSpeed.coerceIn(0.5f, 2.0f))
                        android.util.Log.d("TTSRepository", "✅ Playback speed set to: $playbackSpeed")
                    } catch (e: Exception) {
                        // Fallback for older devices
                        android.util.Log.w("TTSRepository", "⚠️ Could not set playback speed", e)
                    }

                    setOnCompletionListener {
                        android.util.Log.d("TTSRepository", "🎵 Audio playback completed")
                        stopAudio()
                        onComplete()
                    }

                    setOnErrorListener { mp, what, extra ->
                        android.util.Log.e("TTSRepository", "❌ MediaPlayer error - what: $what, extra: $extra")
                        stopAudio()
                        false
                    }

                    start()
                    android.util.Log.d("TTSRepository", "✅ MediaPlayer started")
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
                android.util.Log.e("TTSRepository", "❌ playAudio failed", e)
                e.printStackTrace()
                stopAudio()
                throw e  // Propagate error to ViewModel
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

    override fun seekTo(positionMs: Long) {
        try {
            mediaPlayer?.seekTo(positionMs.toInt())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getCurrentPosition(): Long {
        return try {
            mediaPlayer?.currentPosition?.toLong() ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    override fun getDuration(): Long {
        return try {
            mediaPlayer?.duration?.toLong() ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }
}
