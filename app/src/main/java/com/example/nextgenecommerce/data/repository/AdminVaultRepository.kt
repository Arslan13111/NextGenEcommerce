package com.example.nextgenecommerce.data.repository

import android.util.Log
import com.example.nextgenecommerce.data.models.AdminCommission
import com.example.nextgenecommerce.data.models.VaultSummary
import com.example.nextgenecommerce.util.Resource
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminVaultRepository @Inject constructor(
    private val supabaseDb: Postgrest
) {
    companion object {
        private const val TAG   = "AdminVaultRepository"
        private const val TABLE = "admin_commissions"
        private const val RETAILER_RATE = 0.05
        private const val DELIVERY_RATE = 0.02
    }

    fun getVaultSummary(): Flow<Resource<VaultSummary>> = flow {
        emit(Resource.Loading())
        try {
            val all = supabaseDb.from(TABLE).select().decodeList<AdminCommission>()
            val retailer = all.filter { it.commissionType == "RETAILER" }.sumOf { it.amount }
            val delivery = all.filter { it.commissionType == "DELIVERY" }.sumOf { it.amount }
            emit(Resource.Success(VaultSummary(retailer, delivery, retailer + delivery)))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load vault summary", e)
            emit(Resource.Error(e.message ?: "Failed to load vault"))
        }
    }

    fun getCommissionHistory(): Flow<Resource<List<AdminCommission>>> = flow {
        emit(Resource.Loading())
        try {
            val all = supabaseDb.from(TABLE).select().decodeList<AdminCommission>()
            emit(Resource.Success(all))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load history"))
        }
    }

    // Idempotent — safe to call multiple times for the same order
    suspend fun recordCommissionForOrder(orderId: String, subtotal: Double) {
        try {
            val existing = supabaseDb.from(TABLE)
                .select { filter { eq("order_id", orderId) } }
                .decodeList<AdminCommission>()
            if (existing.isNotEmpty()) return

            val records = listOf(
                CommissionInsert(orderId = orderId, commissionType = "RETAILER", amount = subtotal * RETAILER_RATE),
                CommissionInsert(orderId = orderId, commissionType = "DELIVERY", amount = subtotal * DELIVERY_RATE)
            )
            supabaseDb.from(TABLE).insert(records)
            Log.d(TAG, "Recorded commissions for order $orderId: retailer=${subtotal * RETAILER_RATE}, delivery=${subtotal * DELIVERY_RATE}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record commission for order $orderId", e)
        }
    }
}

@Serializable
private data class CommissionInsert(
    @SerialName("order_id")        val orderId: String,
    @SerialName("commission_type") val commissionType: String,
    val amount: Double
)
