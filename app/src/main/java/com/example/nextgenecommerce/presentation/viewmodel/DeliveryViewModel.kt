package com.example.nextgenecommerce.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.models.DeliveryPartner
import com.example.nextgenecommerce.data.models.Order
import com.example.nextgenecommerce.data.repository.AdminVaultRepository
import com.example.nextgenecommerce.data.repository.DeliveryRepository
import com.example.nextgenecommerce.data.repository.EarningsStats
import com.example.nextgenecommerce.data.repository.PushNotificationRepository
import com.example.nextgenecommerce.data.repository.RetailerStoreInfo
import com.example.nextgenecommerce.data.repository.StorageRepository
import com.example.nextgenecommerce.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeliveryViewModel @Inject constructor(
    private val deliveryRepository: DeliveryRepository,
    private val storageRepository: StorageRepository,
    private val adminVaultRepository: AdminVaultRepository,
    private val pushNotificationRepository: PushNotificationRepository
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

    private val _availableReturnOrders = MutableStateFlow<Resource<List<Order>>>(Resource.Loading())
    val availableReturnOrders: StateFlow<Resource<List<Order>>> = _availableReturnOrders.asStateFlow()

    private val _myReturnOrders = MutableStateFlow<Resource<List<Order>>>(Resource.Loading())
    val myReturnOrders: StateFlow<Resource<List<Order>>> = _myReturnOrders.asStateFlow()

    private val _retailerInfoMap = MutableStateFlow<Map<String, RetailerStoreInfo>>(emptyMap())
    val retailerInfoMap: StateFlow<Map<String, RetailerStoreInfo>> = _retailerInfoMap.asStateFlow()

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

    fun createProfile(companyName: String, contactPerson: String, address: String, phone: String, imageUris: List<Uri> = emptyList()) {
        viewModelScope.launch {
            val userId = deliveryRepository.getCurrentUserId() ?: ""
            val urls = uploadImages(userId, imageUris)
            deliveryRepository.createProfile(companyName, contactPerson, address, phone, urls).collect { result ->
                _profileState.value = result
                if (result is Resource.Success) _profile.value = result.data
            }
        }
    }

    private suspend fun uploadImages(userId: String, uris: List<Uri>): List<String> {
        val urls = mutableListOf<String>()
        for (uri in uris) {
            storageRepository.uploadProfileImage(userId, uri).collect { result ->
                if (result is Resource.Success) result.data?.let { urls.add(it) }
            }
        }
        return urls
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
        refreshReturnOrders()
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
        val order = (_availableOrders.value as? Resource.Success)?.data?.find { it.id == orderId }
        viewModelScope.launch {
            deliveryRepository.acceptOrder(orderId).collect { result ->
                _actionState.value = result
                if (result is Resource.Success) {
                    order?.let { o ->
                        sendDeliveryNotification(
                            customerId = o.userId,
                            orderId = orderId,
                            title = "Your Order is Shipped 🚚",
                            message = "Your order #${o.orderNumber} has been picked up by the delivery partner and is on its way to you."
                        )
                    }
                    refreshAvailableOrders()
                    refreshMyOrders()
                    refreshEarningsStats()
                }
            }
        }
    }

    fun startDelivery(orderId: String) {
        val order = (_myOrders.value as? Resource.Success)?.data?.find { it.id == orderId }
        viewModelScope.launch {
            deliveryRepository.startDelivery(orderId).collect { result ->
                _actionState.value = result
                if (result is Resource.Success) {
                    order?.let { o ->
                        sendDeliveryNotification(
                            customerId = o.userId,
                            orderId = orderId,
                            title = "Out for Delivery! 📦",
                            message = "Your order #${o.orderNumber} is out for delivery. Expect it at your doorstep soon!"
                        )
                    }
                    refreshMyOrders()
                }
            }
        }
    }

    fun markDelivered(orderId: String) {
        val order = (_myOrders.value as? Resource.Success)?.data?.find { it.id == orderId }
        viewModelScope.launch {
            order?.let { adminVaultRepository.recordCommissionForOrder(it.id, it.subtotal) }
            deliveryRepository.markDelivered(orderId).collect { result ->
                _actionState.value = result
                if (result is Resource.Success) {
                    order?.let { o ->
                        sendDeliveryNotification(
                            customerId = o.userId,
                            orderId = orderId,
                            title = "Order Delivered ✅",
                            message = "Your order #${o.orderNumber} has been delivered successfully. Thank you for shopping with us!"
                        )
                    }
                    refreshMyOrders()
                    refreshEarningsStats()
                    refreshDeliveryHistory()
                }
            }
        }
    }

    fun refreshReturnOrders() {
        viewModelScope.launch {
            deliveryRepository.getAvailableReturnOrders().collect { result ->
                _availableReturnOrders.value = result
                if (result is Resource.Success) loadRetailerInfoForReturnOrders()
            }
        }
        viewModelScope.launch {
            deliveryRepository.getMyReturnOrders().collect { result ->
                _myReturnOrders.value = result
                if (result is Resource.Success) loadRetailerInfoForReturnOrders()
            }
        }
    }

    private fun loadRetailerInfoForReturnOrders() {
        viewModelScope.launch {
            val available = (_availableReturnOrders.value as? Resource.Success)?.data ?: emptyList()
            val inTransit = (_myReturnOrders.value as? Resource.Success)?.data ?: emptyList()
            val retailerIds = (available + inTransit).mapNotNull { it.retailerId }.toSet()
            if (retailerIds.isNotEmpty()) {
                _retailerInfoMap.value = deliveryRepository.getRetailerInfoBatch(retailerIds)
            }
        }
    }

    fun acceptReturnPickup(orderId: String) {
        val order = (_availableReturnOrders.value as? Resource.Success)?.data?.find { it.id == orderId }
        viewModelScope.launch {
            deliveryRepository.acceptReturnPickup(orderId).collect { result ->
                _actionState.value = result
                if (result is Resource.Success) {
                    order?.let { o ->
                        sendDeliveryNotification(
                            customerId = o.userId,
                            orderId = orderId,
                            title = "Return Picked Up 🔄",
                            message = "Your return for order #${o.orderNumber} has been picked up and is on its way back to the retailer."
                        )
                    }
                    refreshReturnOrders()
                }
            }
        }
    }

    fun markReturnDeliveredToRetailer(orderId: String) {
        val order = (_myReturnOrders.value as? Resource.Success)?.data?.find { it.id == orderId }
        viewModelScope.launch {
            deliveryRepository.markReturnDeliveredToRetailer(orderId).collect { result ->
                _actionState.value = result
                if (result is Resource.Success) {
                    order?.let { o ->
                        sendDeliveryNotification(
                            customerId = o.userId,
                            orderId = orderId,
                            title = "Return Completed ✅",
                            message = "Your return for order #${o.orderNumber} has been received by the retailer. Your refund will be processed soon."
                        )
                    }
                    refreshReturnOrders()
                }
            }
        }
    }

    private suspend fun sendDeliveryNotification(
        customerId: String,
        orderId: String,
        title: String,
        message: String
    ) {
        val senderId = deliveryRepository.getCurrentUserId() ?: return
        pushNotificationRepository.sendNotification(
            title = title,
            message = message,
            senderId = senderId,
            senderRole = "delivery_partner",
            recipientRole = "customer",
            recipientId = customerId,
            orderId = orderId
        )
    }

    fun resetActionState() {
        _actionState.value = null
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    private val _allDeliveryPartners = MutableStateFlow<Resource<List<DeliveryPartner>>>(Resource.Loading())
    val allDeliveryPartners: StateFlow<Resource<List<DeliveryPartner>>> = _allDeliveryPartners.asStateFlow()

    private val _deliveryPartnerApprovalState = MutableStateFlow<Resource<Unit>?>(null)
    val deliveryPartnerApprovalState: StateFlow<Resource<Unit>?> = _deliveryPartnerApprovalState.asStateFlow()

    fun loadAllDeliveryPartnersForAdmin() {
        viewModelScope.launch {
            deliveryRepository.getAllDeliveryPartnersForAdmin().collect { _allDeliveryPartners.value = it }
        }
    }

    fun setDeliveryPartnerApproval(partnerId: String, approved: Boolean) {
        viewModelScope.launch {
            deliveryRepository.setDeliveryPartnerApproval(partnerId, approved).collect { result ->
                _deliveryPartnerApprovalState.value = result
                if (result is Resource.Success) loadAllDeliveryPartnersForAdmin()
            }
        }
    }

    fun resetDeliveryPartnerApprovalState() {
        _deliveryPartnerApprovalState.value = null
    }
}
