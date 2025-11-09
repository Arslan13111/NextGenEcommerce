package com.example.nextgenecommerce.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.models.User
import com.example.nextgenecommerce.data.repository.AuthRepository
import com.example.nextgenecommerce.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
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
        authRepository.logout()
        _currentUser.value = null
        _authState.value = null
    }

    fun isUserLoggedIn(): Boolean = authRepository.isUserLoggedIn()

    fun resetAuthState() {
        _authState.value = null
    }
}
