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
) {
    /**
     * Check if this user has admin privileges.
     * Admin is determined by:
     * 1. role = 'admin' in the users table
     * 2. OR user ID matches a hardcoded admin UID (fallback)
     */
    fun isAdmin(): Boolean {
        return role.equals("admin", ignoreCase = true) ||
               id == AdminConfig.ADMIN_USER_ID
    }
}

/**
 * Admin configuration constants.
 * Update ADMIN_USER_ID with your actual admin user's UUID from Supabase.
 */
object AdminConfig {
    // Your admin user ID from Supabase
    const val ADMIN_USER_ID = "f0d36218-f72c-468e-a0c6-6e46f28da4d7"

    // Admin role constant
    const val ADMIN_ROLE = "admin"
    const val CUSTOMER_ROLE = "customer"
}

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
