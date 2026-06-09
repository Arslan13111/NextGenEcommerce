package com.example.nextgenecommerce.data.repository

import com.example.nextgenecommerce.data.local.dao.NotificationDao
import com.example.nextgenecommerce.data.models.NotificationEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationDao: NotificationDao
) {

    fun getAllNotifications(userId: String): Flow<List<NotificationEntity>> =
        notificationDao.getAllNotifications(userId)

    fun getUnreadNotifications(userId: String): Flow<List<NotificationEntity>> =
        notificationDao.getUnreadNotifications(userId)

    fun getUnreadNotificationCount(userId: String): Flow<Int> =
        notificationDao.getUnreadNotificationCount(userId)

    suspend fun getNotificationById(notificationId: Int): NotificationEntity? =
        notificationDao.getNotificationById(notificationId)

    suspend fun existsBySupabaseId(supabaseId: String): Boolean =
        notificationDao.existsBySupabaseId(supabaseId)

    suspend fun addNotification(notification: NotificationEntity) {
        notificationDao.insertNotification(notification)
    }

    suspend fun addNotifications(notifications: List<NotificationEntity>) {
        notificationDao.insertNotifications(notifications)
    }

    suspend fun markAsRead(notificationId: Int) {
        notificationDao.markAsRead(notificationId)
    }

    suspend fun markAllAsRead(userId: String) {
        notificationDao.markAllAsRead(userId)
    }

    suspend fun deleteNotificationById(notificationId: Int) {
        notificationDao.deleteNotificationById(notificationId)
    }

    suspend fun clearAllNotifications(userId: String) {
        notificationDao.clearAllNotifications(userId)
    }

    suspend fun deleteReadNotifications(userId: String) {
        notificationDao.deleteReadNotifications(userId)
    }
}
