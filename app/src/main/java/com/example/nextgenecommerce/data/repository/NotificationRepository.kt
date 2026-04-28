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

    fun getAllNotifications(): Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()

    fun getUnreadNotifications(): Flow<List<NotificationEntity>> = notificationDao.getUnreadNotifications()

    fun getUnreadNotificationCount(): Flow<Int> = notificationDao.getUnreadNotificationCount()

    suspend fun getNotificationById(notificationId: Int): NotificationEntity? {
        return notificationDao.getNotificationById(notificationId)
    }

    suspend fun addNotification(notification: NotificationEntity) {
        notificationDao.insertNotification(notification)
    }

    suspend fun addNotifications(notifications: List<NotificationEntity>) {
        notificationDao.insertNotifications(notifications)
    }

    suspend fun updateNotification(notification: NotificationEntity) {
        notificationDao.updateNotification(notification)
    }

    suspend fun markAsRead(notificationId: Int) {
        notificationDao.markAsRead(notificationId)
    }

    suspend fun markAllAsRead() {
        notificationDao.markAllAsRead()
    }

    suspend fun deleteNotification(notification: NotificationEntity) {
        notificationDao.deleteNotification(notification)
    }

    suspend fun deleteNotificationById(notificationId: Int) {
        notificationDao.deleteNotificationById(notificationId)
    }

    suspend fun clearAllNotifications() {
        notificationDao.clearAllNotifications()
    }

    suspend fun deleteReadNotifications() {
        notificationDao.deleteReadNotifications()
    }
}
