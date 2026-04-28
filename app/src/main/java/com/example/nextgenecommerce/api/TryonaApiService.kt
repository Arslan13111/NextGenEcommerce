package com.example.nextgenecommerce.api

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * API Service for Tryona AI Try-On
 * Endpoint: https://api.tryona.com/v1/tryon/simple
 */
interface TryonaApiService {
    @Multipart
    @Headers("Accept: application/json")
    @POST("v1/tryon/simple")
    suspend fun tryOnSimple(
        @Part person_file: MultipartBody.Part,
        @Part garment_file: MultipartBody.Part,
        @Header("tokenapi") tokenapi: String
    ): Response<ResponseBody>
}
