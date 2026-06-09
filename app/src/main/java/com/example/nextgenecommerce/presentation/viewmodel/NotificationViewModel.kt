package com.example.nextgenecommerce.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.models.NotificationEntity
import com.example.nextgenecommerce.data.repository.AuthRepository
import com.example.nextgenecommerce.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private var listJob: Job? = null
    private var countJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.sharedUser
                .map { it?.id ?: "" }
                .distinctUntilChanged()
                .collect { uid ->
                    listJob?.cancel()
                    countJob?.cancel()
                    listJob = launch {
                        notificationRepository.getAllNotifications(uid).collect {
                            _notifications.value = it
                        }
                    }
                    countJob = launch {
                        notificationRepository.getUnreadNotificationCount(uid).collect {
                            _unreadCount.value = it
                        }
                    }
                }
        }
    }

    private fun currentUserId() = authRepository.sharedUser.value?.id ?: ""

    fun markAsRead(notificationId: Int) {
        viewModelScope.launch { notificationRepository.markAsRead(notificationId) }
    }

    fun markAllAsRead() {
        viewModelScope.launch { notificationRepository.markAllAsRead(currentUserId()) }
    }

    fun deleteNotification(notificationId: Int) {
        viewModelScope.launch { notificationRepository.deleteNotificationById(notificationId) }
    }

    fun clearAllNotifications() {
        viewModelScope.launch { notificationRepository.clearAllNotifications(currentUserId()) }
    }

    fun deleteReadNotifications() {
        viewModelScope.launch { notificationRepository.deleteReadNotifications(currentUserId()) }
    }
}
