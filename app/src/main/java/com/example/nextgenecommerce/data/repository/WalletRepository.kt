package com.example.nextgenecommerce.data.repository

import com.example.nextgenecommerce.util.Resource
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class WalletTransaction(
    val id: String,
    val amount: Double,
    val type: String,        // "CREDIT" or "DEBIT"
    val description: String,
    val createdAt: Long
)

@Singleton
class WalletRepository @Inject constructor(
    private val supabaseAuth: Auth,
    private val supabaseDb: Postgrest
) {

    fun getBalance(): Flow<Resource<Double>> = flow {
        emit(Resource.Loading())
        try {
            val userId = supabaseAuth.currentUserOrNull()?.id
                ?: throw Exception("User not logged in")
            val rows = supabaseDb.from("user_wallets")
                .select(Columns.raw("balance")) {
                    filter { eq("user_id", userId) }
                    limit(1)
                }
                .decodeList<WalletRow>()
            emit(Resource.Success(rows.firstOrNull()?.balance ?: 0.0))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load vault balance"))
        }
    }

    fun getTransactions(): Flow<Resource<List<WalletTransaction>>> = flow {
        emit(Resource.Loading())
        try {
            val userId = supabaseAuth.currentUserOrNull()?.id
                ?: throw Exception("User not logged in")
            val rows = supabaseDb.from("wallet_transactions")
                .select(Columns.raw("id, amount, type, description, created_at")) {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<WalletTransactionRow>()
            emit(Resource.Success(rows.map { it.toWalletTransaction() }))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load vault transactions"))
        }
    }

    /**
     * Syncs refunds from completed returns into the customer's vault.
     * Must be called from the CUSTOMER's authenticated session (not the retailer's),
     * because RLS only allows users to write to their own wallet.
     *
     * Scans all RETURNED+REFUNDED orders for the current user, finds any that haven't
     * been credited yet (identified by "Refund for order: {orderId}" in transactions),
     * and applies them in a single balance update.
     */
    suspend fun syncReturnRefunds() {
        try {
            val userId = supabaseAuth.currentUserOrNull()?.id ?: return

            // 1. Get all RETURNED orders for this user
            val returnedOrders = supabaseDb.from("orders")
                .select(Columns.raw("id, total")) {
                    filter {
                        eq("user_id", userId)
                        eq("status", "RETURNED")
                        eq("payment_status", "REFUNDED")
                    }
                }
                .decodeList<OrderRefundRow>()

            if (returnedOrders.isEmpty()) return

            // 2. Get descriptions of existing CREDIT transactions to avoid double-crediting
            val creditedOrderIds = supabaseDb.from("wallet_transactions")
                .select(Columns.raw("description")) {
                    filter {
                        eq("user_id", userId)
                        eq("type", "CREDIT")
                    }
                }
                .decodeList<TxnDescriptionRow>()
                .mapNotNull { row ->
                    if (row.description.startsWith("Refund for order: "))
                        row.description.removePrefix("Refund for order: ")
                    else null
                }
                .toSet()

            // 3. Find uncredited returns
            val uncredited = returnedOrders.filter { it.id !in creditedOrderIds }
            if (uncredited.isEmpty()) return

            // 4. Ensure wallet exists and read current balance
            val walletId = ensureWalletExists(userId)
            val balanceRows = supabaseDb.from("user_wallets")
                .select(Columns.raw("balance")) {
                    filter { eq("user_id", userId) }
                    limit(1)
                }
                .decodeList<WalletRow>()
            val newBalance = (balanceRows.firstOrNull()?.balance ?: 0.0) +
                uncredited.sumOf { it.total }

            // 5. Insert a transaction record for each credited order
            uncredited.forEach { order ->
                supabaseDb.from("wallet_transactions").insert(
                    WalletTransactionInsert(
                        id = UUID.randomUUID().toString(),
                        userId = userId,
                        walletId = walletId,
                        amount = order.total,
                        type = "CREDIT",
                        description = "Refund for order: ${order.id}"
                    )
                )
            }

            // 6. Update wallet balance in one write
            supabaseDb.from("user_wallets").update(
                WalletBalanceUpdate(balance = newBalance)
            ) { filter { eq("user_id", userId) } }

        } catch (_: Exception) { /* best-effort — UI will retry on next refresh */ }
    }

    suspend fun debitWallet(userId: String, amount: Double, description: String): Boolean {
        return try {
            val walletId = ensureWalletExists(userId)
            val rows = supabaseDb.from("user_wallets")
                .select(Columns.raw("balance")) {
                    filter { eq("user_id", userId) }
                    limit(1)
                }
                .decodeList<WalletRow>()
            val current = rows.firstOrNull()?.balance ?: 0.0
            if (current < amount) return false
            supabaseDb.from("user_wallets").update(
                WalletBalanceUpdate(balance = current - amount)
            ) { filter { eq("user_id", userId) } }
            supabaseDb.from("wallet_transactions").insert(
                WalletTransactionInsert(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    walletId = walletId,
                    amount = amount,
                    type = "DEBIT",
                    description = description
                )
            )
            true
        } catch (_: Exception) { false }
    }

    private suspend fun ensureWalletExists(userId: String): String {
        val rows = supabaseDb.from("user_wallets")
            .select(Columns.raw("id")) {
                filter { eq("user_id", userId) }
                limit(1)
            }
            .decodeList<WalletIdRow>()
        if (rows.isNotEmpty()) return rows.first().id
        val newId = UUID.randomUUID().toString()
        supabaseDb.from("user_wallets").insert(
            WalletInsert(id = newId, userId = userId, balance = 0.0)
        )
        return newId
    }
}

// ── Serializable helpers ──────────────────────────────────────────────────────

@Serializable
private data class WalletRow(
    @SerialName("balance") val balance: Double = 0.0
)

@Serializable
private data class WalletIdRow(
    @SerialName("id") val id: String = ""
)

@Serializable
private data class WalletBalanceUpdate(
    @SerialName("balance") val balance: Double
)

@Serializable
private data class WalletInsert(
    @SerialName("id")      val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("balance") val balance: Double
)

@Serializable
private data class WalletTransactionInsert(
    @SerialName("id")          val id: String,
    @SerialName("user_id")     val userId: String,
    @SerialName("wallet_id")   val walletId: String,
    @SerialName("amount")      val amount: Double,
    @SerialName("type")        val type: String,
    @SerialName("description") val description: String
)

@Serializable
private data class OrderRefundRow(
    @SerialName("id")    val id: String = "",
    @SerialName("total") val total: Double = 0.0
)

@Serializable
private data class TxnDescriptionRow(
    @SerialName("description") val description: String = ""
)

@Serializable
private data class WalletTransactionRow(
    @SerialName("id")          val id: String = "",
    @SerialName("amount")      val amount: Double = 0.0,
    @SerialName("type")        val type: String = "CREDIT",
    @SerialName("description") val description: String = "",
    @SerialName("created_at")  val createdAt: String = ""
) {
    fun toWalletTransaction() = WalletTransaction(
        id = id,
        amount = amount,
        type = type,
        description = description,
        createdAt = runCatching {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                .parse(createdAt)?.time ?: System.currentTimeMillis()
        }.getOrDefault(System.currentTimeMillis())
    )
}
