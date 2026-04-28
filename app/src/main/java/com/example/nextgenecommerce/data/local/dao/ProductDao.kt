package com.example.nextgenecommerce.data.local.dao

import androidx.room.*
import com.example.nextgenecommerce.data.models.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isFeatured = 1 ORDER BY createdAt DESC LIMIT 10")
    fun getFeaturedProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isNew = 1 ORDER BY createdAt DESC LIMIT 10")
    fun getNewProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId")
    fun getProductById(productId: String): Flow<ProductEntity?>

    @Query("SELECT * FROM products WHERE category = :category ORDER BY createdAt DESC")
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()
}
