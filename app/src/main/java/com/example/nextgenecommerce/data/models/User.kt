package com.example.nextgenecommerce.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    @SerialName("id")
    val id: String = "",
    @SerialName("email")
    val email: String = "",
    @SerialName("name")
    val name: String = "",
    @SerialName("phone")
    val phone: String = "",
    @SerialName("profile_image_url")
    val profileImageUrl: String? = null,
    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @SerialName("role")
    val role: String = "customer",
    @SerialName("updated_at")
    val updatedAt: String? = null  // ISO 8601 timestamp from Supabase
)

enum class UserRole {
    CUSTOMER,
    ADMIN
}

data class Address(
    val id: String = "",
    val userId: String = "",
    val label: String = "", // Home, Office, etc.
    val fullName: String = "",
    val phone: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val country: String = "USA",
    val isDefault: Boolean = false
)
