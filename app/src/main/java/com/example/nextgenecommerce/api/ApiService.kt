package com.example.nextgenecommerce.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class TryOnResponse(
    val success: Boolean,
    val resultImage: String,
    val message: String?,
    val description: String?
)

interface ApiService {
    @POST("/api/try-on")
    suspend fun tryOnClothing(@Body request: Map<String, String>): Response<TryOnResponse>
}