package com.example.nextgenecommerce.api

import com.example.nextgenecommerce.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object SafepayClient {

    val SAFEPAY_ENV: String = BuildConfig.SAFEPAY_ENV
    val SAFEPAY_PUBLIC_KEY: String = BuildConfig.SAFEPAY_PUBLIC_KEY

    private val BASE_URL = BuildConfig.SAFEPAY_API_BASE_URL

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.ENABLE_HTTP_LOGGING) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: SafepayApiService = retrofit.create(SafepayApiService::class.java)
}
