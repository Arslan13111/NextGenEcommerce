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
    val updatedAt: String? = null
) {
    /**
     * Check if this user has admin privileges.
     * Admin access comes only from the persisted users.role value.
     */
    fun isAdmin(): Boolean {
        return role.equals(AdminConfig.ADMIN_ROLE, ignoreCase = true)
    }

    fun isRetailer(): Boolean {
        return role.equals(AdminConfig.RETAILER_ROLE, ignoreCase = true)
    }

    fun isDeliveryPartner(): Boolean {
        return role.equals(AdminConfig.DELIVERY_ROLE, ignoreCase = true)
    }
}

object AdminConfig {
    const val ADMIN_ROLE = "admin"
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
    val label: String = "",
    val fullName: String = "",
    val phone: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val province: String = "",
    val postalCode: String = "",
    val country: String = "Pakistan",
    val isDefault: Boolean = false
)

/**
 * Payment transaction model for tracking payment status.
 * Used for Easypaisa and JazzCash mock payments.
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
