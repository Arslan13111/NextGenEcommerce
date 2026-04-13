package com.example.nextgenecommerce.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.models.User
import com.example.nextgenecommerce.data.repository.AuthRepository
import com.example.nextgenecommerce.data.repository.StorageRepository
import com.example.nextgenecommerce.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<Resource<User>?>(null)
    val authState: StateFlow<Resource<User>?> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        checkCurrentUser()
    }

    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            authRepository.register(email, password, name).collect {
                _authState.value = it
                if (it is Resource.Success) {
                    _currentUser.value = it.data
                }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            authRepository.login(email, password).collect {
                _authState.value = it
                if (it is Resource.Success) {
                    _currentUser.value = it.data
                }
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            authRepository.loginWithGoogle(idToken).collect {
                _authState.value = it
                if (it is Resource.Success) {
                    _currentUser.value = it.data
                }
            }
        }
    }

    fun checkCurrentUser() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect {
                if (it is Resource.Success) {
                    _currentUser.value = it.data
                }
            }
        }
    }

    fun updateUserProfile(user: User) {
        viewModelScope.launch {
            authRepository.updateUserProfile(user).collect {
                if (it is Resource.Success) {
                    _currentUser.value = it.data
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _currentUser.value = null
            _authState.value = null
        }
    }

    fun isUserLoggedIn(): Boolean = authRepository.isUserLoggedIn()

    fun resetAuthState() {
        _authState.value = null
    }

    fun uploadProfilePicture(imageUri: Uri) {
        viewModelScope.launch {
            val currentUser = _currentUser.value
            if (currentUser != null) {
                storageRepository.uploadProfileImage(currentUser.id, imageUri).collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            val imageUrl = resource.data ?: return@collect
                            val updatedUser = currentUser.copy(profileImageUrl = imageUrl)
                            updateUserProfile(updatedUser)
                        }
                        is Resource.Error -> {
                            _authState.value = Resource.Error(resource.message ?: "Failed to upload image")
                        }
                        is Resource.Loading -> {
                            _authState.value = Resource.Loading()
                        }
                    }
                }
            }
        }
    }

    fun deleteProfilePicture() {
        viewModelScope.launch {
            val currentUser = _currentUser.value
            if (currentUser != null && currentUser.profileImageUrl != null) {
                // Extract filename from URL
                val url = currentUser.profileImageUrl!!
                val filename = url.substringAfterLast("/")

                storageRepository.deleteProfileImage(currentUser.id, filename).collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            val updatedUser = currentUser.copy(profileImageUrl = null)
                            updateUserProfile(updatedUser)
                        }
                        is Resource.Error -> {
                            _authState.value = Resource.Error(resource.message ?: "Failed to delete image")
                        }
                        is Resource.Loading -> {
                            _authState.value = Resource.Loading()
                        }
                    }
                }
            }
        }
    }
}
