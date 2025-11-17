package com.example.voicereaderapp.data.repository.utp

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.voicereaderapp.domain.model.utp.Token
import com.example.voicereaderapp.domain.model.utp.UniversalText
import com.example.voicereaderapp.domain.repository.utp.HighlightEvent
import com.example.voicereaderapp.domain.repository.utp.TtsHighlightRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * Layer 3A: TTS + Highlight Implementation
 */
class TtsHighlightRepositoryImpl @Inject constructor(
    private val tts: TextToSpeech
) : TtsHighlightRepository {
    
    private var isPaused = false
    private var currentTokenIndex = 0
    
    override fun speakWithHighlight(
        text: UniversalText,
        speed: Float,
        pitch: Float
    ): Flow<HighlightEvent> = callbackFlow {
        tts.setSpeechRate(speed)
        tts.setPitch(pitch)
        
        currentTokenIndex = 0
        
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                trySend(HighlightEvent.Started)
            }
            
            override fun onDone(utteranceId: String?) {
                trySend(HighlightEvent.Completed)
            }
            
            override fun onError(utteranceId: String?) {
                trySend(HighlightEvent.Error("TTS error"))
            }
            
            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                // Calculate which token is being spoken
                val token = findTokenAtPosition(text.tokens, start, end)
                if (token != null) {
                    val progress = (end - start).toFloat() / token.text.length
                    trySend(HighlightEvent.TokenHighlight(token, progress))
                    currentTokenIndex = token.index
                }
            }
        })
        
        // Speak text with utterance tracking
        val utteranceId = System.currentTimeMillis().toString()
        tts.speak(text.rawText, TextToSpeech.QUEUE_ADD, null, utteranceId)
        
        awaitClose {
            tts.stop()
        }
    }
    
    override fun stop() {
        tts.stop()
        currentTokenIndex = 0
    }
    
    override fun pause() {
        tts.stop()
        isPaused = true
    }
    
    override fun resume() {
        isPaused = false
        // Resume from currentTokenIndex
    }
    
    private fun findTokenAtPosition(tokens: List<Token>, start: Int, end: Int): Token? {
        var currentPos = 0
        for (token in tokens) {
            val tokenEnd = currentPos + token.text.length
            if (start >= currentPos && start < tokenEnd) {
                return token
            }
            currentPos = tokenEnd + 1 // +1 for space
        }
        return null
    }
}
