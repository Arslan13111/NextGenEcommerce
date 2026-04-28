package com.example.nextgenecommerce.data.repository

import android.util.Log
import com.example.nextgenecommerce.data.local.dao.CartDao
import com.example.nextgenecommerce.data.models.CartItem
import com.example.nextgenecommerce.data.models.SupabaseCartItem
import com.example.nextgenecommerce.util.Resource
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartRepository @Inject constructor(
    private val cartDao: CartDao,
    private val supabaseAuth: Auth,
    private val supabaseDb: Postgrest
) {

    companion object {
        private const val TAG = "CartRepository"
        private const val CART_TABLE = "cart_items"
        private const val GUEST_USER_ID = "guest"
    }

    private fun getCurrentUserId(): String {
        return supabaseAuth.currentUserOrNull()?.id ?: GUEST_USER_ID
    }

    private fun isUserLoggedIn(): Boolean {
        return supabaseAuth.currentUserOrNull() != null
    }

    // --- Read operations (from Room cache) ---

    fun getCartItemsByUser(userId: String): Flow<List<CartItem>> =
        cartDao.getCartItemsByUser(userId)

    fun getCartItemCount(userId: String): Flow<Int> =
        cartDao.getCartItemCount(userId)

    fun getCartTotal(userId: String): Flow<Double?> =
        cartDao.getCartTotal(userId)

    // --- Write operations ---

    suspend fun addToCart(item: CartItem) {
        val userId = getCurrentUserId()
        val itemWithUser = item.copy(userId = userId)

        if (isUserLoggedIn()) {
            try {
                val existingItems = supabaseDb.from(CART_TABLE)
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("product_id", item.productId)
                            eq("selected_size", item.selectedSize)
                            eq("selected_color", item.selectedColor)
                        }
                    }
                    .decodeList<SupabaseCartItem>()

                if (existingItems.isNotEmpty()) {
                    val existing = existingItems.first()
                    val newQuantity = existing.quantity + item.quantity
                    supabaseDb.from(CART_TABLE)
                        .update({
                            set("quantity", newQuantity)
                        }) {
                            filter { eq("id", existing.id) }
                        }
                } else {
                    supabaseDb.from(CART_TABLE)
                        .insert(itemWithUser.toSupabaseCartItem())
                }

                refreshLocalCache(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Error adding to Supabase cart, falling back to local", e)
                addToLocalCart(itemWithUser)
            }
        } else {
            addToLocalCart(itemWithUser)
        }
    }

    private suspend fun addToLocalCart(item: CartItem) {
        val existingItem = cartDao.getCartItemByProduct(
            item.userId, item.productId, item.selectedSize, item.selectedColor
        )
        if (existingItem != null) {
            cartDao.updateQuantity(existingItem.id, existingItem.quantity + item.quantity)
        } else {
            cartDao.insertCartItem(item)
        }
    }

    suspend fun updateQuantity(itemId: Int, quantity: Int) {
        if (quantity <= 0) {
            cartDao.getCartItemById(itemId)?.let { removeFromCart(it) }
            return
        }

        if (isUserLoggedIn()) {
            try {
                val localItem = cartDao.getCartItemById(itemId) ?: return
                val userId = getCurrentUserId()

                val remoteItems = supabaseDb.from(CART_TABLE)
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("product_id", localItem.productId)
                            eq("selected_size", localItem.selectedSize)
                            eq("selected_color", localItem.selectedColor)
                        }
                    }
                    .decodeList<SupabaseCartItem>()

                if (remoteItems.isNotEmpty()) {
                    supabaseDb.from(CART_TABLE)
                        .update({
                            set("quantity", quantity)
                        }) {
                            filter { eq("id", remoteItems.first().id) }
                        }
                }

                cartDao.updateQuantity(itemId, quantity)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating quantity on Supabase", e)
                cartDao.updateQuantity(itemId, quantity)
            }
        } else {
            cartDao.updateQuantity(itemId, quantity)
        }
    }

    suspend fun removeFromCart(item: CartItem) {
        if (isUserLoggedIn()) {
            try {
                supabaseDb.from(CART_TABLE)
                    .delete {
                        filter {
                            eq("user_id", item.userId)
                            eq("product_id", item.productId)
                            eq("selected_size", item.selectedSize)
                            eq("selected_color", item.selectedColor)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing from Supabase cart", e)
            }
        }
        cartDao.deleteCartItem(item)
    }

    suspend fun clearCart() {
        val userId = getCurrentUserId()

        if (isUserLoggedIn()) {
            try {
                supabaseDb.from(CART_TABLE)
                    .delete {
                        filter { eq("user_id", userId) }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing Supabase cart", e)
            }
        }
        cartDao.clearCartByUser(userId)
    }

    // --- Sync operations ---

    suspend fun syncCartFromSupabase(): Flow<Resource<List<CartItem>>> = flow {
        emit(Resource.Loading())
        try {
            val userId = getCurrentUserId()
            if (!isUserLoggedIn()) {
                emit(Resource.Error("User not logged in"))
                return@flow
            }

            val remoteItems = supabaseDb.from(CART_TABLE)
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<SupabaseCartItem>()

            Log.d(TAG, "Fetched ${remoteItems.size} cart items from Supabase")

            val localItems = remoteItems.map { it.toEntity() }
            cartDao.replaceCartItems(userId, localItems)

            emit(Resource.Success(localItems))
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing cart from Supabase", e)
            emit(Resource.Error(e.message ?: "Failed to sync cart"))
        }
    }

    suspend fun mergeGuestCartToUser(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val userId = getCurrentUserId()
            if (!isUserLoggedIn()) {
                emit(Resource.Error("User not logged in"))
                return@flow
            }

            val guestItems = cartDao.getGuestCartItems()
            if (guestItems.isEmpty()) {
                emit(Resource.Success(true))
                return@flow
            }

            Log.d(TAG, "Merging ${guestItems.size} guest cart items to user $userId")

            for (guestItem in guestItems) {
                val existingRemote = supabaseDb.from(CART_TABLE)
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("product_id", guestItem.productId)
                            eq("selected_size", guestItem.selectedSize)
                            eq("selected_color", guestItem.selectedColor)
                        }
                    }
                    .decodeList<SupabaseCartItem>()

                if (existingRemote.isNotEmpty()) {
                    val existing = existingRemote.first()
                    val newQuantity = existing.quantity + guestItem.quantity
                    supabaseDb.from(CART_TABLE)
                        .update({
                            set("quantity", newQuantity)
                        }) {
                            filter { eq("id", existing.id) }
                        }
                } else {
                    val supabaseItem = guestItem.copy(userId = userId).toSupabaseCartItem()
                    supabaseDb.from(CART_TABLE)
                        .insert(supabaseItem)
                }
            }

            cartDao.clearCartByUser(GUEST_USER_ID)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "Error merging guest cart", e)
            emit(Resource.Error(e.message ?: "Failed to merge guest cart"))
        }
    }

    suspend fun clearLocalUserCart(userId: String) {
        cartDao.clearCartByUser(userId)
    }

    private suspend fun refreshLocalCache(userId: String) {
        try {
            val remoteItems = supabaseDb.from(CART_TABLE)
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<SupabaseCartItem>()

            val localItems = remoteItems.map { it.toEntity() }
            cartDao.replaceCartItems(userId, localItems)
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing local cache", e)
        }
    }
}
