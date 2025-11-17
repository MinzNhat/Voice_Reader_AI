package com.example.voicereaderapp.data.repository

import com.example.voicereaderapp.data.remote.ApiService
import com.example.voicereaderapp.data.remote.NetworkResult
import javax.inject.Inject

class TestApiRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun testConnection(): NetworkResult<String> {
        return try {
            val response = apiService.healthCheck()
            if (response.isSuccessful) {
                NetworkResult.Success("Backend connected successfully!")
            } else {
                NetworkResult.Error("Backend returned error: ${response.code()}")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Connection failed: ${e.message}")
        }
    }
}
