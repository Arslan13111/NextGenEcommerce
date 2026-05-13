package com.example.nextgenecommerce.data.repository

import com.example.nextgenecommerce.data.models.Order
import com.example.nextgenecommerce.data.models.OrderStatus
import com.example.nextgenecommerce.data.models.PaymentMethod
import com.example.nextgenecommerce.data.models.PaymentStatus
import com.example.nextgenecommerce.data.models.Retailer
import com.example.nextgenecommerce.data.models.SupabaseProduct
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
class RetailerRepository @Inject constructor(
    private val supabaseAuth: Auth,
    private val supabaseDb: Postgrest
) {
    private fun uid() = supabaseAuth.currentUserOrNull()?.id
        ?: error("User not logged in")

    // ── Store profile ──────────────────────────────────────────────────────────

    fun getMyStore(): Flow<Resource<Retailer>> = flow {
        emit(Resource.Loading())
        try {
            val userId = uid()
            val row = supabaseDb.from("retailers")
                .select {
                    filter { eq("user_id", userId) }
                    limit(1)
                }
                .decodeList<Retailer>()
                .firstOrNull()
            if (row != null) emit(Resource.Success(row))
            else emit(Resource.Error("No store found. Please create your store first."))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load store"))
        }
    }

    fun createStore(storeName: String, description: String, phone: String, address: String): Flow<Resource<Retailer>> = flow {
        emit(Resource.Loading())
        try {
            val userId = uid()
            val insert = RetailerInsert(
                userId      = userId,
                storeName   = storeName,
                storeDescription = description,
                contactPhone = phone,
                storeAddress = address
            )
            supabaseDb.from("retailers").insert(insert)
            val created = supabaseDb.from("retailers")
                .select { filter { eq("user_id", userId) }; limit(1) }
                .decodeList<Retailer>()
                .first()
            emit(Resource.Success(created))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to create store"))
        }
    }

    fun updateStore(retailer: Retailer): Flow<Resource<Retailer>> = flow {
        emit(Resource.Loading())
        try {
            supabaseDb.from("retailers").update(
                RetailerUpdate(
                    storeName        = retailer.storeName,
                    storeDescription = retailer.storeDescription,
                    contactPhone     = retailer.contactPhone,
                    storeAddress     = retailer.storeAddress,
                    storeLogoUrl     = retailer.storeLogoUrl
                )
            ) { filter { eq("id", retailer.id) } }
            emit(Resource.Success(retailer))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update store"))
        }
    }

    // ── Public store discovery (no auth required) ──────────────────────────────

    fun getAllStores(): Flow<Resource<List<Retailer>>> = flow {
        emit(Resource.Loading())
        try {
            val stores = supabaseDb.from("retailers")
                .select()
                .decodeList<Retailer>()
            emit(Resource.Success(stores))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load stores"))
        }
    }

    fun getProductsByRetailer(retailerId: String): Flow<Resource<List<SupabaseProduct>>> = flow {
        emit(Resource.Loading())
        try {
            val products = supabaseDb.from("products")
                .select { filter { eq("retailer_id", retailerId) } }
                .decodeList<SupabaseProduct>()
            emit(Resource.Success(products))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load store products"))
        }
    }

    // ── Products ───────────────────────────────────────────────────────────────

    fun getMyProducts(retailerId: String): Flow<Resource<List<SupabaseProduct>>> = flow {
        emit(Resource.Loading())
        try {
            val products = supabaseDb.from("products")
                .select { filter { eq("retailer_id", retailerId) } }
                .decodeList<SupabaseProduct>()
            emit(Resource.Success(products))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load products"))
        }
    }

    fun addProduct(product: SupabaseProduct): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            supabaseDb.from("products").insert(product)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to add product"))
        }
    }

    fun updateProduct(product: SupabaseProduct): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            supabaseDb.from("products").update(product) {
                filter { eq("id", product.id) }
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update product"))
        }
    }

    fun deleteProduct(productId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            supabaseDb.from("products").delete {
                filter { eq("id", productId) }
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to delete product"))
        }
    }

    // ── Orders ─────────────────────────────────────────────────────────────────

    fun getMyOrders(retailerId: String): Flow<Resource<List<Order>>> = flow {
        emit(Resource.Loading())
        try {
            val rows = supabaseDb.from("orders")
                .select(Columns.raw("*, order_items(*)")) {
                    filter { eq("retailer_id", retailerId) }
                }
                .decodeList<RetailerOrderRow>()
            emit(Resource.Success(rows.map { it.toOrder() }))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load orders"))
        }
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            supabaseDb.from("orders").update(
                RetailerStatusUpdate(status = status.name)
            ) { filter { eq("id", orderId) } }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update status"))
        }
    }
}

