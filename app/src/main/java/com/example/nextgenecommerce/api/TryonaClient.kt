package com.example.nextgenecommerce.api

import com.example.nextgenecommerce.BuildConfig
import com.example.nextgenecommerce.util.ApiConstants
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Retrofit Client for Tryona AI Try-On API
 */
object TryonaClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.ENABLE_HTTP_LOGGING) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(ApiConstants.Tryona.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(ApiConstants.Tryona.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(ApiConstants.Tryona.READ_TIMEOUT, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(ApiConstants.Tryona.BASE_URL)
        .client(okHttpClient)
        .build()

    val apiService: TryonaApiService = retrofit.create(TryonaApiService::class.java)
}
