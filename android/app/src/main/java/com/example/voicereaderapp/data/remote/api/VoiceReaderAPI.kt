package com.example.voicereaderapp.data.remote.api

import com.example.voicereaderapp.data.remote.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API interface for Voice Reader backend
 * Base URL should be configured in NetworkModule
 */
interface VoiceReaderAPI {

    /**
     * POST /ocr
     * Upload PDF or image for OCR processing
     * Returns text and bounding boxes
     */
    @Multipart
    @POST("ocr")
    suspend fun performOCR(
        @Part file: MultipartBody.Part
    ): Response<OCRResponse>

    /**
     * POST /ocr/crop
     * Upload image with crop coordinates for OCR
     * Returns text and bounding boxes from cropped region
     */
    @Multipart
    @POST("ocr/crop")
    suspend fun performOCRWithCrop(
        @Part file: MultipartBody.Part,
        @Part("x") x: RequestBody,
        @Part("y") y: RequestBody,
        @Part("width") width: RequestBody,
        @Part("height") height: RequestBody
    ): Response<OCRResponse>

    /**
     * POST /tts
     * Convert text to speech
     * Returns base64 encoded MP3 audio
     */
    @POST("tts")
    suspend fun generateTTS(
        @Body request: TTSRequest
    ): Response<TTSResponse>

    /**
     * POST /tts/timing
     * Get word timing for real-time highlighting
     * Returns timing array with start/end milliseconds
     */
    @POST("tts/timing")
    suspend fun getWordTimings(
        @Body request: TimingRequest
    ): Response<TimingResponse>
}
