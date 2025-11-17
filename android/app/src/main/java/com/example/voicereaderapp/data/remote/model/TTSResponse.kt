package com.example.voicereaderapp.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * TTS API Response from backend
 * Contains base64 encoded MP3 audio
 */
data class TTSResponse(
    @SerializedName("audio")
    val audio: String // base64 encoded MP3
)

/**
 * TTS Request body
 */
data class TTSRequest(
    @SerializedName("text")
    val text: String,

    @SerializedName("speaker")
    val speaker: String = "matt" // Default: Matt (male English speaker)
)
