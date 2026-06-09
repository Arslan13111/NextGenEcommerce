package com.example.nextgenecommerce.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.nextgenecommerce.R
import com.example.nextgenecommerce.data.repository.PushNotificationRepository
import com.example.nextgenecommerce.presentation.MainComposeActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NextGenFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var pushNotificationRepository: PushNotificationRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val CHANNEL_ID = "push_notifications_channel"
        const val CHANNEL_NAME = "Admin & Retailer Alerts"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token is stored to Supabase after login in AuthViewModel
        // Store it in SharedPreferences so AuthViewModel can pick it up
        getSharedPreferences("fcm_prefs", MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["message"] ?: return
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications from admin and retailers"
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(this, MainComposeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.launch {  }.cancel()
    }
}
