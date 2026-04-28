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

    // Delegate to the singleton repository so all ViewModel instances share the same user state
    val currentUser: StateFlow<User?> = authRepository.sharedUser

    // Delegate to singleton — survives screen navigation and ViewModel recreation
    val isAdmin: StateFlow<Boolean> = authRepository.isAdmin

    private val _adminLoginState = MutableStateFlow<Resource<User>?>(null)
    val adminLoginState: StateFlow<Resource<User>?> = _adminLoginState.asStateFlow()

    init {
        checkCurrentUser()
    }

    fun checkAdminStatus() {
        if (authRepository.isAdminVerifiedByLogin()) return
        viewModelScope.launch {
            authRepository.isCurrentUserAdmin().collect { result ->
                when (result) {
                    is Resource.Success -> { if (result.data == true) authRepository.setAdminVerified(true) }
                    is Resource.Error -> { /* Don't revoke on error — keep current value */ }
                    is Resource.Loading -> { }
                }
            }
        }
    }

    fun adminLogin(email: String, password: String) {
        viewModelScope.launch {
            authRepository.adminLogin(email, password).collect { result ->
                _adminLoginState.value = result
                when (result) {
                    is Resource.Success -> {
                        authRepository.updateSharedUser(result.data)
                        authRepository.setAdminVerified(true)
                    }
                    is Resource.Error -> {
                        authRepository.setAdminVerified(false)
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }

    fun resetAdminLoginState() {
        _adminLoginState.value = null
    }

    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            authRepository.register(email, password, name).collect {
                _authState.value = it
                if (it is Resource.Success) authRepository.updateSharedUser(it.data)
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            authRepository.login(email, password).collect {
                _authState.value = it
                if (it is Resource.Success) authRepository.updateSharedUser(it.data)
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            authRepository.loginWithGoogle(idToken, allowCreation = false).collect {
                _authState.value = it
                if (it is Resource.Success) authRepository.updateSharedUser(it.data)
            }
        }
    }

    fun registerWithGoogle(idToken: String) {
        viewModelScope.launch {
            authRepository.loginWithGoogle(idToken, allowCreation = true).collect {
                _authState.value = it
                if (it is Resource.Success) authRepository.updateSharedUser(it.data)
            }
        }
    }

    fun checkCurrentUser() {
        viewModelScope.launch {
            authRepository.getCurrentUser().collect {
                if (it is Resource.Success) {
                    authRepository.updateSharedUser(it.data)
                    if (!authRepository.isAdminVerifiedByLogin()) {
                        authRepository.setAdminVerified(it.data?.isAdmin() == true)
                    }
                }
            }
        }
    }

    fun updateUserProfile(user: User) {
        viewModelScope.launch {
            // Optimistic update — reflects in all screens immediately
            authRepository.updateSharedUser(user)
            authRepository.updateUserProfile(user).collect {
                if (it is Resource.Success) authRepository.updateSharedUser(it.data)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            authRepository.updateSharedUser(null)
            authRepository.setAdminVerified(false)
            _authState.value = null
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
            val user = authRepository.sharedUser.value ?: return@launch
            // Optimistic update — show local image in all screens immediately
            authRepository.updateSharedUser(user.copy(profileImageUrl = imageUri.toString()))
            storageRepository.uploadProfileImage(user.id, imageUri).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val imageUrl = resource.data ?: return@collect
                        updateUserProfile(user.copy(profileImageUrl = imageUrl))
                    }
                    is Resource.Error -> {
                        // Revert optimistic update on failure
                        authRepository.updateSharedUser(user)
                        _authState.value = Resource.Error(resource.message ?: "Failed to upload image")
                    }
                    is Resource.Loading -> {
                        _authState.value = Resource.Loading()
                    }
                }
            }
        }
    }

    fun deleteProfilePicture() {
        viewModelScope.launch {
            val user = authRepository.sharedUser.value ?: return@launch
            if (user.profileImageUrl == null) return@launch
            val filename = user.profileImageUrl.substringAfterLast("/")
            // Optimistic update
            authRepository.updateSharedUser(user.copy(profileImageUrl = null))
            storageRepository.deleteProfileImage(user.id, filename).collect { resource ->
                when (resource) {
                    is Resource.Success -> updateUserProfile(user.copy(profileImageUrl = null))
                    is Resource.Error -> {
                        authRepository.updateSharedUser(user)
                        _authState.value = Resource.Error(resource.message ?: "Failed to delete image")
                    }
                    is Resource.Loading -> {}
                }
            }
        }
    }
}
