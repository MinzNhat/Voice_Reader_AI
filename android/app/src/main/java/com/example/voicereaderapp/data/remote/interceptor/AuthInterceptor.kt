package com.example.voicereaderapp.data.remote.interceptor

import com.example.voicereaderapp.data.remote.ApiConstants
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val requestBuilder = originalRequest.newBuilder()
            .addHeader("X-API-Key", ApiConstants.API_KEY)

        // ⚠️ Don't override Content-Type - let Retrofit handle it
        // Multipart requests need "multipart/form-data"
        // JSON requests need "application/json"
        // Retrofit sets these automatically

        return chain.proceed(requestBuilder.build())
    }
}
