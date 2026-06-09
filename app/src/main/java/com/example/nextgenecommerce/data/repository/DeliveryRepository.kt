package com.example.nextgenecommerce.data.repository

import com.example.nextgenecommerce.data.models.DeliveryPartner
import com.example.nextgenecommerce.data.models.Order
import com.example.nextgenecommerce.data.models.OrderStatus
import com.example.nextgenecommerce.data.models.PaymentMethod
import com.example.nextgenecommerce.data.models.PaymentStatus
import com.example.nextgenecommerce.util.Resource
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class EarningsStats(
    val todayDeliveries: Int = 0,
    val weekDeliveries: Int = 0,
    val totalDeliveries: Int = 0,
    val todayEarnings: Double = 0.0,
    val weekEarnings: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val pendingCodAmount: Double = 0.0,
    val rating: Double = 0.0
)

data class RetailerStoreInfo(
    val storeName: String = "",
    val storeAddress: String = "",
    val contactPhone: String = ""
)

@Singleton
class DeliveryRepository @Inject constructor(
    private val supabaseAuth: Auth,
    private val supabaseDb: Postgrest
) {
    private fun uid() = supabaseAuth.currentUserOrNull()?.id
        ?: error("User not logged in")

    fun getCurrentUserId(): String? = supabaseAuth.currentUserOrNull()?.id

    // ── Profile ────────────────────────────────────────────────────────────────

    fun getMyProfile(): Flow<Resource<DeliveryPartner>> = flow {
        emit(Resource.Loading())
        try {
            val userId = uid()
            val profile = supabaseDb.from("delivery_partners")
                .select { filter { eq("user_id", userId) }; limit(1) }
                .decodeList<DeliveryPartner>()
                .firstOrNull()
            if (profile != null) emit(Resource.Success(profile))
            else emit(Resource.Error("Profile not found"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load profile"))
        }
    }

    fun createProfile(
        companyName: String,
        contactPerson: String,
        address: String,
        phone: String,
        verificationImages: List<String> = emptyList()
    ): Flow<Resource<DeliveryPartner>> = flow {
        emit(Resource.Loading())
        try {
            val userId = uid()
            supabaseDb.from("delivery_partners").insert(
                DeliveryPartnerInsert(
                    userId             = userId,
                    companyName        = companyName,
                    contactPerson      = contactPerson,
                    companyAddress     = address,
                    contactPhone       = phone,
                    verificationImages = verificationImages
                )
            )
            val created = supabaseDb.from("delivery_partners")
                .select { filter { eq("user_id", userId) }; limit(1) }
                .decodeList<DeliveryPartner>()
                .first()
            emit(Resource.Success(created))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to create profile"))
        }
    }

    fun setAvailability(partnerId: String, isAvailable: Boolean): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            supabaseDb.from("delivery_partners").update(
                AvailabilityUpdate(isAvailable = isAvailable)
            ) { filter { eq("id", partnerId) } }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update availability"))
        }
    }

    fun updateProfile(
        companyName: String,
        contactPerson: String,
        companyAddress: String,
        contactPhone: String
    ): Flow<Resource<DeliveryPartner>> = flow {
        emit(Resource.Loading())
        try {
            val userId = uid()
            supabaseDb.from("delivery_partners").update(
                ProfileUpdate(
                    companyName    = companyName,
                    contactPerson  = contactPerson,
                    companyAddress = companyAddress,
                    contactPhone   = contactPhone
                )
            ) { filter { eq("user_id", userId) } }
            val updated = supabaseDb.from("delivery_partners")
                .select { filter { eq("user_id", userId) }; limit(1) }
                .decodeList<DeliveryPartner>()
                .first()
            emit(Resource.Success(updated))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update profile"))
        }
    }

    // ── Orders ─────────────────────────────────────────────────────────────────

    /** Orders in READY_FOR_PICKUP status with no delivery partner yet assigned */
    fun getAvailableOrders(): Flow<Resource<List<Order>>> = flow {
        emit(Resource.Loading())
        try {
            val rows = supabaseDb.from("orders")
                .select(Columns.raw("*, order_items(*), shipping_addresses!shipping_address_id(*)")) {
                    filter {
                        eq("status", "READY_FOR_PICKUP")
                    }
                }
                .decodeList<DeliveryOrderRow>()
                .filter { it.deliveryPartnerId == null }
            emit(Resource.Success(rows.map { it.toOrder() }))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load available orders"))
        }
    }

    /** Orders assigned to the current delivery partner */
    fun getMyOrders(): Flow<Resource<List<Order>>> = flow {
        emit(Resource.Loading())
        try {
            val userId = uid()
            val rows = supabaseDb.from("orders")
                .select(Columns.raw("*, order_items(*), shipping_addresses!shipping_address_id(*)")) {
                    filter { eq("delivery_partner_id", userId) }
                }
                .decodeList<DeliveryOrderRow>()
            emit(Resource.Success(rows.map { it.toOrder() }))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load your orders"))
        }
    }

    /** Claim an order — sets this partner as the delivery agent and marks SHIPPED (Picked up) */
    fun acceptOrder(orderId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val userId = uid()
            supabaseDb.from("orders").update(
                AcceptOrderUpdate(
                    deliveryPartnerId = userId,
                    status            = OrderStatus.SHIPPED.name
                )
            ) { filter { eq("id", orderId) } }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to accept order"))
        }
    }

    /** Mark an order as out for delivery */
    fun startDelivery(orderId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            supabaseDb.from("orders").update(
                DeliveryStatusUpdate(status = OrderStatus.OUT_FOR_DELIVERY.name)
            ) { filter { eq("id", orderId) } }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to start delivery"))
        }
    }

    /** Mark an order as delivered */
    fun markDelivered(orderId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            supabaseDb.from("orders").update(
                DeliveryStatusUpdate(status = OrderStatus.DELIVERED.name)
            ) { filter { eq("id", orderId) } }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to mark as delivered"))
        }
    }

    /** Earnings and performance stats computed from assigned order data */
    fun getEarningsStats(): Flow<Resource<EarningsStats>> = flow {
        emit(Resource.Loading())
        try {
            val userId = uid()
            val allRows = supabaseDb.from("orders")
                .select { filter { eq("delivery_partner_id", userId) } }
                .decodeList<DeliveryOrderRow>()

            val profile = supabaseDb.from("delivery_partners")
                .select { filter { eq("user_id", userId) }; limit(1) }
                .decodeList<DeliveryPartner>()
                .firstOrNull()

            val now     = System.currentTimeMillis()
            val dayMs   = 86_400_000L
            val weekMs  = 7 * dayMs

            val delivered  = allRows.filter { it.status == "DELIVERED" }
            val todayDone  = delivered.filter { parseTimestamp(it.createdAt) > now - dayMs }
            val weekDone   = delivered.filter { parseTimestamp(it.createdAt) > now - weekMs }
            val pendingCod = allRows.filter {
                it.paymentMethod == "CASH_ON_DELIVERY" &&
                it.status in listOf("SHIPPED", "OUT_FOR_DELIVERY")
            }

            emit(Resource.Success(EarningsStats(
                todayDeliveries  = todayDone.size,
                weekDeliveries   = weekDone.size,
                totalDeliveries  = profile?.totalDeliveries ?: delivered.size,
                todayEarnings    = todayDone.sumOf { it.shipping },
                weekEarnings     = weekDone.sumOf { it.shipping },
                totalEarnings    = delivered.sumOf { it.shipping },
                pendingCodAmount = pendingCod.sumOf { it.total },
                rating           = profile?.rating ?: 0.0
            )))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load stats"))
        }
    }

    /** All completed deliveries for history tab */
    fun getDeliveryHistory(): Flow<Resource<List<Order>>> = flow {
        emit(Resource.Loading())
        try {
            val userId = uid()
            val rows = supabaseDb.from("orders")
                .select(Columns.raw("*, order_items(*), shipping_addresses!shipping_address_id(*)")) {
                    filter {
                        eq("delivery_partner_id", userId)
                        eq("status", "DELIVERED")
                    }
                }
                .decodeList<DeliveryOrderRow>()
            emit(Resource.Success(rows.map { it.toOrder() }))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load history"))
        }
    }

    // ── Return pickups ─────────────────────────────────────────────────────────

    /** Retailer-approved returns waiting for a delivery partner to pick up from customer */
    fun getAvailableReturnOrders(): Flow<Resource<List<Order>>> = flow {
        emit(Resource.Loading())
        try {
            val rows = supabaseDb.from("orders")
                .select(Columns.raw("*, order_items(*), shipping_addresses!shipping_address_id(*)")) {
                    filter { eq("status", "RETURN_APPROVED") }
                }
                .decodeList<DeliveryOrderRow>()
            emit(Resource.Success(rows.map { it.toOrder() }))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load return orders"))
        }
    }

    /** Returns currently being transported by this delivery partner to the retailer */
    fun getMyReturnOrders(): Flow<Resource<List<Order>>> = flow {
        emit(Resource.Loading())
        try {
            val userId = uid()
            val rows = supabaseDb.from("orders")
                .select(Columns.raw("*, order_items(*), shipping_addresses!shipping_address_id(*)")) {
                    filter {
                        eq("delivery_partner_id", userId)
                        eq("status", "RETURN_IN_TRANSIT")
                    }
                }
                .decodeList<DeliveryOrderRow>()
            emit(Resource.Success(rows.map { it.toOrder() }))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load in-transit returns"))
        }
    }

    /** Partner accepts a return pickup — assigns themselves and marks RETURN_IN_TRANSIT */
    fun acceptReturnPickup(orderId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val userId = uid()
            supabaseDb.from("orders").update(
                AcceptOrderUpdate(deliveryPartnerId = userId, status = OrderStatus.RETURN_IN_TRANSIT.name)
            ) { filter { eq("id", orderId) } }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to accept return pickup"))
        }
    }

    /** Partner has delivered the returned item back to the retailer */
    fun markReturnDeliveredToRetailer(orderId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            supabaseDb.from("orders").update(
                DeliveryStatusUpdate(status = OrderStatus.RETURN_RECEIVED.name)
            ) { filter { eq("id", orderId) } }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to mark return as delivered to retailer"))
        }
    }

    // ── Admin ──────────────────────────────────────────────────────────────────

    fun getAllDeliveryPartnersForAdmin(): Flow<Resource<List<DeliveryPartner>>> = flow {
        emit(Resource.Loading())
        try {
            val partners = supabaseDb.from("delivery_partners")
                .select()
                .decodeList<DeliveryPartner>()
            emit(Resource.Success(partners))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load delivery partners"))
        }
    }

    fun setDeliveryPartnerApproval(partnerId: String, approved: Boolean): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            if (approved) {
                // Capture current active partners before revoking (for order reassignment)
                val oldActive = supabaseDb.from("delivery_partners")
                    .select { filter { eq("is_verified", true) } }
                    .decodeList<DeliveryPartner>()
                    .filter { it.id != partnerId }

                // Get new partner's userId
                val newPartner = supabaseDb.from("delivery_partners")
                    .select { filter { eq("id", partnerId) }; limit(1) }
                    .decodeList<DeliveryPartner>()
                    .first()

                // Auto-revoke all others back to PENDING (not REJECTED — admin didn't explicitly reject them)
                supabaseDb.from("delivery_partners").update(
                    VerifyUpdate(isVerified = false, isRejected = false)
                ) { filter { neq("id", partnerId) } }

                // Approve new partner
                supabaseDb.from("delivery_partners").update(
                    VerifyUpdate(isVerified = true, isRejected = false)
                ) { filter { eq("id", partnerId) } }

                // Reassign in-progress orders from old partners to new partner
                oldActive.forEach { oldPartner ->
                    supabaseDb.from("orders").update(
                        PartnerReassignUpdate(deliveryPartnerId = newPartner.userId)
                    ) {
                        filter {
                            eq("delivery_partner_id", oldPartner.userId)
                            or {
                                eq("status", "SHIPPED")
                                eq("status", "OUT_FOR_DELIVERY")
                            }
                        }
                    }
                }
            } else {
                // Explicit admin rejection
                supabaseDb.from("delivery_partners").update(
                    VerifyUpdate(isVerified = false, isRejected = true)
                ) { filter { eq("id", partnerId) } }
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update delivery partner approval"))
        }
    }

    /** Fetch store name / address / phone for a set of retailer IDs (for return card display) */
    suspend fun getRetailerInfoBatch(retailerIds: Set<String>): Map<String, RetailerStoreInfo> {
        if (retailerIds.isEmpty()) return emptyMap()
        return try {
            supabaseDb.from("retailers")
                .select(Columns.raw("id, store_name, store_address, contact_phone"))
                .decodeList<RetailerInfoRow>()
                .filter { it.id in retailerIds }
                .associate { row ->
                    row.id to RetailerStoreInfo(
                        storeName    = row.storeName,
                        storeAddress = row.storeAddress ?: "",
                        contactPhone = row.contactPhone ?: ""
                    )
                }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun parseTimestamp(str: String): Long = runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(str)?.time
            ?: System.currentTimeMillis()
    }.getOrDefault(System.currentTimeMillis())
}

