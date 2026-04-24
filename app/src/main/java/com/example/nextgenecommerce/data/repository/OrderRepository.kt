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

            val subtotal = request.items.sumOf { it.price * it.quantity }
            val tax = subtotal * 0.08
            val shipping = if (subtotal > 5000.0) 0.0 else 200.0
            val discount = request.discountAmount.coerceAtLeast(0.0)
            val total = (subtotal + tax + shipping - discount).coerceAtLeast(0.0)
            val orderId = UUID.randomUUID().toString()
            val orderNumber = "ORD-${System.currentTimeMillis()}"
            val now = System.currentTimeMillis()

            // Use @Serializable data class — avoids Map<String, Any> serialization error
            val orderInsert = OrderInsert(
                id = orderId,
                userId = userId,
                orderNumber = orderNumber,
                subtotal = subtotal,
                tax = tax,
                shipping = shipping,
                total = total,
                currency = "PKR",
                status = "PENDING",
                paymentStatus = "PENDING",
                paymentMethod = request.paymentMethod,
                shippingAddressId = null  // address stored locally; no FK dependency
            )
            supabaseDb.from("orders").insert(orderInsert)

            // Insert order items
            val itemInserts = request.items.map { item ->
                OrderItemInsert(
                    orderId = orderId,
                    productId = item.productId,
                    productName = item.productName,
                    productImage = item.productImage,
                    price = item.price,
                    quantity = item.quantity,
                    selectedSize = item.selectedSize,
                    selectedColor = item.selectedColor
                )
            }
            try {
                supabaseDb.from("order_items").insert(itemInserts)
            } catch (_: Exception) { /* saved locally below; Supabase sync can retry later */ }

            // Insert payment transaction — optional, don't fail the order if table missing
            try {
                val txnInsert = PaymentTransactionInsert(
                    orderId = orderId,
                    paymentMethod = request.paymentMethod,
                    transactionId = "TXN-$orderId",
                    status = "PENDING",
                    amount = total,
                    currency = "PKR",
                    otpVerified = false
                )
                supabaseDb.from("payment_transactions").insert(txnInsert)
            } catch (_: Exception) { /* table may not exist yet — order still succeeds */ }

            val order = Order(
                id = orderId,
                userId = userId,
                orderNumber = orderNumber,
                items = request.items,
                subtotal = subtotal,
                tax = tax,
                shipping = shipping,
                total = total,
                currency = "PKR",
                status = OrderStatus.PENDING,
                paymentStatus = PaymentStatus.PENDING,
                paymentMethod = PaymentMethod.valueOf(request.paymentMethod),
                shippingAddress = request.shippingAddress,
                createdAt = now,
                updatedAt = now
            )

            orderDao.insertOrder(order)
            emit(Resource.Success(order))
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

    suspend fun cancelOrder(orderId: String, reason: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            supabaseDb.from("orders").update(
                CancelOrderUpdate(status = "CANCELLED", cancellationReason = reason)
            ) {
                filter { eq("id", orderId) }
            }
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
            orders.forEach { orderDao.insertOrder(it) }
            emit(Resource.Success(orders))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Sync failed"))
        }
    }
}

// ── Update payloads ───────────────────────────────────────────────────────────

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
