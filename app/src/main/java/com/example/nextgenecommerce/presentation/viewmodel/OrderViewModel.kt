package com.example.nextgenecommerce.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.models.Order
import com.example.nextgenecommerce.data.models.OrderStatus
import com.example.nextgenecommerce.data.remote.CreateOrderRequest
import com.example.nextgenecommerce.data.repository.OrderRepository
import com.example.nextgenecommerce.data.repository.StorageRepository
import com.example.nextgenecommerce.util.CardTierUtil
import com.example.nextgenecommerce.util.Resource
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val storageRepository: StorageRepository,
    private val authRepository: com.example.nextgenecommerce.data.repository.AuthRepository
) : ViewModel() {

    private val _selectedReturnImages = MutableStateFlow<List<Uri>>(emptyList())
    val selectedReturnImages: StateFlow<List<Uri>> = _selectedReturnImages.asStateFlow()

    fun addReturnImage(uri: Uri) {
        _selectedReturnImages.value = _selectedReturnImages.value + uri
    }

    fun removeReturnImage(uri: Uri) {
        _selectedReturnImages.value = _selectedReturnImages.value - uri
    }

    fun clearReturnImages() {
        _selectedReturnImages.value = emptyList()
    }

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    // Total spent from confirmed/delivered orders — drives card tier
    val totalSpent: StateFlow<Double> = _orders.map { list ->
        list.filter { it.status !in listOf(OrderStatus.CANCELLED, OrderStatus.RETURN_REQUESTED, OrderStatus.RETURNED) }
            .sumOf { it.subtotal }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val userTier: StateFlow<CardTierUtil.CardTier> = totalSpent.map { spent ->
        CardTierUtil.getTierForSpend(spent)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardTierUtil.CardTier.SILVER)

    private val _allAdminOrders = MutableStateFlow<List<Order>>(emptyList())
    val allAdminOrders: StateFlow<List<Order>> = _allAdminOrders.asStateFlow()

    private val _selectedOrder = MutableStateFlow<Order?>(null)
    val selectedOrder: StateFlow<Order?> = _selectedOrder.asStateFlow()

    private val _createOrderState = MutableStateFlow<Resource<Order>?>(null)
    val createOrderState: StateFlow<Resource<Order>?> = _createOrderState.asStateFlow()

    private val _cancelOrderState = MutableStateFlow<Resource<Unit>?>(null)
    val cancelOrderState: StateFlow<Resource<Unit>?> = _cancelOrderState.asStateFlow()

    private val _updateOrderStatusState = MutableStateFlow<Resource<Unit>?>(null)
    val updateOrderStatusState: StateFlow<Resource<Unit>?> = _updateOrderStatusState.asStateFlow()

    private val _returnRequestState = MutableStateFlow<Resource<Unit>?>(null)
    val returnRequestState: StateFlow<Resource<Unit>?> = _returnRequestState.asStateFlow()

    private val _returnRefundState = MutableStateFlow<Resource<Unit>?>(null)
    val returnRefundState: StateFlow<Resource<Unit>?> = _returnRefundState.asStateFlow()

    private val currentUserId: String
        get() = authRepository.sharedUser.value?.id ?: ""

    init {
        viewModelScope.launch {
            authRepository.sharedUser.collect { user ->
                user?.id?.let { userId ->
                    loadOrders(userId)
                    syncOrders(userId)
                }
            }
        }
    }

    fun loadOrders(userId: String = currentUserId) {
        if (userId.isEmpty()) return
        viewModelScope.launch {
            orderRepository.getUserOrders(userId).collect { _orders.value = it }
        }
        viewModelScope.launch {
            orderRepository.syncOrders(userId).collect { }
        }
    }

    fun loadOrderById(orderId: String) {
        viewModelScope.launch {
            orderRepository.getOrderById(orderId).collect { _selectedOrder.value = it }
        }
    }

    fun createOrder(request: CreateOrderRequest) {
        viewModelScope.launch {
            orderRepository.createOrder(request.copy(userId = currentUserId)).collect {
                _createOrderState.value = it
            }
        }
    }

    fun getOrdersByStatus(status: OrderStatus) {
        viewModelScope.launch {
            orderRepository.getOrdersByStatus(status).collect { _orders.value = it }
        }
    }

    fun syncOrders(userId: String = currentUserId) {
        if (userId.isEmpty()) return
        viewModelScope.launch { orderRepository.syncOrders(userId).collect { } }
    }

    fun cancelOrder(orderId: String, reason: String) {
        viewModelScope.launch {
            orderRepository.cancelOrder(orderId, reason).collect {
                _cancelOrderState.value = it
            }
        }
    }

    fun resetCancelOrderState() {
        _cancelOrderState.value = null
    }

    fun requestReturn(orderId: String, reason: String, imageUris: List<Uri> = emptyList()) {
        viewModelScope.launch {
            _returnRequestState.value = Resource.Loading()
            try {
                val imageUrls = if (imageUris.isNotEmpty()) {
                    uploadReturnImages(orderId, imageUris)
                } else {
                    emptyList()
                }

                orderRepository.requestReturn(orderId, reason, imageUrls).collect {
                    _returnRequestState.value = it
                    if (it is Resource.Success) {
                        loadOrderById(orderId)
                        clearReturnImages()
                    }
                }
            } catch (e: Exception) {
                _returnRequestState.value = Resource.Error(e.message ?: "Failed to submit return request")
            }
        }
    }

    private suspend fun uploadReturnImages(orderId: String, uris: List<Uri>): List<String> {
        return uris.map { uri ->
            var imageUrl: String? = null
            var error: String? = null
            
            storageRepository.uploadImage(
                bucketName = "return-images",
                imageUri = uri,
                folder = "returns/$orderId"
            ).collect { resource ->
                when (resource) {
                    is Resource.Success -> imageUrl = resource.data
                    is Resource.Error -> error = resource.message
                    is Resource.Loading -> { /* keep waiting */ }
                }
            }
            
            imageUrl ?: throw Exception(error ?: "Failed to upload return image")
        }
    }

    fun resetReturnRequestState() {
        _returnRequestState.value = null
    }

    fun acceptReturn(orderId: String, adminNote: String) {
        viewModelScope.launch {
            try {
                orderRepository.acceptReturn(orderId, adminNote).collect {
                    _updateOrderStatusState.value = it
                    if (it is Resource.Success) loadAllOrdersForAdmin()
                }
            } catch (e: Exception) {
                _updateOrderStatusState.value = Resource.Error(e.message ?: "Failed to accept return")
            }
        }
    }

    fun rejectReturn(orderId: String, adminNote: String) {
        viewModelScope.launch {
            try {
                orderRepository.rejectReturn(orderId, adminNote).collect {
                    _updateOrderStatusState.value = it
                    if (it is Resource.Success) loadAllOrdersForAdmin()
                }
            } catch (e: Exception) {
                _updateOrderStatusState.value = Resource.Error(e.message ?: "Failed to reject return")
            }
        }
    }

    fun processReturnRefund(orderId: String) {
        viewModelScope.launch {
            try {
                orderRepository.processReturnRefund(orderId).collect {
                    _returnRefundState.value = it
                    if (it is Resource.Success) loadAllOrdersForAdmin()
                }
            } catch (e: Exception) {
                _returnRefundState.value = Resource.Error(e.message ?: "Failed to process refund")
            }
        }
    }

    fun resetReturnRefundState() {
        _returnRefundState.value = null
    }

    fun loadAllOrdersForAdmin() {
        viewModelScope.launch {
            orderRepository.getAllOrdersAdmin().collect { result ->
                if (result is Resource.Success) {
                    _allAdminOrders.value = result.data ?: emptyList()
                }
            }
        }
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        viewModelScope.launch {
            orderRepository.updateOrderStatus(orderId, status).collect {
                _updateOrderStatusState.value = it
                if (it is Resource.Success) loadAllOrdersForAdmin()
            }
        }
    }

    fun resetUpdateOrderStatusState() {
        _updateOrderStatusState.value = null
    }

    fun resetCreateOrderState() {
        _createOrderState.value = null
    }

    /**
     * Called when user taps "I've Paid" on the EasyPaisa/JazzCash screen.
     * Marks the transaction as PROCESSING in Supabase.
     * No user input needed — admin verifies from their wallet app.
     */
    fun confirmMobilePayment(orderId: String) {
        viewModelScope.launch {
            _createOrderState.value = Resource.Loading()
            try {
                orderRepository.savePaymentDetails(orderId)
            } catch (_: Exception) {
                // Order is already saved — proceed regardless
            }
            _createOrderState.value = Resource.Success(Order(id = orderId))
        }
    }
}
