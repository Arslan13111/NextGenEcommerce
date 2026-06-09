package com.example.nextgenecommerce.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.models.User
import com.example.nextgenecommerce.data.repository.AuthRepository
import com.example.nextgenecommerce.data.repository.PushNotificationRepository
import com.example.nextgenecommerce.data.repository.StorageRepository
import com.example.nextgenecommerce.util.Resource
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val storageRepository: StorageRepository,
    private val pushNotificationRepository: PushNotificationRepository,
    @ApplicationContext private val context: Context
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

    fun register(email: String, password: String, name: String, role: String = "customer") {
        viewModelScope.launch {
            authRepository.register(email, password, name, role).collect {
                _authState.value = it
                if (it is Resource.Success) {
                    authRepository.updateSharedUser(it.data)
                    uploadFcmToken(it.data)
                }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            authRepository.login(email, password).collect {
                _authState.value = it
                if (it is Resource.Success) {
                    authRepository.updateSharedUser(it.data)
                    uploadFcmToken(it.data)
                }
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            authRepository.loginWithGoogle(idToken, allowCreation = false).collect {
                _authState.value = it
                if (it is Resource.Success) {
                    authRepository.updateSharedUser(it.data)
                    uploadFcmToken(it.data)
                }
            }
        }
    }

    fun registerWithGoogle(idToken: String) {
        viewModelScope.launch {
            authRepository.loginWithGoogle(idToken, allowCreation = true).collect {
                _authState.value = it
                if (it is Resource.Success) {
                    authRepository.updateSharedUser(it.data)
                    uploadFcmToken(it.data)
                }
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
                    // Refresh FCM token on every app start so it stays current
                    uploadFcmToken(it.data)
                }
            }
        }
    }

    // Fetches the current FCM token from Firebase and saves it to Supabase so the server
    // knows which device to target for push notifications.
    private fun uploadFcmToken(user: User?) {
        user ?: return
        val role = when {
            user.isAdmin() -> "admin"
            user.isRetailer() -> "retailer"
            user.isDeliveryPartner() -> "delivery_partner"
            else -> "customer"
        }
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            // Keep SharedPreferences in sync for NextGenFirebaseMessagingService
            context.getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
                .edit().putString("fcm_token", token).apply()
            viewModelScope.launch {
                pushNotificationRepository.saveFcmToken(user.id, token, role)
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

    private val _deleteAccountState = MutableStateFlow<Resource<Boolean>?>(null)
    val deleteAccountState: StateFlow<Resource<Boolean>?> = _deleteAccountState.asStateFlow()

    fun deleteAccount() {
        viewModelScope.launch {
            authRepository.deleteAccount().collect { result ->
                _deleteAccountState.value = result
                if (result is Resource.Success) {
                    authRepository.updateSharedUser(null)
                    authRepository.setAdminVerified(false)
                    _authState.value = null
                    _adminLoginState.value = null
                }
            }
        }
    }

    fun resetDeleteAccountState() {
        _deleteAccountState.value = null
    }

    fun isUserLoggedIn(): Boolean = authRepository.isUserLoggedIn()

    private val _changePasswordState = MutableStateFlow<Resource<Boolean>?>(null)
    val changePasswordState: StateFlow<Resource<Boolean>?> = _changePasswordState.asStateFlow()

    private val _detectedRole = MutableStateFlow<Resource<String>?>(null)
    val detectedRole: StateFlow<Resource<String>?> = _detectedRole.asStateFlow()

    fun detectUserRole(email: String) {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _detectedRole.value = null
            return
        }
        viewModelScope.launch {
            authRepository.fetchUserRole(email).collect {
                _detectedRole.value = it
            }
        }
    }

    fun resetDetectedRole() {
        _detectedRole.value = null
    }

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
