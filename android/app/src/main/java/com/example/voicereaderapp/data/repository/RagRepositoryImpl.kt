package com.example.voicereaderapp.data.repository

import com.example.voicereaderapp.data.remote.ApiService
import com.example.voicereaderapp.data.remote.dto.RagRequest
import com.example.voicereaderapp.domain.repository.RagRepository
import javax.inject.Inject

class RagRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : RagRepository {
    override suspend fun ingestText(text: String): Result<Boolean> {
        return try {
            val response = apiService.ingestText(RagRequest(text = text))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun askQuestion(question: String): Result<String> {
        return try {
            val response = apiService.askQuestion(RagRequest(question = question))
            if (response.isSuccessful) {
                Result.success(response.body()?.answer ?: "Không có câu trả lời")
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
