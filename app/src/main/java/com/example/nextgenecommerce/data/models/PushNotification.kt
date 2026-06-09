package com.example.nextgenecommerce.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PushNotification(
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("message") val message: String = "",
    @SerialName("sender_id") val senderId: String = "",
    @SerialName("sender_role") val senderRole: String = "",
    @SerialName("recipient_role") val recipientRole: String = "",
    @SerialName("recipient_id") val recipientId: String? = null,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

enum class RecipientRole(val value: String, val label: String) {
    ALL("all", "Everyone"),
    CUSTOMER("customer", "Customers"),
    RETAILER("retailer", "Retailers"),
    DELIVERY_PARTNER("delivery_partner", "Delivery Partners")
}
