package com.lifelover.companion159.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import kotlinx.serialization.json.Json
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Singleton object that creates and configures our Retrofit instance
 * Singleton means only one instance exists throughout the app's lifetime
 */
object RetrofitInstance {

    /**
     * OkHttpClient handles the actual HTTP requests
     * We configure it with interceptors for logging
     */
    private val client = OkHttpClient.Builder()
        .addInterceptor(
            // HttpLoggingInterceptor logs all network requests/responses
            // Very useful for debugging API calls
            HttpLoggingInterceptor().apply {
                // BODY level logs request/response headers and body content
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .build()

    /**
     * Lazy initialization means this is created only when first accessed
     * This improves app startup time
     */
    val api: ApiService by lazy {
        Retrofit.Builder()
            // Base URL for all API calls
            .baseUrl("https://jsonplaceholder.typicode.com/")
            // Use our configured OkHttpClient
            .client(client)
            // Tell Retrofit how to convert JSON to Kotlin objects
            .addConverterFactory(
                Json.asConverterFactory("application/json".toMediaType())
            )
            .build()
            // Create implementation of our ApiService interface
            .create(ApiService::class.java)
    }
}