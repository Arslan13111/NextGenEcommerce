package com.example.nextgenecommerce.data.repository

import com.example.nextgenecommerce.data.config.SupabaseConfig
import com.example.nextgenecommerce.data.models.PushNotification
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotificationRepository @Inject constructor() {

    private val _incoming = MutableSharedFlow<PushNotification>(extraBufferCapacity = 64)
    val incoming: Flow<PushNotification> = _incoming.asSharedFlow()

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    suspend fun sendNotification(
        title: String,
        message: String,
        senderId: String,
        senderRole: String,
        recipientRole: String,
        recipientId: String? = null,
        orderId: String? = null
    ): Result<Unit> {
        if (recipientId != null) {
            val targeted = runCatching {
                SupabaseConfig.database.from("push_notifications").insert(
                    buildJsonObject {
                        put("title", title)
                        put("message", message)
                        put("sender_id", senderId)
                        put("sender_role", senderRole)
                        put("recipient_role", recipientRole)
                        put("recipient_id", recipientId)
                        if (orderId != null) put("order_id", orderId)
                    }
                )
            }.map { }
            if (targeted.isSuccess) return targeted
        }
        return runCatching {
            SupabaseConfig.database.from("push_notifications").insert(
                buildJsonObject {
                    put("title", title)
                    put("message", message)
                    put("sender_id", senderId)
                    put("sender_role", senderRole)
                    put("recipient_role", recipientRole)
                    if (orderId != null) put("order_id", orderId)
                }
            )
        }.map { }
    }

    suspend fun getRecentNotifications(
        recipientRole: String,
        currentUserId: String? = null
    ): List<PushNotification> = runCatching {
        SupabaseConfig.database
            .from("push_notifications")
            .select {
                filter {
                    or {
                        eq("recipient_role", recipientRole)
                        eq("recipient_role", "all")
                    }
                }
                order("created_at", Order.DESCENDING)
                limit(100)
            }
            .decodeList<PushNotification>()
            .filter { n ->
                // Keep broadcasts (no specific recipient) or notifications targeted at this user
                currentUserId == null || n.recipientId == null || n.recipientId == currentUserId
            }
    }.getOrDefault(emptyList())

    private var activeUserId: String? = null

    fun startListening(scope: CoroutineScope, userRole: String, userId: String? = null) {
        if (activeUserId == userId) return
        activeUserId = userId

        val realtimeChannel = SupabaseConfig.client.realtime.channel("push_notifications_channel")

        // Flow must be registered BEFORE subscribe() activates the channel
        realtimeChannel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "push_notifications"
        }.onEach { insert ->
            runCatching {
                val notification = json.decodeFromJsonElement(
                    PushNotification.serializer(),
                    insert.record
                )
                val roleMatches = notification.recipientRole == "all" || notification.recipientRole == userRole
                val userMatches = notification.recipientId == null || notification.recipientId == userId
                if (roleMatches && userMatches) {
                    _incoming.emit(notification)
                }
            }
        }.launchIn(scope)

        scope.launch {
            realtimeChannel.subscribe()
        }
    }

    suspend fun saveFcmToken(userId: String, token: String, role: String) {
        runCatching {
            SupabaseConfig.database
                .from("user_fcm_tokens")
                .upsert(
                    buildJsonObject {
                        put("user_id", userId)
                        put("fcm_token", token)
                        put("role", role)
                    },
                    onConflict = "user_id"
                )
        }
    }
}