// ── Serializable payloads ──────────────────────────────────────────────────────

@Serializable
private data class DeliveryPartnerInsert(
    @SerialName("user_id")              val userId: String,
    @SerialName("company_name")         val companyName: String,
    @SerialName("contact_person")       val contactPerson: String,
    @SerialName("company_address")      val companyAddress: String,
    @SerialName("contact_phone")        val contactPhone: String,
    @SerialName("verification_images")  val verificationImages: List<String> = emptyList()
)

@Serializable
private data class AvailabilityUpdate(
    @SerialName("is_available") val isAvailable: Boolean
)

@Serializable
private data class ProfileUpdate(
    @SerialName("company_name")    val companyName: String,
    @SerialName("contact_person")  val contactPerson: String,
    @SerialName("company_address") val companyAddress: String,
    @SerialName("contact_phone")   val contactPhone: String
)

@Serializable
private data class AcceptOrderUpdate(
    @SerialName("delivery_partner_id") val deliveryPartnerId: String,
    @SerialName("status")              val status: String
)

@Serializable
private data class DeliveryStatusUpdate(
    @SerialName("status") val status: String
)

@Serializable
private data class VerifyUpdate(
    @SerialName("is_verified") val isVerified: Boolean,
    @SerialName("is_rejected") val isRejected: Boolean
)

