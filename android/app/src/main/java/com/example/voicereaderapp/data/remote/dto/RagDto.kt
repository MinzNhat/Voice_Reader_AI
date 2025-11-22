package com.example.voicereaderapp.data.remote.dto
import com.google.gson.annotations.SerializedName

data class RagRequest(
    @SerializedName("text") val text: String? = null,
    @SerializedName("question") val question: String? = null
)

data class RagResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("answer") val answer: String? = null, // Cho endpoint ask
    @SerializedName("message") val message: String? = null // Cho endpoint ingest
)
