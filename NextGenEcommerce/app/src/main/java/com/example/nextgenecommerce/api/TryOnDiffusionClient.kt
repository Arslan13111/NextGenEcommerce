package com.example.nextgenecommerce.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object TryOnDiffusionClient {

    private const val BASE_URL = "https://try-on-diffusion.p.rapidapi.com/"

    // RapidAPI credentials for Try-On Diffusion
    const val RAPID_API_KEY = "5c1a41631fmsh8e573a5fbb4c9f6p1fd101jsnfebe441d7708"
    const val RAPID_API_HOST = "try-on-diffusion.p.rapidapi.com"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(120, TimeUnit.SECONDS) // Longer timeout for AI processing
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: TryOnDiffusionApiService = retrofit.create(TryOnDiffusionApiService::class.java)
}
