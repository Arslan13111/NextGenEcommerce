package com.example.nextgenecommerce.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object SafepayClient {

    const val SAFEPAY_ENV        = "sandbox"
    const val SAFEPAY_PUBLIC_KEY = "sec_9f434394-c7a1-4725-ad25-1ece9333e1f4"
    const val SAFEPAY_SECRET_KEY = "5ee3d978c15c99591255bd6a2805e32c783d108e0ea9c660762dbebb1a95027f"

    private const val BASE_URL = "https://sandbox.api.getsafepay.com/"
    // Production: "https://api.getsafepay.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
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
