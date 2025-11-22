package com.example.voicereaderapp.data.remote

import com.example.voicereaderapp.data.remote.api.VoiceReaderAPI
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Network module for Retrofit configuration
 * Provides VoiceReaderAPI instance
 */
object NetworkModule {

    // TODO: Update this to your backend server URL
    // For Android Emulator: http://10.0.2.2:3000
    // For physical device: http://YOUR_IP:3000
    private const val BASE_URL = "http://192.168.1.16:3000/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: VoiceReaderAPI = retrofit.create(VoiceReaderAPI::class.java)

    /**
     * Helper function to update base URL at runtime
     * Useful for switching between emulator and physical device
     */
    fun createApiWithBaseUrl(baseUrl: String): VoiceReaderAPI {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VoiceReaderAPI::class.java)
    }
}
