package com.example.nextgenecommerce.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SafepayApiService {

    /**
     * POST /order/v1/init
     * Creates a Safepay payment tracker. Returns a token used to open the checkout URL.
     */
    @POST("order/v1/init")
    suspend fun createPaymentSession(
        @Body request: SafepayPaymentRequest
    ): Response<SafepayPaymentResponse>
}

// ── Request ───────────────────────────────────────────────────────────────────

data class SafepayPaymentRequest(
    val client: String = SafepayClient.SAFEPAY_PUBLIC_KEY,
    val amount: Double,
    val currency: String = "PKR",
    val environment: String = SafepayClient.SAFEPAY_ENV
)

// ── Response ──────────────────────────────────────────────────────────────────
// Response shape: { "data": { "token": "track_xxx", ... }, "status": { "message": "success" } }

data class SafepayPaymentResponse(
    val data: SafepayData?,
    val status: SafepayStatus?
)

data class SafepayData(
    val token: String?
)

data class SafepayStatus(
    val message: String?,
    val errors: List<String>?
)
