package com.example.nextgenecommerce.data.repository

import com.example.nextgenecommerce.data.local.dao.CartDao
import com.example.nextgenecommerce.data.models.CartItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartRepository @Inject constructor(
    private val cartDao: CartDao
) {

    fun getAllCartItems(): Flow<List<CartItem>> = cartDao.getAllCartItems()

    fun getCartItemCount(): Flow<Int> = cartDao.getCartItemCount()

    fun getCartTotal(): Flow<Double?> = cartDao.getCartTotal()

    suspend fun addToCart(item: CartItem) {
        // Check if item with same product, size, and color already exists
        val existingItem = cartDao.getCartItemByProduct(
            item.productId,
            item.selectedSize,
            item.selectedColor
        )

        if (existingItem != null) {
            // Update quantity if item exists
            cartDao.updateQuantity(existingItem.id, existingItem.quantity + item.quantity)
        } else {
            // Insert new item
            cartDao.insertCartItem(item)
        }
    }

    suspend fun updateCartItem(item: CartItem) {
        cartDao.updateCartItem(item)
    }

    suspend fun removeFromCart(item: CartItem) {
        cartDao.deleteCartItem(item)
    }

    suspend fun updateQuantity(itemId: Int, quantity: Int) {
        if (quantity <= 0) {
            cartDao.getCartItemById(itemId)?.let {
                cartDao.deleteCartItem(it)
            }
        } else {
            cartDao.updateQuantity(itemId, quantity)
        }
    }

    suspend fun clearCart() {
        cartDao.clearCart()
    }
}
