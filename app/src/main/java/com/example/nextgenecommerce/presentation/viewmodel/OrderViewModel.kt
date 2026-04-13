package com.example.nextgenecommerce.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.models.Order
import com.example.nextgenecommerce.data.models.OrderStatus
import com.example.nextgenecommerce.data.remote.CreateOrderRequest
import com.example.nextgenecommerce.data.repository.OrderRepository
import com.example.nextgenecommerce.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _selectedOrder = MutableStateFlow<Order?>(null)
    val selectedOrder: StateFlow<Order?> = _selectedOrder.asStateFlow()

    private val _createOrderState = MutableStateFlow<Resource<Order>?>(null)
    val createOrderState: StateFlow<Resource<Order>?> = _createOrderState.asStateFlow()

    fun loadOrders(userId: String) {
        viewModelScope.launch {
            orderRepository.getUserOrders(userId).collect {
                _orders.value = it
            }
        }
    }

    fun loadOrderById(orderId: String) {
        viewModelScope.launch {
            orderRepository.getOrderById(orderId).collect {
                _selectedOrder.value = it
            }
        }
    }

    fun createOrder(request: CreateOrderRequest) {
        viewModelScope.launch {
            orderRepository.createOrder(request).collect {
                _createOrderState.value = it
            }
        }
    }

    fun getOrdersByStatus(status: OrderStatus) {
        viewModelScope.launch {
            orderRepository.getOrdersByStatus(status).collect {
                _orders.value = it
            }
        }
    }

    fun syncOrders(userId: String) {
        viewModelScope.launch {
            orderRepository.syncOrders(userId).collect {
                // Orders synced
            }
        }
    }

    fun resetCreateOrderState() {
        _createOrderState.value = null
    }
}
