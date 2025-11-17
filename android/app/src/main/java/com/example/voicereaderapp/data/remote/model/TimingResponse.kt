package com.example.voicereaderapp.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Timing API Response from backend
 * Provides word-level timing for real-time highlighting
 */
data class TimingResponse(
    @SerializedName("timings")
    val timings: List<WordTiming>
)

data class WordTiming(
    @SerializedName("word")
    val word: String,

    @SerializedName("index")
    val index: Int,

    @SerializedName("startMs")
    val startMs: Long,

    @SerializedName("endMs")
    val endMs: Long
)

/**
 * Timing Request body
 */
data class TimingRequest(
    @SerializedName("text")
    val text: String
)
