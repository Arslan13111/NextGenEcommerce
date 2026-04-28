package com.example.nextgenecommerce.data.repository

import com.example.nextgenecommerce.data.local.dao.OrderDao
import com.example.nextgenecommerce.data.models.Order
import com.example.nextgenecommerce.data.models.OrderItem
import com.example.nextgenecommerce.data.models.OrderStatus
import com.example.nextgenecommerce.data.models.PaymentMethod
import com.example.nextgenecommerce.data.models.PaymentStatus
import com.example.nextgenecommerce.data.remote.CreateOrderRequest
import com.example.nextgenecommerce.util.Resource
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val orderDao: OrderDao,
    private val supabaseAuth: Auth,
    private val supabaseDb: Postgrest
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

            val totalSubtotal = request.items.sumOf { it.price * it.quantity }
            val totalShipping = if (totalSubtotal > 5000.0) 0.0 else 200.0
            val totalDiscount = request.discountAmount.coerceAtLeast(0.0)
            
            val orderInserts = mutableListOf<OrderInsert>()
            val itemInserts = mutableListOf<OrderItemInsert>()
            val txnInserts = mutableListOf<PaymentTransactionInsert>()
            val roomOrders = mutableListOf<Order>()
            
            val now = System.currentTimeMillis()

            request.items.forEachIndexed { index, item ->
                val itemSubtotal = item.price * item.quantity
                val itemTax = itemSubtotal * 0.08
                
                // Distribute shipping and discount proportionally based on item's share of total subtotal
                val weight = if (totalSubtotal > 0) itemSubtotal / totalSubtotal else 1.0 / request.items.size
                val itemShipping = totalShipping * weight
                val itemDiscount = totalDiscount * weight
                
                val itemTotal = (itemSubtotal + itemTax + itemShipping - itemDiscount).coerceAtLeast(0.0)
                
                val orderId = UUID.randomUUID().toString()
                // Append index to ensure unique order numbers even if processed within the same millisecond
                val orderNumber = "ORD-$now-${index + 1}"

                orderInserts.add(OrderInsert(
                    id = orderId,
                    userId = userId,
                    orderNumber = orderNumber,
                    subtotal = itemSubtotal,
                    tax = itemTax,
                    shipping = itemShipping,
                    total = itemTotal,
                    currency = "PKR",
                    status = "PENDING",
                    paymentStatus = "PENDING",
                    paymentMethod = request.paymentMethod,
                    shippingAddressId = null
                ))

                itemInserts.add(OrderItemInsert(
                    orderId = orderId,
                    productId = item.productId,
                    productName = item.productName,
                    productImage = item.productImage,
                    price = item.price,
                    quantity = item.quantity,
                    selectedSize = item.selectedSize,
                    selectedColor = item.selectedColor
                ))

                txnInserts.add(PaymentTransactionInsert(
                    orderId = orderId,
                    paymentMethod = request.paymentMethod,
                    transactionId = "TXN-$orderId",
                    status = "PENDING",
                    amount = itemTotal,
                    currency = "PKR",
                    otpVerified = false
                ))

                roomOrders.add(Order(
                    id = orderId,
                    userId = userId,
                    orderNumber = orderNumber,
                    items = listOf(item),
                    subtotal = itemSubtotal,
                    tax = itemTax,
                    shipping = itemShipping,
                    total = itemTotal,
                    currency = "PKR",
                    status = OrderStatus.PENDING,
                    paymentStatus = PaymentStatus.PENDING,
                    paymentMethod = runCatching { PaymentMethod.valueOf(request.paymentMethod) }.getOrDefault(PaymentMethod.CASH_ON_DELIVERY),
                    shippingAddress = request.shippingAddress,
                    createdAt = now,
                    updatedAt = now
                ))
            }

            // 1. Batch insert into Supabase
            supabaseDb.from("orders").insert(orderInserts)
            try {
                supabaseDb.from("order_items").insert(itemInserts)
            } catch (_: Exception) {}
            try {
                supabaseDb.from("payment_transactions").insert(txnInserts)
            } catch (_: Exception) {}

            // 2. Save all to Room
            roomOrders.forEach { orderDao.insertOrder(it) }

            if (roomOrders.isNotEmpty()) {
                // Return the first order to the UI for navigation and success display
                emit(Resource.Success(roomOrders.first()))
            } else {
                emit(Resource.Error("No items in order"))
            }
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

    suspend fun acceptReturn(orderId: String, adminNote: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            try {
                supabaseDb.from("orders").update(
                    AcceptReturnUpdate(status = "RETURNED", adminReturnNote = adminNote)
                ) { filter { eq("id", orderId) } }
            } catch (_: Exception) {
                supabaseDb.from("orders").update(
                    OrderStatusUpdate(status = "RETURNED")
                ) { filter { eq("id", orderId) } }
            }
            val existing = orderDao.getOrderByIdOnce(orderId)
            if (existing != null) {
                orderDao.updateOrder(
                    existing.copy(
                        status = OrderStatus.RETURNED,
                        adminReturnNote = adminNote,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to accept return"))
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
                .select(Columns.raw("*, order_items(*)"))
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
                .select(Columns.raw("*, order_items(*)")) {
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
                    val keepLocalStatus =
                        (existing.status == OrderStatus.RETURN_REQUESTED &&
                            remote.status != OrderStatus.RETURN_REQUESTED &&
                            remote.status != OrderStatus.RETURNED) ||
                        (existing.status == OrderStatus.CANCELLED &&
                            remote.status != OrderStatus.CANCELLED)
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
    @SerialName("shipping_address_id") val shippingAddressId: String?
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
    @SerialName("cancellation_reason") val cancellationReason: String? = null,
    @SerialName("return_reason") val returnReason: String? = null,
    @SerialName("return_images") val returnImages: List<String> = emptyList(),
    @SerialName("admin_return_note") val adminReturnNote: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("order_items") val orderItems: List<SupabaseOrderItemRow> = emptyList()
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
        cancellationReason = cancellationReason,
        returnReason = returnReason,
        returnImages = returnImages,
        adminReturnNote = adminReturnNote,
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
