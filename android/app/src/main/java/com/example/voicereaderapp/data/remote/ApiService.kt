package com.example.voicereaderapp.data.remote

import com.example.voicereaderapp.data.remote.dto.*
import com.example.voicereaderapp.data.remote.model.OCRResponse  // ← ADD: Backend returns full model
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET(ApiConstants.Endpoints.HEALTH)
    suspend fun healthCheck(): Response<Map<String, Any>>

    // OCR Endpoints
    @Multipart
    @POST(ApiConstants.Endpoints.OCR_EXTRACT)
    suspend fun extractTextFromImage(
        @Part image: MultipartBody.Part,
        @Part("language") language: RequestBody
    ): Response<ApiResponse<OCRResponse>>  // ← CHANGED: OCRResponse (model with words)

    // TTS Endpoints
    @POST(ApiConstants.Endpoints.TTS_SYNTHESIZE)
    suspend fun synthesizeSpeech(
        @Body request: TtsRequest
    ): Response<ApiResponse<TtsResponse>>

    @GET(ApiConstants.Endpoints.TTS_VOICES)
    suspend fun getAvailableVoices(
        @Query("language") language: String
    ): Response<ApiResponse<List<VoiceInfo>>>

    // PDF Endpoints
    @Multipart
    @POST(ApiConstants.Endpoints.PDF_EXTRACT)
    suspend fun extractTextFromPdf(
        @Part pdf: MultipartBody.Part
    ): Response<ApiResponse<OCRResponse>>  // ← CHANGED: OCRResponse (model with words)

    @Multipart
    @POST(ApiConstants.Endpoints.PDF_EXTRACT_PAGE)
    suspend fun extractTextFromPdfPage(
        @Part pdf: MultipartBody.Part,
        @Part("page") page: RequestBody
    ): Response<ApiResponse<PdfResponse>>

    @Multipart
    @POST(ApiConstants.Endpoints.PDF_METADATA)
    suspend fun getPdfMetadata(
        @Part pdf: MultipartBody.Part
    ): Response<ApiResponse<PdfMetadataResponse>>
}
