package com.example.nextgenecommerce.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.models.NotificationEntity
import com.example.nextgenecommerce.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    private val _unreadNotifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val unreadNotifications: StateFlow<List<NotificationEntity>> = _unreadNotifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        loadNotifications()
        loadUnreadNotifications()
        loadUnreadCount()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            notificationRepository.getAllNotifications().collect {
                _notifications.value = it
            }
        }
    }

    private fun loadUnreadNotifications() {
        viewModelScope.launch {
            notificationRepository.getUnreadNotifications().collect {
                _unreadNotifications.value = it
            }
        }
    }

    private fun loadUnreadCount() {
        viewModelScope.launch {
            notificationRepository.getUnreadNotificationCount().collect {
                _unreadCount.value = it
            }
        }
    }

    fun addNotification(notification: NotificationEntity) {
        viewModelScope.launch {
            notificationRepository.addNotification(notification)
        }
    }

    fun markAsRead(notificationId: Int) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
        }
    }

    fun deleteNotification(notificationId: Int) {
        viewModelScope.launch {
            notificationRepository.deleteNotificationById(notificationId)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            notificationRepository.clearAllNotifications()
        }
    }

    fun deleteReadNotifications() {
        viewModelScope.launch {
            notificationRepository.deleteReadNotifications()
        }
    }
}
