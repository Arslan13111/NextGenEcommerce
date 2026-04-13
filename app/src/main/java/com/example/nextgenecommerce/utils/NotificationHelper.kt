package com.example.nextgenecommerce.utils

import com.example.nextgenecommerce.data.models.NotificationEntity
import com.example.nextgenecommerce.data.models.NotificationType
import com.example.nextgenecommerce.data.repository.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    private val notificationRepository: NotificationRepository
) {

    suspend fun createSampleNotifications() {
        val sampleNotifications = listOf(
            NotificationEntity(
                title = "Welcome to NextGen Store!",
                message = "Thank you for choosing us. Enjoy exclusive deals and offers!",
                type = NotificationType.GENERAL,
                isRead = false
            ),
            NotificationEntity(
                title = "Flash Sale Alert!",
                message = "Get up to 50% off on selected items. Limited time offer!",
                type = NotificationType.PROMOTION,
                isRead = false
            ),
            NotificationEntity(
                title = "Order Shipped",
                message = "Your order #12345 has been shipped and is on the way!",
                type = NotificationType.DELIVERY,
                orderId = "12345",
                isRead = false
            ),
            NotificationEntity(
                title = "Price Drop Alert",
                message = "The item in your wishlist is now 20% cheaper!",
                type = NotificationType.PRICE_DROP,
                productId = "1",
                isRead = true
            ),
            NotificationEntity(
                title = "Back in Stock",
                message = "Good news! The item you wanted is now back in stock.",
                type = NotificationType.PRODUCT_RESTOCK,
                productId = "2",
                isRead = false
            )
        )

        notificationRepository.addNotifications(sampleNotifications)
    }

    suspend fun addOrderNotification(orderId: String, title: String, message: String) {
        notificationRepository.addNotification(
            NotificationEntity(
                title = title,
                message = message,
                type = NotificationType.ORDER_UPDATE,
                orderId = orderId,
                isRead = false
            )
        )
    }

    suspend fun addPromotionNotification(title: String, message: String, productId: String? = null) {
        notificationRepository.addNotification(
            NotificationEntity(
                title = title,
                message = message,
                type = NotificationType.PROMOTION,
                productId = productId,
                isRead = false
            )
        )
    }

    suspend fun addPriceDropNotification(productId: String, productName: String, discount: Int) {
        notificationRepository.addNotification(
            NotificationEntity(
                title = "Price Drop on $productName",
                message = "Great news! $productName is now $discount% cheaper.",
                type = NotificationType.PRICE_DROP,
                productId = productId,
                isRead = false
            )
        )
    }

    suspend fun addRestockNotification(productId: String, productName: String) {
        notificationRepository.addNotification(
            NotificationEntity(
                title = "Back in Stock",
                message = "$productName is now available again!",
                type = NotificationType.PRODUCT_RESTOCK,
                productId = productId,
                isRead = false
            )
        )
    }

    suspend fun addDeliveryNotification(orderId: String, status: String) {
        val (title, message) = when (status) {
            "shipped" -> "Order Shipped" to "Your order #$orderId has been shipped!"
            "out_for_delivery" -> "Out for Delivery" to "Your order #$orderId is out for delivery."
            "delivered" -> "Order Delivered" to "Your order #$orderId has been delivered."
            else -> "Order Update" to "There's an update on your order #$orderId."
        }

        notificationRepository.addNotification(
            NotificationEntity(
                title = title,
                message = message,
                type = NotificationType.DELIVERY,
                orderId = orderId,
                isRead = false
            )
        )
    }

    suspend fun addPaymentNotification(title: String, message: String, orderId: String? = null) {
        notificationRepository.addNotification(
            NotificationEntity(
                title = title,
                message = message,
                type = NotificationType.PAYMENT,
                orderId = orderId,
                isRead = false
            )
        )
    }
}
