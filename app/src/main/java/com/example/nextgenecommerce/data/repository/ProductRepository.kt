package com.example.nextgenecommerce.data.repository

import android.util.Log
import com.example.nextgenecommerce.data.local.dao.ProductDao
import com.example.nextgenecommerce.data.models.ProductEntity
import com.example.nextgenecommerce.data.models.SupabaseProduct
import com.example.nextgenecommerce.data.remote.ApiService
import com.example.nextgenecommerce.util.Resource
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val apiService: ApiService,
    private val productDao: ProductDao,
    private val postgrest: Postgrest
) {

    companion object {
        private const val TAG = "ProductRepository"
        private const val PRODUCTS_TABLE = "products"
    }

    fun getAllProducts(): Flow<List<ProductEntity>> = productDao.getAllProducts()

    fun getFeaturedProducts(): Flow<List<ProductEntity>> = productDao.getFeaturedProducts()

    fun getNewProducts(): Flow<List<ProductEntity>> = productDao.getNewProducts()

    fun getProductById(productId: String): Flow<ProductEntity?> = productDao.getProductById(productId)

    fun getProductsByCategory(category: String): Flow<List<ProductEntity>> =
        productDao.getProductsByCategory(category)

    fun searchProducts(query: String): Flow<List<ProductEntity>> = productDao.searchProducts(query)

    suspend fun syncProducts(): Flow<Resource<List<ProductEntity>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getProducts(limit = 100)
            if (response.isSuccessful && response.body() != null) {
                val products = response.body()!!.products
                productDao.insertProducts(products)
                emit(Resource.Success(products))
            } else {
                emit(Resource.Error("Failed to fetch products"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An error occurred"))
        }
    }

    suspend fun syncFeaturedProducts(): Flow<Resource<List<ProductEntity>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getFeaturedProducts()
            if (response.isSuccessful && response.body() != null) {
                val products = response.body()!!.products
                products.forEach { product ->
                    productDao.insertProduct(product.copy(isFeatured = true))
                }
                emit(Resource.Success(products))
            } else {
                emit(Resource.Error("Failed to fetch featured products"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An error occurred"))
        }
    }

    suspend fun insertProduct(product: ProductEntity) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: ProductEntity) {
        productDao.insertProduct(product) // Room's @Insert with OnConflictStrategy.REPLACE will update
    }

    suspend fun deleteProduct(product: ProductEntity) {
        productDao.deleteProduct(product)
    }

    suspend fun deleteProductById(productId: String) {
        productDao.deleteProductById(productId)
    }

    private suspend fun replaceLocalProductCache(remoteProducts: List<ProductEntity>) {
        if (remoteProducts.isEmpty()) {
            productDao.deleteAllProducts()
            return
        }

        productDao.insertProducts(remoteProducts)
        productDao.deleteProductsNotInIds(remoteProducts.map { it.id })
    }

    /**
     * Sync products from Supabase to Room database
     * Fetches all products from Supabase and stores them locally
     */
    suspend fun syncProductsFromSupabase(): Flow<Resource<List<ProductEntity>>> = flow {
        emit(Resource.Loading())
        try {
            Log.d(TAG, "Starting Supabase product sync...")

            val supabaseProducts = postgrest.from(PRODUCTS_TABLE)
                .select()
                .decodeList<SupabaseProduct>()

            Log.d(TAG, "Fetched ${supabaseProducts.size} products from Supabase")

            // Convert to Room entities
            val productEntities = supabaseProducts.map { it.toEntity() }

            // Upsert — never wipe the cache first (causes empty-list flash in UI)
            replaceLocalProductCache(productEntities)

            Log.d(TAG, "Saved ${productEntities.size} products to Room database")
            emit(Resource.Success(productEntities))
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing products from Supabase", e)
            emit(Resource.Error(e.message ?: "Failed to sync products from Supabase"))
        }
    }

    /**
     * Fetch a single product from Supabase by ID
     */
    suspend fun fetchProductFromSupabase(productId: String): Flow<Resource<ProductEntity>> = flow {
        emit(Resource.Loading())
        try {
            val supabaseProduct = postgrest.from(PRODUCTS_TABLE)
                .select {
                    filter { eq("id", productId) }
                }
                .decodeSingleOrNull<SupabaseProduct>()

            if (supabaseProduct != null) {
                val entity = supabaseProduct.toEntity()
                productDao.insertProduct(entity)
                emit(Resource.Success(entity))
            } else {
                productDao.deleteProductById(productId)
                emit(Resource.Error("Product not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching product from Supabase", e)
            emit(Resource.Error(e.message ?: "Failed to fetch product"))
        }
    }

    /**
     * Fetch featured products from Supabase
     */
    suspend fun syncFeaturedProductsFromSupabase(): Flow<Resource<List<ProductEntity>>> = flow {
        emit(Resource.Loading())
        try {
            val supabaseProducts = postgrest.from(PRODUCTS_TABLE)
                .select {
                    filter { eq("is_featured", true) }
                }
                .decodeList<SupabaseProduct>()

            val productEntities = supabaseProducts.map { it.toEntity() }
            productDao.insertProducts(productEntities)
            emit(Resource.Success(productEntities))
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing featured products", e)
            emit(Resource.Error(e.message ?: "Failed to sync featured products"))
        }
    }

    /**
     * Fetch new products from Supabase
     */
    suspend fun syncNewProductsFromSupabase(): Flow<Resource<List<ProductEntity>>> = flow {
        emit(Resource.Loading())
        try {
            val supabaseProducts = postgrest.from(PRODUCTS_TABLE)
                .select {
                    filter { eq("is_new", true) }
                }
                .decodeList<SupabaseProduct>()

            val productEntities = supabaseProducts.map { it.toEntity() }
            productDao.insertProducts(productEntities)
            emit(Resource.Success(productEntities))
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing new products", e)
            emit(Resource.Error(e.message ?: "Failed to sync new products"))
        }
    }

    /**
     * Fetch products by category from Supabase
     */
    suspend fun syncProductsByCategoryFromSupabase(category: String): Flow<Resource<List<ProductEntity>>> = flow {
        emit(Resource.Loading())
        try {
            val supabaseProducts = postgrest.from(PRODUCTS_TABLE)
                .select {
                    filter { eq("category", category) }
                }
                .decodeList<SupabaseProduct>()

            val productEntities = supabaseProducts.map { it.toEntity() }
            productDao.insertProducts(productEntities)
            emit(Resource.Success(productEntities))
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing products by category", e)
            emit(Resource.Error(e.message ?: "Failed to sync products by category"))
        }
    }

    // =====================================================
    // ADMIN OPERATIONS - Supabase CRUD
    // These operations require admin privileges (enforced by RLS)
    // =====================================================

    /**
     * Add a new product to Supabase (Admin only)
     * RLS policies will reject if user is not admin
     */
    suspend fun addProductToSupabase(product: ProductEntity): Flow<Resource<ProductEntity>> = flow {
        emit(Resource.Loading())
        try {
            val supabaseProduct = product.toSupabaseProduct()

            Log.d(TAG, "Adding product to Supabase: ${product.name}")

            postgrest.from(PRODUCTS_TABLE)
                .insert(supabaseProduct)

            // Also save to local database
            productDao.insertProduct(product)

            Log.d(TAG, "Product added successfully: ${product.id}")
            emit(Resource.Success(product))
        } catch (e: Exception) {
            Log.e(TAG, "Error adding product to Supabase", e)
            val errorMessage = when {
                e.message?.contains("policy", ignoreCase = true) == true ||
                e.message?.contains("permission", ignoreCase = true) == true ||
                e.message?.contains("denied", ignoreCase = true) == true ->
                    "Access denied. Admin privileges required."
                else -> e.message ?: "Failed to add product"
            }
            emit(Resource.Error(errorMessage))
        }
    }

    /**
     * Update an existing product in Supabase (Admin only)
     * RLS policies will reject if user is not admin
     */
    suspend fun updateProductInSupabase(product: ProductEntity): Flow<Resource<ProductEntity>> = flow {
        emit(Resource.Loading())
        try {
            Log.d(TAG, "Updating product in Supabase: ${product.id}")

            // Use a payload that excludes retailer_id so the DB value is never wiped
            val payload = ProductAdminUpdatePayload(
                name         = product.name,
                description  = product.description,
                price        = product.price,
                originalPrice = product.originalPrice,
                category     = product.category.name,
                subCategory  = product.subCategory,
                images       = product.images,
                imageUrl     = product.images.firstOrNull(),
                lensId       = product.lensId,
                sizes        = product.sizes,
                colors       = product.colors,
                colorImages  = product.colorImages,
                stock        = product.stock,
                inStock      = product.stock > 0,
                isFeatured   = product.isFeatured,
                isNew        = product.isNew,
                brand        = product.brand,
                tags         = product.tags
            )

            postgrest.from(PRODUCTS_TABLE)
                .update(payload) {
                    filter { eq("id", product.id) }
                }

            productDao.insertProduct(product)

            Log.d(TAG, "Product updated successfully: ${product.id}")
            emit(Resource.Success(product))
        } catch (e: Exception) {
            Log.e(TAG, "Error updating product in Supabase", e)
            val errorMessage = when {
                e.message?.contains("policy", ignoreCase = true) == true ||
                e.message?.contains("permission", ignoreCase = true) == true ||
                e.message?.contains("denied", ignoreCase = true) == true ->
                    "Access denied. Admin privileges required."
                else -> e.message ?: "Failed to update product"
            }
            emit(Resource.Error(errorMessage))
        }
    }

    /**
     * Delete a product from Supabase (Admin only)
     * RLS policies will reject if user is not admin
     */
    suspend fun deleteProductFromSupabase(productId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            Log.d(TAG, "Deleting product from Supabase: $productId")

            postgrest.from(PRODUCTS_TABLE)
                .delete {
                    filter { eq("id", productId) }
                }

            // Also delete from local database
            productDao.deleteProductById(productId)

            Log.d(TAG, "Product deleted successfully: $productId")
            emit(Resource.Success(true))
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting product from Supabase", e)
            val errorMessage = when {
                e.message?.contains("policy", ignoreCase = true) == true ||
                e.message?.contains("permission", ignoreCase = true) == true ||
                e.message?.contains("denied", ignoreCase = true) == true ->
                    "Access denied. Admin privileges required."
                else -> e.message ?: "Failed to delete product"
            }
            emit(Resource.Error(errorMessage))
        }
    }

    suspend fun assignAllProductsToRetailer(retailerId: String): Flow<Resource<Int>> = flow {
        emit(Resource.Loading())
        try {
            // Fetch only admin/unassigned products (retailer_id IS NULL)
            val adminProducts = postgrest.from(PRODUCTS_TABLE)
                .select()
                .decodeList<SupabaseProduct>()
                .filter { it.retailerId == null }
            if (adminProducts.isEmpty()) {
                emit(Resource.Error("No unassigned products found to copy"))
                return@flow
            }
            // Fetch existing product names for this retailer to avoid duplicate copies
            val existing = postgrest.from(PRODUCTS_TABLE)
                .select { filter { eq("retailer_id", retailerId) } }
                .decodeList<SupabaseProduct>()
                .map { it.name.lowercase() }
                .toSet()

            var copied = 0
            adminProducts.forEach { product ->
                if (product.name.lowercase() !in existing) {
                    val copy = product.copy(
                        id = UUID.randomUUID().toString(),
                        retailerId = retailerId,
                        retailerIds = emptyList()
                    )
                    postgrest.from(PRODUCTS_TABLE).insert(copy)
                    productDao.insertProduct(copy.toEntity())
                    copied++
                }
            }
            emit(Resource.Success(copied))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to assign products"))
        }
    }
}

// Excludes retailer_id/retailer_ids/created_at so admin edits never wipe the DB assignment
@Serializable
private data class ProductAdminUpdatePayload(
    val name: String,
    val description: String,
    val price: Double,
    @SerialName("original_price") val originalPrice: Double,
    val category: String,
    @SerialName("sub_category") val subCategory: String,
    val images: List<String>,
    @SerialName("image_url") val imageUrl: String?,
    @SerialName("lens_id") val lensId: String?,
    val sizes: List<String>,
    val colors: List<String>,
    @SerialName("color_images") val colorImages: Map<String, String>,
    val stock: Int,
    @SerialName("in_stock") val inStock: Boolean,
    @SerialName("is_featured") val isFeatured: Boolean,
    @SerialName("is_new") val isNew: Boolean,
    val brand: String,
    val tags: List<String>,
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis()
)
