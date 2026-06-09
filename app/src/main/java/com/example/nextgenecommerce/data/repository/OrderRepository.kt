package com.example.nextgenecommerce.data.repository

import com.example.nextgenecommerce.data.local.dao.OrderDao
import com.example.nextgenecommerce.data.models.Order
import com.example.nextgenecommerce.data.models.OrderItem
import com.example.nextgenecommerce.data.models.OrderStatus
import com.example.nextgenecommerce.data.models.PaymentMethod
import com.example.nextgenecommerce.data.models.PaymentStatus
import com.example.nextgenecommerce.data.remote.ApiService
import com.example.nextgenecommerce.data.remote.CreateOrderRequest
import com.example.nextgenecommerce.util.Resource
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val orderDao: OrderDao,
    private val supabaseAuth: Auth,
    private val supabaseDb: Postgrest,
    private val apiService: ApiService
) {

    fun getAllOrders(): Flow<List<Order>> = orderDao.getAllOrders()
    fun getOrderById(orderId: String): Flow<Order?> = orderDao.getOrderById(orderId)
    fun getUserOrders(userId: String): Flow<List<Order>> = orderDao.getOrdersByUserId(userId)
    fun getOrdersByStatus(status: OrderStatus): Flow<List<Order>> = orderDao.getOrdersByStatus(status)

    suspend fun createOrder(request: CreateOrderRequest): Flow<Resource<Order>> = flow {
        emit(Resource.Loading())
        try {
            val userId = supabaseAuth.currentUserOrNull()?.id
                ?: throw Exception("User not logged in")
            val accessToken = supabaseAuth.currentAccessTokenOrNull()
                ?: throw Exception("Session expired. Please log in again.")

            val response = apiService.createOrder(
                authorization = "Bearer $accessToken",
                request = request.copy(userId = userId)
            )

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                throw Exception(errorBody ?: "Failed to place order")
            }

            val createdOrder = response.body()?.order
                ?: throw Exception("Server did not return an order")

            orderDao.insertOrder(createdOrder)
            emit(Resource.Success(createdOrder))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to place order"))
        }
    }

    suspend fun insertOrder(order: Order) = orderDao.insertOrder(order)
    suspend fun updateOrder(order: Order) = orderDao.updateOrder(order)

    /**
     * Save EasyPaisa/JazzCash mobile number and transaction ID to Supabase.
     * Order stays PENDING until admin verifies the payment in their wallet app.
     */
    /**
     * Mark the payment transaction as PROCESSING (user confirmed they've paid).
     * Admin verifies by checking their EasyPaisa/JazzCash app.
     */
    suspend fun savePaymentDetails(orderId: String) {
        supabaseDb.from("payment_transactions")
            .update(PaymentDetailsUpdate(status = "PROCESSING")) {
                filter { eq("order_id", orderId) }
            }
    }

    suspend fun requestReturn(order_id: String, reason: String, images: List<String> = emptyList()): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // 1. Try to update Supabase.
            try {
                supabaseDb.from("orders").update(
                    ReturnRequestWithImagesUpdate(
                        status = "RETURN_REQUESTED", 
                        returnReason = reason,
                        returnImages = images
                    )
                ) { filter { eq("id", order_id) } }
            } catch (e: Exception) {
                // Fallback to status only if columns are missing
                try {
                    supabaseDb.from("orders").update(
                        OrderStatusUpdate(status = "RETURN_REQUESTED")
                    ) { filter { eq("id", order_id) } }
                } catch (e2: Exception) {
                    // Log or handle constraint error
                }
            }

            // 2. Always persist locally
            val existing = orderDao.getOrderByIdOnce(order_id)
            if (existing != null) {
                orderDao.updateOrder(
                    existing.copy(
                        status = OrderStatus.RETURN_REQUESTED,
                        returnReason = reason,
                        returnImages = images,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to submit return request"))
        }
    }

    suspend fun approveReturn(orderId: String, note: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            try {
                supabaseDb.from("orders").update(
                    AcceptReturnUpdate(status = "RETURN_APPROVED", adminReturnNote = note)
                ) { filter { eq("id", orderId) } }
            } catch (_: Exception) {
                supabaseDb.from("orders").update(
                    OrderStatusUpdate(status = "RETURN_APPROVED")
                ) { filter { eq("id", orderId) } }
            }
            val existing = orderDao.getOrderByIdOnce(orderId)
            if (existing != null) {
                orderDao.updateOrder(
                    existing.copy(
                        status = OrderStatus.RETURN_APPROVED,
                        adminReturnNote = note,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to approve return"))
        }
    }

    // Kept for backward compat with admin return flow
    suspend fun acceptReturn(orderId: String, adminNote: String): Flow<Resource<Unit>> =
        approveReturn(orderId, adminNote)

    suspend fun markReturnPickedUp(orderId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            supabaseDb.from("orders").update(
                OrderStatusUpdate(status = "RETURN_IN_TRANSIT")
            ) { filter { eq("id", orderId) } }
            val existing = orderDao.getOrderByIdOnce(orderId)
            if (existing != null) {
                orderDao.updateOrder(existing.copy(status = OrderStatus.RETURN_IN_TRANSIT, updatedAt = System.currentTimeMillis()))
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to mark return as picked up"))
        }
    }

    suspend fun markReturnDeliveredToRetailer(orderId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            supabaseDb.from("orders").update(
                OrderStatusUpdate(status = "RETURN_RECEIVED")
            ) { filter { eq("id", orderId) } }
            val existing = orderDao.getOrderByIdOnce(orderId)
            if (existing != null) {
                orderDao.updateOrder(existing.copy(status = OrderStatus.RETURN_RECEIVED, updatedAt = System.currentTimeMillis()))
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to mark return as delivered to retailer"))
        }
    }

    suspend fun verifyAndCompleteReturn(orderId: String, retailerId: String, orderTotal: Double): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // 1. Mark order as fully returned and refunded
            supabaseDb.from("orders").update(
                VerifyReturnUpdate(status = "RETURNED", paymentStatus = "REFUNDED")
            ) { filter { eq("id", orderId) } }
            // 2. Deduct from retailer revenue (read-then-write)
            try {
                val rows = supabaseDb.from("retailers")
                    .select { filter { eq("id", retailerId) }; limit(1) }
                    .decodeList<RetailerRevenueRow>()
                val current = rows.firstOrNull()?.totalRevenue ?: 0.0
                val updated = (current - orderTotal).coerceAtLeast(0.0)
                supabaseDb.from("retailers").update(
                    RetailerRevenueUpdate(totalRevenue = updated)
                ) { filter { eq("id", retailerId) } }
            } catch (_: Exception) { /* revenue deduction best-effort */ }
            // 3. Update Room
            val existing = orderDao.getOrderByIdOnce(orderId)
            if (existing != null) {
                orderDao.updateOrder(
                    existing.copy(
                        status = OrderStatus.RETURNED,
                        paymentStatus = PaymentStatus.REFUNDED,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to complete return and refund"))
        }
    }

    suspend fun rejectReturn(orderId: String, adminNote: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            try {
                supabaseDb.from("orders").update(
                    RejectReturnUpdate(status = "RETURN_REJECTED", adminReturnNote = adminNote)
                ) { filter { eq("id", orderId) } }
            } catch (_: Exception) {
                supabaseDb.from("orders").update(
                    OrderStatusUpdate(status = "RETURN_REJECTED")
                ) { filter { eq("id", orderId) } }
            }
            val existing = orderDao.getOrderByIdOnce(orderId)
            if (existing != null) {
                orderDao.updateOrder(
                    existing.copy(
                        status = OrderStatus.RETURN_REJECTED,
                        adminReturnNote = adminNote,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to reject return"))
        }
    }

    suspend fun processReturnRefund(orderId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            try {
                supabaseDb.from("payment_transactions").update(
                    PaymentDetailsUpdate(status = "REFUNDED")
                ) { filter { eq("order_id", orderId) } }
            } catch (_: Exception) { /* table may not exist */ }
            supabaseDb.from("orders").update(
                PaymentStatusUpdate(paymentStatus = "REFUNDED")
            ) { filter { eq("id", orderId) } }
            val existing = orderDao.getOrderByIdOnce(orderId)
            if (existing != null) {
                orderDao.updateOrder(
                    existing.copy(
                        paymentStatus = PaymentStatus.REFUNDED,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to process refund"))
        }
    }

    suspend fun cancelOrder(orderId: String, reason: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // Update status only first — works even if cancellation_reason column is missing
            supabaseDb.from("orders").update(
                OrderStatusUpdate(status = "CANCELLED")
            ) {
                filter { eq("id", orderId) }
            }

            // Try to persist the reason in Supabase — silently skip if column doesn't exist yet
            try {
                supabaseDb.from("orders").update(
                    CancelOrderUpdate(status = "CANCELLED", cancellationReason = reason)
                ) {
                    filter { eq("id", orderId) }
                }
            } catch (_: Exception) { /* column may not exist in Supabase yet — reason stored locally */ }

            // Always update Room with both status and reason
            val existing = orderDao.getOrderByIdOnce(orderId)
            if (existing != null) {
                orderDao.updateOrder(
                    existing.copy(
                        status = OrderStatus.CANCELLED,
                        cancellationReason = reason,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to cancel order"))
        }
    }

    suspend fun getAllOrdersAdmin(): Flow<Resource<List<Order>>> = flow {
        emit(Resource.Loading())
        try {
            val rows = supabaseDb.from("orders")
                .select(Columns.raw("*, order_items(*), shipping_addresses!shipping_address_id(*)"))
                .decodeList<SupabaseOrderRow>()
            val orders = rows.map { it.toOrder() }
            orders.forEach { orderDao.insertOrder(it) }
            emit(Resource.Success(orders))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load orders"))
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            supabaseDb.from("orders").update(
                OrderStatusUpdate(status = status.name)
            ) {
                filter { eq("id", orderId) }
            }
            val existing = orderDao.getOrderByIdOnce(orderId)
            if (existing != null) {
                orderDao.updateOrder(existing.copy(status = status, updatedAt = System.currentTimeMillis()))
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update order status"))
        }
    }

    suspend fun syncOrders(userId: String): Flow<Resource<List<Order>>> = flow {
        emit(Resource.Loading())
        try {
            val rows = supabaseDb.from("orders")
                .select(Columns.raw("*, order_items(*), shipping_addresses!shipping_address_id(*)")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<SupabaseOrderRow>()

            val orders = rows.map { it.toOrder() }
            orders.forEach { remote ->
                val existing = orderDao.getOrderByIdOnce(remote.id)
                val merged = if (existing != null) {
                    // Preserve local user-initiated status changes that may not have synced to Supabase yet.
                    // RETURN_REQUESTED and CANCELLED are set on-device; if Supabase still shows an earlier
                    // status, the local value is more up-to-date.
                    val returnStatuses = setOf(
                        OrderStatus.RETURN_REQUESTED, OrderStatus.RETURN_APPROVED,
                        OrderStatus.RETURN_IN_TRANSIT, OrderStatus.RETURN_RECEIVED,
                        OrderStatus.RETURNED, OrderStatus.RETURN_REJECTED
                    )
                    val keepLocalStatus =
                        (existing.status in returnStatuses && remote.status == OrderStatus.DELIVERED) ||
                        (existing.status == OrderStatus.CANCELLED && remote.status != OrderStatus.CANCELLED)
                    remote.copy(
                        status = if (keepLocalStatus) existing.status else remote.status,
                        returnReason = remote.returnReason ?: existing.returnReason,
                        returnImages = if (remote.returnImages.isEmpty()) existing.returnImages else remote.returnImages,
                        cancellationReason = remote.cancellationReason ?: existing.cancellationReason,
                        adminReturnNote = remote.adminReturnNote ?: existing.adminReturnNote
                    )
                } else remote
                orderDao.insertOrder(merged)
            }
            emit(Resource.Success(orders))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Sync failed"))
        }
    }
}

// ── Update payloads ───────────────────────────────────────────────────────────

@Serializable
private data class ReturnRequestWithImagesUpdate(
    @SerialName("status") val status: String,
    @SerialName("return_reason") val returnReason: String,
    @SerialName("return_images") val returnImages: List<String>
)

@Serializable
private data class ReturnRequestUpdate(
    @SerialName("status") val status: String,
    @SerialName("return_reason") val returnReason: String
)

@Serializable
private data class AcceptReturnUpdate(
    @SerialName("status") val status: String,
    @SerialName("admin_return_note") val adminReturnNote: String
)

@Serializable
private data class VerifyReturnUpdate(
    @SerialName("status") val status: String,
    @SerialName("payment_status") val paymentStatus: String
)

@Serializable
private data class RetailerRevenueUpdate(
    @SerialName("total_revenue") val totalRevenue: Double
)

@Serializable
private data class RetailerRevenueRow(
    @SerialName("total_revenue") val totalRevenue: Double = 0.0
)

@Serializable
private data class RejectReturnUpdate(
    @SerialName("status") val status: String,
    @SerialName("admin_return_note") val adminReturnNote: String
)

@Serializable
private data class PaymentStatusUpdate(
    @SerialName("payment_status") val paymentStatus: String
)

@Serializable
private data class PaymentDetailsUpdate(
    @SerialName("status") val status: String
)

@Serializable
private data class CancelOrderUpdate(
    @SerialName("status") val status: String,
    @SerialName("cancellation_reason") val cancellationReason: String
)

@Serializable
private data class OrderStatusUpdate(
    @SerialName("status") val status: String
)

// ── Insert payloads (all @Serializable to avoid Map<String, Any> error) ──────

@Serializable
private data class OrderInsert(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("order_number") val orderNumber: String,
    @SerialName("subtotal") val subtotal: Double,
    @SerialName("tax") val tax: Double,
    @SerialName("shipping") val shipping: Double,
    @SerialName("total") val total: Double,
    @SerialName("currency") val currency: String,
    @SerialName("status") val status: String,
    @SerialName("payment_status") val paymentStatus: String,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("shipping_address_id") val shippingAddressId: String?,
    @SerialName("retailer_id") val retailerId: String? = null,
    @SerialName("parent_order_id") val parentOrderId: String? = null
)

@Serializable
private data class OrderItemInsert(
    @SerialName("order_id") val orderId: String,
    @SerialName("product_id") val productId: String,
    @SerialName("product_name") val productName: String,
    @SerialName("product_image") val productImage: String,
    @SerialName("price") val price: Double,
    @SerialName("quantity") val quantity: Int,
    @SerialName("selected_size") val selectedSize: String,
    @SerialName("selected_color") val selectedColor: String
)

@Serializable
private data class PaymentTransactionInsert(
    @SerialName("order_id") val orderId: String,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("status") val status: String,
    @SerialName("amount") val amount: Double,
    @SerialName("currency") val currency: String,
    @SerialName("otp_verified") val otpVerified: Boolean
)

// ── Supabase read helpers ─────────────────────────────────────────────────────

@Serializable
private data class SupabaseAddressRow(
    @SerialName("id")            val id: String = "",
    @SerialName("user_id")       val userId: String = "",
    @SerialName("label")         val label: String = "",
    @SerialName("full_name")     val fullName: String = "",
    @SerialName("phone")         val phone: String = "",
    @SerialName("address_line1") val addressLine1: String = "",
    @SerialName("address_line2") val addressLine2: String = "",
    @SerialName("city")          val city: String = "",
    @SerialName("province")      val province: String = "",
    @SerialName("postal_code")   val postalCode: String = "",
    @SerialName("country")       val country: String = "Pakistan",
    @SerialName("is_default")    val isDefault: Boolean = false
) {
    fun toAddress() = com.example.nextgenecommerce.data.models.Address(
        id = id, userId = userId, label = label, fullName = fullName,
        phone = phone, addressLine1 = addressLine1, addressLine2 = addressLine2,
        city = city, province = province, postalCode = postalCode, country = country
    )
}

@Serializable
private data class SupabaseOrderRow(
    @SerialName("id") val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("order_number") val orderNumber: String = "",
    @SerialName("subtotal") val subtotal: Double = 0.0,
    @SerialName("tax") val tax: Double = 0.0,
    @SerialName("shipping") val shipping: Double = 0.0,
    @SerialName("total") val total: Double = 0.0,
    @SerialName("currency") val currency: String = "PKR",
    @SerialName("status") val status: String = "PENDING",
    @SerialName("payment_status") val paymentStatus: String = "PENDING",
    @SerialName("payment_method") val paymentMethod: String = "SAFEPAY",
    @SerialName("retailer_id") val retailerId: String? = null,
    @SerialName("delivery_partner_id") val deliveryPartnerId: String? = null,
    @SerialName("parent_order_id") val parentOrderId: String? = null,
    @SerialName("cancellation_reason") val cancellationReason: String? = null,
    @SerialName("return_reason") val returnReason: String? = null,
    @SerialName("return_images") val returnImages: List<String> = emptyList(),
    @SerialName("admin_return_note") val adminReturnNote: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("order_items") val orderItems: List<SupabaseOrderItemRow> = emptyList(),
    @SerialName("shipping_addresses") val shippingAddress: SupabaseAddressRow? = null
) {
    fun toOrder() = Order(
        id = id,
        userId = userId,
        orderNumber = orderNumber,
        items = orderItems.map { it.toOrderItem() },
        subtotal = subtotal,
        tax = tax,
        shipping = shipping,
        total = total,
        currency = currency,
        status = runCatching { OrderStatus.valueOf(status) }.getOrDefault(OrderStatus.PENDING),
        paymentStatus = runCatching { PaymentStatus.valueOf(paymentStatus) }.getOrDefault(PaymentStatus.PENDING),
        paymentMethod = runCatching { PaymentMethod.valueOf(paymentMethod) }.getOrDefault(PaymentMethod.CASH_ON_DELIVERY),
        retailerId = retailerId,
        deliveryPartnerId = deliveryPartnerId,
        parentOrderId = parentOrderId,
        cancellationReason = cancellationReason,
        returnReason = returnReason,
        returnImages = returnImages,
        adminReturnNote = adminReturnNote,
        shippingAddress = shippingAddress?.toAddress(),
        createdAt = runCatching {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                .parse(createdAt)?.time ?: System.currentTimeMillis()
        }.getOrDefault(System.currentTimeMillis()),
        updatedAt = System.currentTimeMillis()
    )
}

@Serializable
private data class SupabaseOrderItemRow(
    @SerialName("product_id") val productId: String = "",
    @SerialName("product_name") val productName: String = "",
    @SerialName("product_image") val productImage: String = "",
    @SerialName("price") val price: Double = 0.0,
    @SerialName("quantity") val quantity: Int = 1,
    @SerialName("selected_size") val selectedSize: String = "",
    @SerialName("selected_color") val selectedColor: String = ""
) {
    fun toOrderItem() = OrderItem(
        productId = productId,
        productName = productName,
        productImage = productImage,
        price = price,
        quantity = quantity,
        selectedSize = selectedSize,
        selectedColor = selectedColor
    )
}
