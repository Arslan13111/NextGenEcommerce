package com.example.nextgenecommerce.api

import com.example.nextgenecommerce.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object TryOnDiffusionClient {

    private const val BASE_URL = "https://try-on-diffusion.p.rapidapi.com/"

    val RAPID_API_KEY: String = BuildConfig.RAPID_API_KEY
    val RAPID_API_HOST: String = BuildConfig.RAPID_API_HOST

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.ENABLE_HTTP_LOGGING) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
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
