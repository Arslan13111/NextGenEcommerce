package com.example.nextgenecommerce.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val phone: String = "",
    val profileImageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val role: UserRole = UserRole.CUSTOMER
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
