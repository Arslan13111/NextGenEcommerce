package com.example.nextgenecommerce.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Retailer(
    @SerialName("id")
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("store_name")
    val storeName: String = "",
    @SerialName("store_description")
    val storeDescription: String = "",
    @SerialName("store_logo_url")
    val storeLogoUrl: String? = null,
    @SerialName("store_address")
    val storeAddress: String = "",
    @SerialName("contact_phone")
    val contactPhone: String = "",
    @SerialName("total_revenue")
    val totalRevenue: Double = 0.0,
    @SerialName("rating")
    val rating: Double = 0.0,
    @SerialName("is_verified")
    val isVerified: Boolean = false,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = ""
)

@Serializable
data class DeliveryPartner(
    @SerialName("id")
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("company_name")
    val companyName: String = "",
    @SerialName("contact_person")
    val contactPerson: String = "",
    @SerialName("company_address")
    val companyAddress: String = "",
    @SerialName("contact_phone")
    val contactPhone: String = "",
    @SerialName("company_logo_url")
    val companyLogoUrl: String? = null,
    @SerialName("is_available")
    val isAvailable: Boolean = true,
    @SerialName("rating")
    val rating: Double = 0.0,
    @SerialName("total_deliveries")
    val totalDeliveries: Int = 0,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = ""
)
