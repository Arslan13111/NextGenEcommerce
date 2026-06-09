package com.example.nextgenecommerce.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.models.Address
import com.example.nextgenecommerce.data.repository.AddressRepository
import com.example.nextgenecommerce.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.gotrue.Auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddressViewModel @Inject constructor(
    private val addressRepository: AddressRepository,
    private val supabaseAuth: Auth
) : ViewModel() {

    private val _addresses = MutableStateFlow<List<Address>>(emptyList())
    val addresses: StateFlow<List<Address>> = _addresses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _operationSuccess = MutableStateFlow<String?>(null)
    val operationSuccess: StateFlow<String?> = _operationSuccess.asStateFlow()

    private val currentUserId: String
        get() = supabaseAuth.currentUserOrNull()?.id ?: ""

    private var collectJob: Job? = null
    private var loadedForUserId: String? = null

    init {
        loadAddresses()
    }

    fun loadAddresses() {
        val userId = currentUserId
        if (userId.isEmpty()) return
        if (userId == loadedForUserId && _addresses.value.isNotEmpty()) return
        loadedForUserId = userId

        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            addressRepository.getUserAddresses(userId).collect {
                _addresses.value = it
            }
        }
        viewModelScope.launch {
            addressRepository.syncAddressesFromSupabase(userId).collect { }
        }
    }

    // Called from screens to ensure addresses are loaded with current auth
    fun refreshAddresses() {
        loadedForUserId = null
        loadAddresses()
    }

    fun addAddress(address: Address) {
        val userId = currentUserId
        if (userId.isEmpty()) return
        viewModelScope.launch {
            addressRepository.addAddress(address.copy(userId = userId)).collect { result ->
                when (result) {
                    is Resource.Loading -> _isLoading.value = true
                    is Resource.Success -> {
                        _isLoading.value = false
                        _operationSuccess.value = "Address added successfully"
                        loadAddresses()
                    }
                    is Resource.Error -> {
                        _isLoading.value = false
                        _error.value = result.message
                    }
                }
            }
        }
    }

    fun updateAddress(address: Address) {
        viewModelScope.launch {
            addressRepository.updateAddress(address).collect { result ->
                when (result) {
                    is Resource.Loading -> _isLoading.value = true
                    is Resource.Success -> {
                        _isLoading.value = false
                        _operationSuccess.value = "Address updated successfully"
                        loadAddresses()
                    }
                    is Resource.Error -> {
                        _isLoading.value = false
                        _error.value = result.message
                    }
                }
            }
        }
    }

    fun deleteAddress(address: Address) {
        viewModelScope.launch {
            addressRepository.deleteAddress(address).collect { result ->
                when (result) {
                    is Resource.Loading -> _isLoading.value = true
                    is Resource.Success -> {
                        _isLoading.value = false
                        _operationSuccess.value = "Address deleted"
                        loadAddresses()
                    }
                    is Resource.Error -> {
                        _isLoading.value = false
                        _error.value = result.message
                    }
                }
            }
        }
    }

    fun setDefaultAddress(address: Address) {
        val userId = currentUserId
        viewModelScope.launch {
            addressRepository.setDefaultAddress(address.id, userId).collect { result ->
                when (result) {
                    is Resource.Loading -> _isLoading.value = true
                    is Resource.Success -> {
                        _isLoading.value = false
                        loadAddresses()
                    }
                    is Resource.Error -> {
                        _isLoading.value = false
                        _error.value = result.message
                    }
                }
            }
        }
    }

    fun clearError() { _error.value = null }
    fun clearSuccess() { _operationSuccess.value = null }
}
