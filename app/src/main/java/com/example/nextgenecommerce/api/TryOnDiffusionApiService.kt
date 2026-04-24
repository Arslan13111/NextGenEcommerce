package com.example.nextgenecommerce.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * API Service for Try-On Diffusion via RapidAPI
 * Endpoint: https://try-on-diffusion.p.rapidapi.com/try-on-file
 *
 * Correct field names from API documentation:
 * - avatar_image: The person's photo
 * - clothing_image: The garment/product photo
 */
interface TryOnDiffusionApiService {

    @Multipart
    @POST("try-on-file")
    suspend fun tryOnFile(
        @Part avatar_image: MultipartBody.Part,
        @Part clothing_image: MultipartBody.Part,
        @Header("x-rapidapi-host") rapidApiHost: String = "try-on-diffusion.p.rapidapi.com",
        @Header("x-rapidapi-key") rapidApiKey: String
    ): Response<ResponseBody>
}
