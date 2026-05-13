package com.example.nextgenecommerce.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.models.Order
import com.example.nextgenecommerce.data.models.OrderStatus
import com.example.nextgenecommerce.data.models.Retailer
import com.example.nextgenecommerce.data.models.SupabaseProduct
import com.example.nextgenecommerce.data.repository.OrderRepository
import com.example.nextgenecommerce.data.repository.RetailerRepository
import com.example.nextgenecommerce.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RetailerViewModel @Inject constructor(
    private val retailerRepository: RetailerRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _storeState = MutableStateFlow<Resource<Retailer>?>(null)
    val storeState: StateFlow<Resource<Retailer>?> = _storeState.asStateFlow()

    private val _store = MutableStateFlow<Retailer?>(null)
    val store: StateFlow<Retailer?> = _store.asStateFlow()

    private val _products = MutableStateFlow<Resource<List<SupabaseProduct>>>(Resource.Loading())
    val products: StateFlow<Resource<List<SupabaseProduct>>> = _products.asStateFlow()

    private val _orders = MutableStateFlow<Resource<List<Order>>>(Resource.Loading())
    val orders: StateFlow<Resource<List<Order>>> = _orders.asStateFlow()

    private val _actionState = MutableStateFlow<Resource<Unit>?>(null)
    val actionState: StateFlow<Resource<Unit>?> = _actionState.asStateFlow()

    init {
        loadStore()
    }

    fun loadStore() {
        viewModelScope.launch {
            retailerRepository.getMyStore().collect { result ->
                _storeState.value = result
                if (result is Resource.Success) {
                    _store.value = result.data
                    result.data?.id?.let { storeId ->
                        loadProducts(storeId)
                        loadOrders(storeId)
                    }
                }
            }
        }
    }

    fun createStore(name: String, description: String, phone: String, address: String) {
        viewModelScope.launch {
            retailerRepository.createStore(name, description, phone, address).collect { result ->
                _storeState.value = result
                if (result is Resource.Success) {
                    _store.value = result.data
                    result.data?.id?.let { storeId ->
                        loadProducts(storeId)
                        loadOrders(storeId)
                    }
                }
            }
        }
    }

    fun updateStore(name: String, description: String, phone: String, address: String) {
        val currentStore = _store.value ?: return
        viewModelScope.launch {
            val updated = currentStore.copy(
                storeName = name,
                storeDescription = description,
                contactPhone = phone,
                storeAddress = address
            )
            retailerRepository.updateStore(updated).collect { result ->
                _actionState.value = result.map { Unit }
                if (result is Resource.Success) {
                    _store.value = result.data
                }
            }
        }
    }

    fun updateStoreLogo(logoUrl: String) {
        val currentStore = _store.value ?: return
        viewModelScope.launch {
            val updated = currentStore.copy(storeLogoUrl = logoUrl)
            retailerRepository.updateStore(updated).collect { result ->
                _actionState.value = result.map { Unit }
                if (result is Resource.Success) {
                    _store.value = result.data
                }
            }
        }
    }

    private fun loadProducts(storeId: String) {
        viewModelScope.launch {
            retailerRepository.getMyProducts(storeId).collect { _products.value = it }
        }
    }

    private fun loadOrders(storeId: String) {
        viewModelScope.launch {
            retailerRepository.getMyOrders(storeId).collect { _orders.value = it }
        }
    }

    fun refreshOrders() {
        _store.value?.id?.let { loadOrders(it) }
    }

    fun refreshProducts() {
        _store.value?.id?.let { loadProducts(it) }
    }

    fun addProduct(product: SupabaseProduct) {
        viewModelScope.launch {
            retailerRepository.addProduct(product).collect { result ->
                _actionState.value = result
                if (result is Resource.Success) refreshProducts()
            }
        }
    }

    fun updateProduct(product: SupabaseProduct) {
        viewModelScope.launch {
            retailerRepository.updateProduct(product).collect { result ->
                _actionState.value = result
                if (result is Resource.Success) refreshProducts()
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            retailerRepository.deleteProduct(productId).collect { result ->
                _actionState.value = result
                if (result is Resource.Success) refreshProducts()
            }
        }
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        viewModelScope.launch {
            retailerRepository.updateOrderStatus(orderId, status).collect { result ->
                _actionState.value = result
                if (result is Resource.Success) refreshOrders()
            }
        }
    }

    fun acceptReturn(orderId: String, note: String) {
        viewModelScope.launch {
            orderRepository.acceptReturn(orderId, note).collect { result ->
                _actionState.value = result
                if (result is Resource.Success) refreshOrders()
            }
        }
    }

    fun rejectReturn(orderId: String, note: String) {
        viewModelScope.launch {
            orderRepository.rejectReturn(orderId, note).collect { result ->
                _actionState.value = result
                if (result is Resource.Success) refreshOrders()
            }
        }
    }

    fun resetActionState() {
        _actionState.value = null
    }
}

private fun <T, R> Resource<T>.map(transform: (T) -> R): Resource<R> = when (this) {
    is Resource.Success -> Resource.Success(transform(data!!))
    is Resource.Error   -> Resource.Error(message ?: "Error")
    is Resource.Loading -> Resource.Loading()
}
