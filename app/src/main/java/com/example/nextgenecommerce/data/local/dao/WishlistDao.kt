package com.example.nextgenecommerce.data.local.dao

import androidx.room.*
import com.example.nextgenecommerce.data.models.WishlistItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {
    @Query("SELECT * FROM wishlist_items ORDER BY addedAt DESC")
    fun getAllWishlistItems(): Flow<List<WishlistItem>>

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist_items WHERE productId = :productId)")
    fun isProductInWishlist(productId: String): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM wishlist_items")
    fun getWishlistCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWishlistItem(item: WishlistItem)

    @Delete
    suspend fun deleteWishlistItem(item: WishlistItem)

    @Query("DELETE FROM wishlist_items WHERE productId = :productId")
    suspend fun deleteWishlistItemByProductId(productId: String)

    @Query("DELETE FROM wishlist_items")
    suspend fun clearWishlist()
}