// ── Serializable payloads ──────────────────────────────────────────────────────

@Serializable
private data class RetailerInsert(
    @SerialName("user_id")           val userId: String,
    @SerialName("store_name")        val storeName: String,
    @SerialName("store_description") val storeDescription: String,
    @SerialName("contact_phone")     val contactPhone: String,
    @SerialName("store_address")     val storeAddress: String
)

@Serializable
private data class RetailerUpdate(
    @SerialName("store_name")        val storeName: String,
    @SerialName("store_description") val storeDescription: String,
    @SerialName("contact_phone")     val contactPhone: String,
    @SerialName("store_address")     val storeAddress: String,
    @SerialName("store_logo_url")    val storeLogoUrl: String?
)

@Serializable
private data class RetailerStatusUpdate(
    @SerialName("status") val status: String
)

@Serializable
private data class RetailerOrderRow(
    @SerialName("id")             val id: String = "",
    @SerialName("user_id")        val userId: String = "",
    @SerialName("order_number")   val orderNumber: String = "",
    @SerialName("subtotal")       val subtotal: Double = 0.0,
    @SerialName("tax")            val tax: Double = 0.0,
    @SerialName("shipping")       val shipping: Double = 0.0,
    @SerialName("total")          val total: Double = 0.0,
    @SerialName("currency")       val currency: String = "PKR",
    @SerialName("status")         val status: String = "PENDING",
    @SerialName("payment_status") val paymentStatus: String = "PENDING",
    @SerialName("payment_method") val paymentMethod: String = "CASH_ON_DELIVERY",
    @SerialName("retailer_id")    val retailerId: String? = null,
    @SerialName("created_at")     val createdAt: String = "",
    @SerialName("order_items")    val orderItems: List<RetailerOrderItemRow> = emptyList()
) {
    fun toOrder() = Order(
        id            = id,
        userId        = userId,
        orderNumber   = orderNumber,
        subtotal      = subtotal,
        tax           = tax,
        shipping      = shipping,
        total         = total,
        currency      = currency,
        status        = runCatching { OrderStatus.valueOf(status) }.getOrDefault(OrderStatus.PENDING),
        paymentStatus = runCatching { PaymentStatus.valueOf(paymentStatus) }.getOrDefault(PaymentStatus.PENDING),
        paymentMethod = runCatching { PaymentMethod.valueOf(paymentMethod) }.getOrDefault(PaymentMethod.CASH_ON_DELIVERY),
        retailerId    = retailerId,
        items         = orderItems.map {
            com.example.nextgenecommerce.data.models.OrderItem(
                productId    = it.productId,
                productName  = it.productName,
                productImage = it.productImage,
                price        = it.price,
                quantity     = it.quantity,
                selectedSize  = it.selectedSize,
                selectedColor = it.selectedColor
            )
        },
        createdAt = runCatching {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                .parse(createdAt)?.time ?: System.currentTimeMillis()
        }.getOrDefault(System.currentTimeMillis()),
        updatedAt = System.currentTimeMillis()
    )
}

@Serializable
private data class RetailerOrderItemRow(
    @SerialName("product_id")     val productId: String = "",
    @SerialName("product_name")   val productName: String = "",
    @SerialName("product_image")  val productImage: String = "",
    @SerialName("price")          val price: Double = 0.0,
    @SerialName("quantity")       val quantity: Int = 1,
    @SerialName("selected_size")  val selectedSize: String = "",
    @SerialName("selected_color") val selectedColor: String = ""
)
