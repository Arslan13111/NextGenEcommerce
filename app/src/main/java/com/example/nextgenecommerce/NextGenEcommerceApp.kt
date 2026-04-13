package com.example.nextgenecommerce

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NextGenEcommerceApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val orderChannel = NotificationChannel(
                "order_updates",
                "Order Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for order status updates"
            }

            val promotionChannel = NotificationChannel(
                "promotions",
                "Promotions & Offers",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Promotional offers and discounts"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(orderChannel)
            notificationManager.createNotificationChannel(promotionChannel)
        }
    }
}
