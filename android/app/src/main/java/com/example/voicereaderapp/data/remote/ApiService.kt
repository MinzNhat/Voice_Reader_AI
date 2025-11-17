package com.example.voicereaderapp.data.remote

interface ApiService {

    @Multipart
    @POST("api/ocr")
    suspend fun uploadImageForOcr(
        @Part file: MultipartBody.Part
    ): OcrResponseDto

    @POST("api/tts")
    suspend fun generateTTS(
        @Body request: TtsRequestDto
    ): TtsResponseDto
}
