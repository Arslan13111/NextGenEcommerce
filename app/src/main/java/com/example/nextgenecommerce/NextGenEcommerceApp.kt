package com.example.nextgenecommerce

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NextGenEcommerceApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache {
            MemoryCache.Builder(this)
                .maxSizePercent(0.30)   // 30% of available RAM
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(150L * 1024 * 1024)  // 150 MB
                .build()
        }
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .networkCachePolicy(CachePolicy.ENABLED)
        .crossfade(false)
        .respectCacheHeaders(false)   // cache even if server sends no-cache
        .build()

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
