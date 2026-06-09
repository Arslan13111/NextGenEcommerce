package com.example.nextgenecommerce.data.local.dao

import androidx.room.*
import com.example.nextgenecommerce.data.models.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE userId = :userId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllNotifications(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isRead = 0 AND isDeleted = 0 AND userId = :userId ORDER BY createdAt DESC")
    fun getUnreadNotifications(userId: String): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0 AND isDeleted = 0 AND userId = :userId")
    fun getUnreadNotificationCount(userId: String): Flow<Int>

    @Query("SELECT * FROM notifications WHERE id = :notificationId")
    suspend fun getNotificationById(notificationId: Int): NotificationEntity?

    // Checks ALL rows including soft-deleted ones — prevents re-syncing dismissed notifications
    @Query("SELECT COUNT(*) > 0 FROM notifications WHERE supabaseId = :supabaseId")
    suspend fun existsBySupabaseId(supabaseId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Update
    suspend fun updateNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: Int)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId AND isDeleted = 0")
    suspend fun markAllAsRead(userId: String)

    // Soft-deletes — rows stay in DB so existsBySupabaseId keeps returning true
    @Query("UPDATE notifications SET isDeleted = 1 WHERE id = :notificationId")
    suspend fun deleteNotificationById(notificationId: Int)

    @Query("UPDATE notifications SET isDeleted = 1 WHERE userId = :userId")
    suspend fun clearAllNotifications(userId: String)

    @Query("UPDATE notifications SET isDeleted = 1 WHERE isRead = 1 AND userId = :userId")
    suspend fun deleteReadNotifications(userId: String)
}
