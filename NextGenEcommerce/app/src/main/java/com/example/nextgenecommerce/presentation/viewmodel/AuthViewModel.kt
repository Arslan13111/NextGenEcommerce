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

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _adminLoginState = MutableStateFlow<Resource<User>?>(null)
    val adminLoginState: StateFlow<Resource<User>?> = _adminLoginState.asStateFlow()

    init {
        checkCurrentUser()
    }

    /**
     * Check if the current user has admin privileges
     */
    fun checkAdminStatus() {
        viewModelScope.launch {
            authRepository.isCurrentUserAdmin().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _isAdmin.value = result.data == true
                    }
                    is Resource.Error -> {
                        _isAdmin.value = false
                    }
                    is Resource.Loading -> {
                        // Keep current value while loading
                    }
                }
            }
        }
    }

    /**
     * Login as admin - verifies admin privileges after authentication
     */
    fun adminLogin(email: String, password: String) {
        viewModelScope.launch {
            authRepository.adminLogin(email, password).collect { result ->
                _adminLoginState.value = result
                when (result) {
                    is Resource.Success -> {
                        _currentUser.value = result.data
                        _isAdmin.value = true
                    }
                    is Resource.Error -> {
                        _isAdmin.value = false
                    }
                    is Resource.Loading -> {
                        // Loading state
                    }
                }
            }
        }
    }

    /**
     * Reset admin login state
     */
    fun resetAdminLoginState() {
        _adminLoginState.value = null
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
            authRepository.loginWithGoogle(idToken, allowCreation = false).collect {
                _authState.value = it
                if (it is Resource.Success) {
                    _currentUser.value = it.data
                }
            }
        }
    }

    fun registerWithGoogle(idToken: String) {
        viewModelScope.launch {
            authRepository.loginWithGoogle(idToken, allowCreation = true).collect {
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
                    // Also update admin status based on user data
                    _isAdmin.value = it.data?.isAdmin() == true
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
            _isAdmin.value = false
            _adminLoginState.value = null
        }
    }

    fun isUserLoggedIn(): Boolean = authRepository.isUserLoggedIn()

    private val _changePasswordState = MutableStateFlow<Resource<Boolean>?>(null)
    val changePasswordState: StateFlow<Resource<Boolean>?> = _changePasswordState.asStateFlow()

    fun changePassword(newPassword: String) {
        viewModelScope.launch {
            authRepository.changePassword(newPassword).collect {
                _changePasswordState.value = it
            }
        }
    }

    fun resetChangePasswordState() {
        _changePasswordState.value = null
    }

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
