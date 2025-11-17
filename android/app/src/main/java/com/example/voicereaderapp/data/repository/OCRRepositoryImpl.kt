package com.example.voicereaderapp.data.repository

import com.example.voicereaderapp.data.remote.api.VoiceReaderAPI
import com.example.voicereaderapp.data.remote.model.OCRResponse
import com.example.voicereaderapp.domain.repository.OCRRepository
import com.example.voicereaderapp.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * Implementation of OCR repository
 * Handles communication with backend OCR API
 */
class OCRRepositoryImpl(
    private val api: VoiceReaderAPI
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
                    "file",
                    file.name,
                    requestBody
                )

                // Make API call
                val response = api.performOCR(multipartBody)

                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
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
                    "file",
                    file.name,
                    fileBody
                )

                // Create crop coordinate bodies
                val xBody = x.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val yBody = y.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val widthBody = width.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val heightBody = height.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                // Make API call
                val response = api.performOCRWithCrop(
                    filePart,
                    xBody,
                    yBody,
                    widthBody,
                    heightBody
                )

                if (response.isSuccessful && response.body() != null) {
                    Result.Success(response.body()!!)
                } else {
                    Result.Error(Exception("OCR crop failed: ${response.message()}"))
                }

            } catch (e: Exception) {
                Result.Error(e)
            }
        }
    }
}
