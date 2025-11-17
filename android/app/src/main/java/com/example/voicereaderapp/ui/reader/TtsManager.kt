package com.example.voicereaderapp.ui.reader

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var onWordSpoken: ((Int) -> Unit)? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("vi", "VN") // ⭐ Giọng tiếng Việt
        }
    }

    fun initialize(onWordSpoken: (Int) -> Unit) {
        this.onWordSpoken = onWordSpoken
        tts = TextToSpeech(context, this)
    }

    fun speak(words: List<String>, startIndex: Int) {
        if (tts == null) return

        val text = words.joinToString(" ")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "reader_id")

        // Fake callback từng từ (TTS chuẩn Android không trả sự kiện từng từ)
        // Nếu muốn chính xác hơn → dùng TTS API mới của Google (Speech Engine mới)
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
    }

    fun setSpeed(speed: Float) {
        tts?.setSpeechRate(speed)
    }

    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch)
    }
}
