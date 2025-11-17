package com.example.voicereaderapp.data.remote.dto

data class TtsRequest(
    val text: String,
    val language: String = "en-US",
    val voice: String? = null,
    val speed: Float = 1.0f,
    val pitch: Float = 0.0f
)

data class TtsResponse(
    val audio: String, // Base64 encoded audio
    val format: String = "mp3",
    val duration: Double? = null
)

data class VoiceInfo(
    val name: String,
    val language: String,
    val gender: String? = null
)
