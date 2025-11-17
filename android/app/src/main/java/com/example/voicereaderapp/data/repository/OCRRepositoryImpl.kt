package com.example.voicereaderapp.data.repository

import com.example.voicereaderapp.data.remote.ApiService
import com.example.voicereaderapp.data.remote.model.OCRResponse  // Backend returns this directly
import com.example.voicereaderapp.domain.repository.OCRRepository
import com.example.voicereaderapp.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

/**
 * Implementation of OCR repository
 * Handles communication with backend OCR API
 */
class OCRRepositoryImpl @Inject constructor(
    private val api: ApiService
) : OCRRepository {

    override suspend fun performOCR(file: File): Result<OCRResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Determine media type
                val mediaType = when (file.extension.lowercase()) {
                    "pdf" -> "application/pdf"
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    else -> "application/octet-stream"
                }.toMediaTypeOrNull()

                // Create multipart body
                val requestBody = file.asRequestBody(mediaType)
                val multipartBody = MultipartBody.Part.createFormData(
                    "image", // Changed from "file" to match ApiService
                    file.name,
                    requestBody
                )

                // Make API call - use correct endpoint based on file type
                val response = if (file.extension.lowercase() == "pdf") {
                    // For PDF files
                    api.extractTextFromPdf(multipartBody)
                } else {
                    // For image files
                    val languageBody = "en".toRequestBody("text/plain".toMediaTypeOrNull())
                    api.extractTextFromImage(multipartBody, languageBody)
                }

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.success && apiResponse.data != null) {
                        // ✅ Backend now returns full OCRResponse with words!
                        val ocrModel: OCRResponse = apiResponse.data
                        Result.Success(ocrModel)
                    } else {
                        Result.Error(Exception("OCR failed: ${apiResponse.message ?: apiResponse.error ?: "Unknown error"}"))
                    }
                } else {
                    Result.Error(Exception("OCR failed: ${response.message()}"))
                }

            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }

    override suspend fun performOCRWithCrop(
        file: File,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): Result<OCRResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Determine media type
                val mediaType = when (file.extension.lowercase()) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    else -> "application/octet-stream"
                }.toMediaTypeOrNull()

                // Create multipart body
                val fileBody = file.asRequestBody(mediaType)
                val filePart = MultipartBody.Part.createFormData(
                    "image",
                    file.name,
                    fileBody
                )

                // For crop, we'll use the regular image endpoint for now
                // TODO: Backend needs to implement crop endpoint
                val languageBody = "en".toRequestBody("text/plain".toMediaTypeOrNull())
                val response = api.extractTextFromImage(filePart, languageBody)

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.success && apiResponse.data != null) {
                        // ✅ Backend now returns full OCRResponse with words!
                        val ocrModel: OCRResponse = apiResponse.data
                        Result.Success(ocrModel)
                    } else {
                        Result.Error(Exception("OCR crop failed: ${apiResponse.message ?: apiResponse.error ?: "Unknown error"}"))
                    }
                } else {
                    Result.Error(Exception("OCR crop failed: ${response.message()}"))
                }

            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }
}
