package com.example.nextgenecommerce.presentation.viewmodel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.R
import com.example.nextgenecommerce.data.models.NotificationEntity
import com.example.nextgenecommerce.data.models.NotificationType
import com.example.nextgenecommerce.data.models.PushNotification
import com.example.nextgenecommerce.data.models.RecipientRole
import com.example.nextgenecommerce.data.repository.NotificationRepository
import com.example.nextgenecommerce.data.repository.PushNotificationRepository
import com.example.nextgenecommerce.presentation.MainComposeActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SendNotificationViewModel @Inject constructor(
    private val repository: PushNotificationRepository,
    private val notificationRepository: NotificationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _sendState = MutableStateFlow<SendState>(SendState.Idle)
    val sendState: StateFlow<SendState> = _sendState.asStateFlow()

    private val _recentNotifications = MutableStateFlow<List<PushNotification>>(emptyList())
    val recentNotifications: StateFlow<List<PushNotification>> = _recentNotifications.asStateFlow()

    sealed class SendState {
        object Idle : SendState()
        object Loading : SendState()
        object Success : SendState()
        data class Error(val message: String) : SendState()
    }

    fun sendNotification(
        title: String,
        message: String,
        senderId: String,
        senderRole: String,
        recipientRole: RecipientRole
    ) {
        if (title.isBlank() || message.isBlank()) {
            _sendState.value = SendState.Error("Title and message cannot be empty")
            return
        }
        viewModelScope.launch {
            _sendState.value = SendState.Loading
            val result = repository.sendNotification(
                title = title,
                message = message,
                senderId = senderId,
                senderRole = senderRole,
                recipientRole = recipientRole.value
            )
            _sendState.value = if (result.isSuccess) {
                loadRecentNotifications(senderRole)
                SendState.Success
            } else {
                SendState.Error(result.exceptionOrNull()?.message ?: "Failed to send notification")
            }
        }
    }

    fun loadRecentNotifications(role: String, userId: String? = null) {
        viewModelScope.launch {
            _recentNotifications.value = repository.getRecentNotifications(role, userId)
        }
    }

    fun startListeningForIncoming(userRole: String, userId: String? = null) {
        val uid = userId ?: ""
        repository.startListening(viewModelScope, userRole, userId)
        viewModelScope.launch {
            repository.incoming.collect { notification ->
                showSystemNotification(notification.title, notification.message)
                saveToLocalNotificationCenter(notification, uid)
            }
        }
        // Sync historical notifications from Supabase so they appear even when
        // the app was closed when the notification was sent.
        viewModelScope.launch {
            val sixtyMinutesAgoMs = System.currentTimeMillis() - 60 * 60 * 1000L
            val history = repository.getRecentNotifications(userRole, userId)
            history.forEach { notification ->
                val id = notification.id.ifBlank { null } ?: return@forEach
                if (!notificationRepository.existsBySupabaseId(id)) {
                    saveToLocalNotificationCenter(notification, uid)
                    // Show system notification for recent ones missed while app was closed
                    val sentAtMs = parseIso8601ToMillis(notification.createdAt)
                    if (sentAtMs > sixtyMinutesAgoMs) {
                        showSystemNotification(notification.title, notification.message)
                    }
                }
            }
        }
    }

    fun resetSendState() {
        _sendState.value = SendState.Idle
    }

    private suspend fun saveToLocalNotificationCenter(notification: PushNotification, userId: String = "") {
        val type = when {
            notification.title.contains("Order", ignoreCase = true) -> NotificationType.ORDER_UPDATE
            notification.title.contains("New Arrival", ignoreCase = true) -> NotificationType.PROMOTION
            notification.title.contains("Delivery", ignoreCase = true) -> NotificationType.DELIVERY
            notification.title.contains("Payment", ignoreCase = true) -> NotificationType.PAYMENT
            else -> NotificationType.GENERAL
        }
        notificationRepository.addNotification(
            NotificationEntity(
                title = notification.title,
                message = notification.message,
                type = type,
                isRead = false,
                orderId = notification.orderId,
                supabaseId = notification.id.ifBlank { null },
                userId = userId,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    private fun parseIso8601ToMillis(iso: String): Long = runCatching {
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.parse(iso.substringBefore(".").substringBefore("Z"))?.time ?: 0L
    }.getOrDefault(0L)

    private fun showSystemNotification(title: String, message: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "push_notifications_channel"
        val channel = NotificationChannel(
            channelId,
            "Admin & Retailer Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications sent by admin and retailers"
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainComposeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
