package com.example.nextgenecommerce.data.local.dao

import androidx.room.*
import com.example.nextgenecommerce.data.models.CartItem
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items WHERE userId = :userId ORDER BY addedAt DESC")
    fun getCartItemsByUser(userId: String): Flow<List<CartItem>>

    @Query("SELECT * FROM cart_items WHERE id = :itemId")
    suspend fun getCartItemById(itemId: Int): CartItem?

    @Query("SELECT * FROM cart_items WHERE userId = :userId AND productId = :productId AND selectedSize = :size AND selectedColor = :color")
    suspend fun getCartItemByProduct(userId: String, productId: String, size: String, color: String): CartItem?

    @Query("SELECT COUNT(*) FROM cart_items WHERE userId = :userId")
    fun getCartItemCount(userId: String): Flow<Int>

    @Query("SELECT SUM(price * quantity) FROM cart_items WHERE userId = :userId")
    fun getCartTotal(userId: String): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItems(items: List<CartItem>)

    @Update
    suspend fun updateCartItem(item: CartItem)

    @Delete
    suspend fun deleteCartItem(item: CartItem)

    @Query("DELETE FROM cart_items WHERE userId = :userId")
    suspend fun clearCartByUser(userId: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    @Query("UPDATE cart_items SET quantity = :quantity WHERE id = :itemId")
    suspend fun updateQuantity(itemId: Int, quantity: Int)

    @Query("SELECT * FROM cart_items WHERE userId = 'guest'")
    suspend fun getGuestCartItems(): List<CartItem>

    @Transaction
    suspend fun replaceCartItems(userId: String, items: List<CartItem>) {
        clearCartByUser(userId)
        insertCartItems(items)
    }
}
