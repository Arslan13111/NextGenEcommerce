package com.example.nextgenecommerce.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdminCommission(
    val id: String = "",
    @SerialName("order_id")        val orderId: String = "",
    @SerialName("commission_type") val commissionType: String = "",
    val amount: Double = 0.0,
    @SerialName("created_at")      val createdAt: String = ""
)

data class VaultSummary(
    val retailerCommission: Double,
    val deliveryCommission: Double,
    val total: Double
)
