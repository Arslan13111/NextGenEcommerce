package com.example.nextgenecommerce.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.models.DeliveryPartner
import com.example.nextgenecommerce.data.models.Order
import com.example.nextgenecommerce.data.repository.DeliveryRepository
import com.example.nextgenecommerce.data.repository.EarningsStats
import com.example.nextgenecommerce.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeliveryViewModel @Inject constructor(
    private val deliveryRepository: DeliveryRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow<Resource<DeliveryPartner>?>(null)
    val profileState: StateFlow<Resource<DeliveryPartner>?> = _profileState.asStateFlow()

    private val _profile = MutableStateFlow<DeliveryPartner?>(null)
    val profile: StateFlow<DeliveryPartner?> = _profile.asStateFlow()

    private val _availableOrders = MutableStateFlow<Resource<List<Order>>>(Resource.Loading())
    val availableOrders: StateFlow<Resource<List<Order>>> = _availableOrders.asStateFlow()

    private val _myOrders = MutableStateFlow<Resource<List<Order>>>(Resource.Loading())
    val myOrders: StateFlow<Resource<List<Order>>> = _myOrders.asStateFlow()

    private val _earningsStats = MutableStateFlow<Resource<EarningsStats>>(Resource.Loading())
    val earningsStats: StateFlow<Resource<EarningsStats>> = _earningsStats.asStateFlow()

    private val _deliveryHistory = MutableStateFlow<Resource<List<Order>>>(Resource.Loading())
    val deliveryHistory: StateFlow<Resource<List<Order>>> = _deliveryHistory.asStateFlow()

    private val _actionState = MutableStateFlow<Resource<Unit>?>(null)
    val actionState: StateFlow<Resource<Unit>?> = _actionState.asStateFlow()

    init {
        loadProfile()
        refreshAll()
    }

    fun loadProfile() {
        viewModelScope.launch {
            deliveryRepository.getMyProfile().collect { result ->
                _profileState.value = result
                if (result is Resource.Success) _profile.value = result.data
            }
        }
    }

    fun createProfile(companyName: String, contactPerson: String, address: String, phone: String) {
        viewModelScope.launch {
            deliveryRepository.createProfile(companyName, contactPerson, address, phone).collect { result ->
                _profileState.value = result
                if (result is Resource.Success) _profile.value = result.data
            }
        }
    }

    fun setAvailability(isAvailable: Boolean) {
        val partnerId = _profile.value?.id ?: return
        viewModelScope.launch {
            deliveryRepository.setAvailability(partnerId, isAvailable).collect { result ->
                if (result is Resource.Success) {
                    _profile.value = _profile.value?.copy(isAvailable = isAvailable)
                }
            }
        }
    }

    fun updateProfile(companyName: String, contactPerson: String, address: String, phone: String) {
        viewModelScope.launch {
            deliveryRepository.updateProfile(companyName, contactPerson, address, phone).collect { result ->
                _actionState.value = when (result) {
                    is Resource.Loading -> Resource.Loading()
                    is Resource.Error   -> Resource.Error(result.message ?: "Failed")
                    is Resource.Success -> { _profile.value = result.data; Resource.Success(Unit) }
                }
            }
        }
    }

    fun refreshAll() {
        refreshAvailableOrders()
        refreshMyOrders()
        refreshEarningsStats()
        refreshDeliveryHistory()
    }

    fun refreshAvailableOrders() {
        viewModelScope.launch {
            deliveryRepository.getAvailableOrders().collect { _availableOrders.value = it }
        }
    }

    fun refreshMyOrders() {
        viewModelScope.launch {
            deliveryRepository.getMyOrders().collect { _myOrders.value = it }
        }
    }

    fun refreshEarningsStats() {
        viewModelScope.launch {
            deliveryRepository.getEarningsStats().collect { _earningsStats.value = it }
        }
    }

    fun refreshDeliveryHistory() {
        viewModelScope.launch {
            deliveryRepository.getDeliveryHistory().collect { _deliveryHistory.value = it }
        }
    }

    fun acceptOrder(orderId: String) {
        viewModelScope.launch {
            deliveryRepository.acceptOrder(orderId).collect { result ->
                _actionState.value = result
                if (result is Resource.Success) {
                    refreshAvailableOrders()
                    refreshMyOrders()
                    refreshEarningsStats()
                }
            }
        }
    }

    fun markDelivered(orderId: String) {
        viewModelScope.launch {
            deliveryRepository.markDelivered(orderId).collect { result ->
                _actionState.value = result
                if (result is Resource.Success) {
                    refreshMyOrders()
                    refreshEarningsStats()
                    refreshDeliveryHistory()
                }
            }
        }
    }

    fun startDelivery(orderId: String) {
        viewModelScope.launch {
            deliveryRepository.startDelivery(orderId).collect { result ->
                _actionState.value = result
                if (result is Resource.Success) {
                    refreshMyOrders()
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = null
    }
}
