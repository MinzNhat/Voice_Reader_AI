package com.example.voicereaderapp.data.remote.dto

data class OcrRequest(
    val language: String = "en"
)

data class OcrResponse(
    val text: String,
    val confidence: Double? = null,
    val language: String? = null,
    val processingTime: Long? = null
)
