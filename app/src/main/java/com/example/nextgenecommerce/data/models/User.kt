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
    val createdAt: String? = null,
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
               id == AdminConfig.ADMIN_USER_ID ||
               AdminConfig.ADMIN_EMAILS.any { it.equals(email, ignoreCase = true) }
    }

    fun isRetailer(): Boolean {
        return role.equals("retailer", ignoreCase = true)
    }

    fun isDeliveryPartner(): Boolean {
        return role.equals("delivery_partner", ignoreCase = true)
    }
}

object AdminConfig {
    // ── Add every admin email here ──────────────────────────────────────────
    // Any email in this set bypasses the role/UUID checks entirely.
    val ADMIN_EMAILS: Set<String> = setOf(
        "arslanmunawar1311@gmail.com",
        "huzaifanadeem1192@gmail.com"
    )

    // Optional: also paste the UUID from Supabase → Authentication → Users
    // Leave as-is if you don't know it — ADMIN_EMAILS above is enough.
    const val ADMIN_USER_ID = "YOUR_ADMIN_UUID_HERE"

    const val ADMIN_ROLE    = "admin"
    const val CUSTOMER_ROLE = "customer"
    const val RETAILER_ROLE = "retailer"
    const val DELIVERY_ROLE = "delivery_partner"
}

enum class UserRole {
    CUSTOMER,
    ADMIN,
    RETAILER,
    DELIVERY_PARTNER
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
    val province: String = "", // Changed from state for Pakistan
    val postalCode: String = "", // Changed from zipCode
    val country: String = "Pakistan", // Changed from USA
    val isDefault: Boolean = false
)

/**
 * Payment transaction model for tracking payment status
 * Used for Easypaisa, JazzCash mock payments
 */
data class PaymentTransaction(
    val id: String = "",
    val orderId: String = "",
    val paymentMethod: String = "",
    val transactionId: String = "",
    val mobileNumber: String? = null,
    val status: PaymentTransactionStatus = PaymentTransactionStatus.PENDING,
    val amount: Double = 0.0,
    val currency: String = "PKR",
    val otpVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

enum class PaymentTransactionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
