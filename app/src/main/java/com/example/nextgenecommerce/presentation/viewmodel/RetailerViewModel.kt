package com.example.nextgenecommerce.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.data.models.Order
import com.example.nextgenecommerce.data.models.OrderStatus
import com.example.nextgenecommerce.data.models.Retailer
import com.example.nextgenecommerce.data.models.SupabaseProduct
import com.example.nextgenecommerce.data.config.SupabaseConfig
import com.example.nextgenecommerce.data.repository.AdminVaultRepository
import com.example.nextgenecommerce.data.repository.OrderRepository
import com.example.nextgenecommerce.data.repository.ProductRepository
import com.example.nextgenecommerce.data.repository.PushNotificationRepository
import com.example.nextgenecommerce.data.repository.RetailerRepository
import com.example.nextgenecommerce.data.repository.StorageRepository
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
    private val storageRepository: StorageRepository,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val pushNotificationRepository: PushNotificationRepository,
    private val adminVaultRepository: AdminVaultRepository
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

    fun createStore(name: String, description: String, phone: String, address: String, imageUris: List<Uri> = emptyList()) {
        viewModelScope.launch {
            val userId = retailerRepository.getCurrentUserId() ?: ""
            val urls = uploadImages(userId, imageUris)
            retailerRepository.createStore(name, description, phone, address, urls).collect { result ->
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

    private suspend fun uploadImages(userId: String, uris: List<Uri>): List<String> {
        val urls = mutableListOf<String>()
        for (uri in uris) {
            storageRepository.uploadProfileImage(userId, uri).collect { result ->
                if (result is Resource.Success) result.data?.let { urls.add(it) }
            }
        }
        return urls
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
                if (result is Resource.Success) {
                    refreshProducts()
                    sendNewProductNotification(product.name)
                }
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
        val retailerId = _store.value?.id ?: return
        viewModelScope.launch {
            retailerRepository.removeProductFromStore(productId, retailerId).collect { result ->
                _actionState.value = result
                if (result is Resource.Success) {
                    productRepository.deleteProductById(productId)
                    refreshProducts()
                }
            }
        }
    }

    fun adminDeleteProduct(productId: String) {
        viewModelScope.launch {
            retailerRepository.adminDeleteProduct(productId).collect { result ->
                _actionState.value = result
                if (result is Resource.Success) {
                    productRepository.deleteProductById(productId)
                }
            }
        }
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        val order = (_orders.value as? Resource.Success)?.data?.find { it.id == orderId }
        viewModelScope.launch {
            if (status == OrderStatus.DELIVERED) {
                order?.let { adminVaultRepository.recordCommissionForOrder(it.id, it.subtotal) }
            }
            retailerRepository.updateOrderStatus(orderId, status).collect { result ->
                _actionState.value = result
                if (result is Resource.Success) {
                    refreshOrders()
                    order?.let { sendOrderStatusNotification(it, status) }
                }
            }
        }
    }

    fun approveReturnRequest(orderId: String, note: String) {
        val order = (_orders.value as? Resource.Success)?.data?.find { it.id == orderId }
        viewModelScope.launch {
            orderRepository.approveReturn(orderId, note).collect { result ->
                _actionState.value = result
                if (result is Resource.Success) {
                    refreshOrders()
                    order?.let { sendOrderStatusNotification(it, OrderStatus.RETURN_APPROVED) }
                }
            }
        }
    }

    fun rejectReturn(orderId: String, note: String) {
        val order = (_orders.value as? Resource.Success)?.data?.find { it.id == orderId }
        viewModelScope.launch {
            orderRepository.rejectReturn(orderId, note).collect { result ->
                _actionState.value = result
                if (result is Resource.Success) {
                    refreshOrders()
                    order?.let { sendOrderStatusNotification(it, OrderStatus.RETURN_REJECTED) }
                }
            }
        }
    }

    fun verifyAndRefund(orderId: String) {
        val order = (_orders.value as? Resource.Success)?.data?.find { it.id == orderId }
        val retailerId = _store.value?.id ?: return
        viewModelScope.launch {
            orderRepository.verifyAndCompleteReturn(orderId, retailerId, order?.total ?: 0.0).collect { result ->
                _actionState.value = result
                if (result is Resource.Success) {
                    refreshOrders()
                    order?.let { sendOrderStatusNotification(it, OrderStatus.RETURNED) }
                }
            }
        }
    }

    fun resetActionState() {
        _actionState.value = null
    }

    // ── Admin: view products per retailer ─────────────────────────────────────

    private val _adminRetailerProducts = MutableStateFlow<Resource<List<SupabaseProduct>>>(Resource.Loading())
    val adminRetailerProducts: StateFlow<Resource<List<SupabaseProduct>>> = _adminRetailerProducts.asStateFlow()

    fun loadProductsForRetailer(retailerId: String) {
        viewModelScope.launch {
            retailerRepository.getProductsForAdmin(retailerId).collect {
                _adminRetailerProducts.value = it
            }
        }
    }

    private fun sendOrderStatusNotification(order: Order, newStatus: OrderStatus) {
        val storeName = _store.value?.storeName ?: "your retailer"
        val senderId = SupabaseConfig.auth.currentUserOrNull()?.id
            ?: _store.value?.userId
            ?: return

        val productSummary = when {
            order.items.isEmpty() -> "your order"
            order.items.size == 1 -> "\"${order.items[0].productName}\""
            order.items.size == 2 -> "\"${order.items[0].productName}\" and \"${order.items[1].productName}\""
            else -> "\"${order.items[0].productName}\" and ${order.items.size - 1} more item(s)"
        }

        val (title, body) = when (newStatus) {
            OrderStatus.CONFIRMED ->
                "Order Confirmed ✅" to
                "Dear Customer, your order #${order.orderNumber} for $productSummary has been confirmed by $storeName and is now being prepared."

            OrderStatus.PROCESSING ->
                "Order Being Processed 🔄" to
                "Dear Customer, your order #${order.orderNumber} for $productSummary is currently being processed by $storeName."

            OrderStatus.PACKED ->
                "Order Packed 📦" to
                "Dear Customer, your order #${order.orderNumber} for $productSummary has been packed by $storeName and is ready for dispatch."

            OrderStatus.READY_FOR_PICKUP ->
                "Ready for Pickup 🚚" to
                "Dear Customer, your order #${order.orderNumber} for $productSummary is packed and waiting for the delivery partner to pick it up."

            OrderStatus.SHIPPED ->
                "Order Shipped 🚀" to
                "Dear Customer, your order #${order.orderNumber} for $productSummary has been shipped by $storeName and is on its way to you!"

            OrderStatus.OUT_FOR_DELIVERY ->
                "Out for Delivery 🛵" to
                "Dear Customer, your order #${order.orderNumber} for $productSummary is out for delivery and will reach you very soon!"

            OrderStatus.DELIVERED ->
                "Order Delivered 🎉" to
                "Dear Customer, your order #${order.orderNumber} for $productSummary has been successfully delivered. Enjoy your purchase!"

            OrderStatus.CANCELLED ->
                "Order Cancelled ❌" to
                "Dear Customer, unfortunately your order #${order.orderNumber} for $productSummary has been cancelled by $storeName. Please contact support if you have questions."

            OrderStatus.RETURN_APPROVED ->
                "Return Approved ✅" to
                "Dear Customer, your return request for order #${order.orderNumber} ($productSummary) has been approved by $storeName. A delivery partner will pick it up from you shortly."

            OrderStatus.RETURNED ->
                "Refund Processed ↩️" to
                "Dear Customer, your returned item for order #${order.orderNumber} ($productSummary) has been verified by $storeName and your refund has been processed."

            OrderStatus.RETURN_REJECTED ->
                "Return Rejected ⚠️" to
                "Dear Customer, your return request for order #${order.orderNumber} ($productSummary) has been reviewed and rejected by $storeName."

            else ->
                "Order #${order.orderNumber} Updated" to
                "Dear Customer, the status of your order #${order.orderNumber} for $productSummary has been updated by $storeName."
        }

        viewModelScope.launch {
            pushNotificationRepository.sendNotification(
                title = title,
                message = body,
                senderId = senderId,
                senderRole = "retailer",
                recipientRole = "customer",
                recipientId = order.userId.ifBlank { null },
                orderId = order.id.ifBlank { null }
            )
        }
    }

    private fun sendNewProductNotification(productName: String) {
        val storeName = _store.value?.storeName ?: "A store"
        val senderId = SupabaseConfig.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            pushNotificationRepository.sendNotification(
                title = "New Arrival!",
                message = "$storeName just listed \"$productName\". Check it out!",
                senderId = senderId,
                senderRole = "retailer",
                recipientRole = "customer"
            )
        }
    }

    // ── Admin: view all retailers + control approval ───────────────────────────

    private val _allRetailers = MutableStateFlow<Resource<List<Retailer>>>(Resource.Loading())
    val allRetailers: StateFlow<Resource<List<Retailer>>> = _allRetailers.asStateFlow()

    private val _retailerApprovalState = MutableStateFlow<Resource<Unit>?>(null)
    val retailerApprovalState: StateFlow<Resource<Unit>?> = _retailerApprovalState.asStateFlow()

    fun loadAllRetailersForAdmin() {
        viewModelScope.launch {
            retailerRepository.getAllRetailersForAdmin().collect { _allRetailers.value = it }
        }
    }

    fun setRetailerApproval(retailerId: String, approved: Boolean) {
        viewModelScope.launch {
            retailerRepository.setRetailerApproval(retailerId, approved).collect { result ->
                _retailerApprovalState.value = result
                if (result is Resource.Success) loadAllRetailersForAdmin()
            }
        }
    }

    fun resetRetailerApprovalState() {
        _retailerApprovalState.value = null
    }
}

private fun <T, R> Resource<T>.map(transform: (T) -> R): Resource<R> = when (this) {
    is Resource.Success -> Resource.Success(transform(data!!))
    is Resource.Error   -> Resource.Error(message ?: "Error")
    is Resource.Loading -> Resource.Loading()
}