@Serializable
private data class PartnerReassignUpdate(
    @SerialName("delivery_partner_id") val deliveryPartnerId: String
)

@Serializable
private data class DeliveryAddressRow(
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
private data class DeliveryOrderRow(
    @SerialName("id")                  val id: String = "",
    @SerialName("user_id")             val userId: String = "",
    @SerialName("order_number")        val orderNumber: String = "",
    @SerialName("subtotal")            val subtotal: Double = 0.0,
    @SerialName("tax")                 val tax: Double = 0.0,
    @SerialName("shipping")            val shipping: Double = 0.0,
    @SerialName("total")               val total: Double = 0.0,
    @SerialName("currency")            val currency: String = "PKR",
    @SerialName("status")              val status: String = "PENDING",
    @SerialName("payment_status")      val paymentStatus: String = "PENDING",
    @SerialName("payment_method")      val paymentMethod: String = "CASH_ON_DELIVERY",
    @SerialName("retailer_id")         val retailerId: String? = null,
    @SerialName("delivery_partner_id") val deliveryPartnerId: String? = null,
    @SerialName("created_at")          val createdAt: String = "",
    @SerialName("order_items")         val orderItems: List<DeliveryOrderItemRow> = emptyList(),
    @SerialName("shipping_addresses")  val shippingAddress: DeliveryAddressRow? = null
) {
    fun toOrder() = Order(
        id                = id,
        userId            = userId,
        orderNumber       = orderNumber,
        subtotal          = subtotal,
        tax               = tax,
        shipping          = shipping,
        total             = total,
        currency          = currency,
        status            = runCatching { OrderStatus.valueOf(status) }.getOrDefault(OrderStatus.PENDING),
        paymentStatus     = runCatching { PaymentStatus.valueOf(paymentStatus) }.getOrDefault(PaymentStatus.PENDING),
        paymentMethod     = runCatching { PaymentMethod.valueOf(paymentMethod) }.getOrDefault(PaymentMethod.CASH_ON_DELIVERY),
        retailerId        = retailerId,
        deliveryPartnerId = deliveryPartnerId,
        shippingAddress   = shippingAddress?.toAddress(),
        items             = orderItems.map {
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
private data class DeliveryOrderItemRow(
    @SerialName("product_id")     val productId: String = "",
    @SerialName("product_name")   val productName: String = "",
    @SerialName("product_image")  val productImage: String = "",
    @SerialName("price")          val price: Double = 0.0,
    @SerialName("quantity")       val quantity: Int = 1,
    @SerialName("selected_size")  val selectedSize: String = "",
    @SerialName("selected_color") val selectedColor: String = ""
)

@Serializable
private data class RetailerInfoRow(
    @SerialName("id")            val id: String = "",
    @SerialName("store_name")    val storeName: String = "",
    @SerialName("store_address") val storeAddress: String? = null,
    @SerialName("contact_phone") val contactPhone: String? = null
)
