package com.example.nextgenecommerce.data.repository

import com.example.nextgenecommerce.data.local.dao.WishlistDao
import com.example.nextgenecommerce.data.models.WishlistItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WishlistRepository @Inject constructor(
    private val wishlistDao: WishlistDao
) {

    fun getAllWishlistItems(): Flow<List<WishlistItem>> = wishlistDao.getAllWishlistItems()

    fun isProductInWishlist(productId: String): Flow<Boolean> = wishlistDao.isProductInWishlist(productId)

    fun getWishlistCount(): Flow<Int> = wishlistDao.getWishlistCount()

    suspend fun addToWishlist(item: WishlistItem) {
        wishlistDao.insertWishlistItem(item)
    }

    suspend fun removeFromWishlist(productId: String) {
        wishlistDao.deleteWishlistItemByProductId(productId)
    }

    suspend fun toggleWishlist(item: WishlistItem, isInWishlist: Boolean) {
        if (isInWishlist) {
            wishlistDao.deleteWishlistItemByProductId(item.productId)
        } else {
            wishlistDao.insertWishlistItem(item)
        }
    }

    suspend fun clearWishlist() {
        wishlistDao.clearWishlist()
    }
}
