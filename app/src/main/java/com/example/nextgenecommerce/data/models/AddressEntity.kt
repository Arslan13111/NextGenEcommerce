package com.example.nextgenecommerce.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "addresses")
data class AddressEntity(
    @PrimaryKey
    @SerialName("id")
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("label")
    val label: String = "", // Home, Office, etc.
    @SerialName("full_name")
    val fullName: String = "",
    @SerialName("phone")
    val phone: String = "",
    @SerialName("address_line1")
    val addressLine1: String = "",
    @SerialName("address_line2")
    val addressLine2: String = "",
    @SerialName("city")
    val city: String = "",
    @SerialName("province")
    val province: String = "",
    @SerialName("postal_code")
    val postalCode: String = "",
    @SerialName("country")
    val country: String = "Pakistan",
    @SerialName("is_default")
    val isDefault: Boolean = false,
    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

// Extension functions to convert between AddressEntity and Address
fun AddressEntity.toAddress() = Address(
    id = id,
    userId = userId,
    label = label,
    fullName = fullName,
    phone = phone,
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    city = city,
    province = province,
    postalCode = postalCode,
    country = country,
    isDefault = isDefault
)

fun Address.toEntity() = AddressEntity(
    id = id,
    userId = userId,
    label = label,
    fullName = fullName,
    phone = phone,
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    city = city,
    province = province,
    postalCode = postalCode,
    country = country,
    isDefault = isDefault
)
