package com.example.nextgenecommerce.data.repository

import com.example.nextgenecommerce.data.local.dao.ProductDao
import com.example.nextgenecommerce.data.models.ProductEntity
import com.example.nextgenecommerce.data.remote.ApiService
import com.example.nextgenecommerce.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val apiService: ApiService,
    private val productDao: ProductDao
) {

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

    suspend fun insertSampleProducts() {
        val sampleProducts = getSampleProducts()
        productDao.insertProducts(sampleProducts)
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
        getProductById(productId).collect { product ->
            product?.let { deleteProduct(it) }
        }
    }

    private fun getSampleProducts(): List<ProductEntity> {
        return listOf(
            ProductEntity(
                id = "1",
                name = "Classic Black T-Shirt",
                description = "Premium cotton t-shirt with modern fit. Perfect for casual wear.",
                price = 29.99,
                originalPrice = 39.99,
                category = com.example.nextgenecommerce.data.models.ProductCategory.CLOTHING,
                subCategory = "T-Shirts",
                images = listOf("https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=400"),
                localImageName = "casual_tshirt.jpeg",
                sizes = listOf("XS", "S", "M", "L", "XL", "XXL"),
                colors = listOf("Black", "White", "Gray", "Navy"),
                rating = 4.5,
                reviewCount = 156,
                stock = 50,
                isFeatured = true,
                isNew = false,
                tags = listOf("casual", "cotton", "basic"),
                brand = "NextGen Fashion"
            ),
            ProductEntity(
                id = "2",
                name = "Summer Floral Dress",
                description = "Light and breezy summer dress with beautiful floral print. Perfect for warm weather.",
                price = 59.99,
                originalPrice = 79.99,
                category = com.example.nextgenecommerce.data.models.ProductCategory.CLOTHING,
                subCategory = "Dresses",
                images = listOf("https://images.unsplash.com/photo-1595777457583-95e059d581b8?w=400"),
                localImageName = "summer_dress.jpg",
                sizes = listOf("XS", "S", "M", "L", "XL"),
                colors = listOf("Floral Blue", "Floral Pink", "Floral Yellow"),
                rating = 4.8,
                reviewCount = 203,
                stock = 30,
                isFeatured = true,
                isNew = true,
                tags = listOf("summer", "dress", "floral"),
                brand = "NextGen Fashion"
            ),
            ProductEntity(
                id = "3",
                name = "Denim Jacket",
                description = "Classic denim jacket with modern fit. A timeless wardrobe essential.",
                price = 79.99,
                originalPrice = 99.99,
                category = com.example.nextgenecommerce.data.models.ProductCategory.CLOTHING,
                subCategory = "Jackets",
                images = listOf("https://images.unsplash.com/photo-1551028719-00167b16eac5?w=400"),
                localImageName = "denim_jacket.jpeg",
                sizes = listOf("S", "M", "L", "XL", "XXL"),
                colors = listOf("Light Blue", "Dark Blue", "Black"),
                rating = 4.6,
                reviewCount = 98,
                stock = 25,
                isFeatured = false,
                isNew = false,
                tags = listOf("denim", "jacket", "casual"),
                brand = "NextGen Fashion"
            ),
            ProductEntity(
                id = "4",
                name = "Formal White Shirt",
                description = "Crisp white formal shirt perfect for professional settings. Non-iron fabric.",
                price = 45.99,
                originalPrice = 59.99,
                category = com.example.nextgenecommerce.data.models.ProductCategory.CLOTHING,
                subCategory = "Shirts",
                images = listOf("https://images.unsplash.com/photo-1602810318383-e386cc2a3ccf?w=400"),
                localImageName = "formal_shirt.jpg",
                sizes = listOf("S", "M", "L", "XL", "XXL"),
                colors = listOf("White", "Light Blue", "Pink"),
                rating = 4.7,
                reviewCount = 145,
                stock = 40,
                isFeatured = false,
                isNew = false,
                tags = listOf("formal", "shirt", "office"),
                brand = "NextGen Fashion"
            ),
            ProductEntity(
                id = "5",
                name = "Premium Leather Jacket",
                description = "Genuine leather jacket with modern design. Premium quality construction.",
                price = 149.99,
                originalPrice = 199.99,
                category = com.example.nextgenecommerce.data.models.ProductCategory.CLOTHING,
                subCategory = "Jackets",
                images = listOf("https://images.unsplash.com/photo-1520975954732-35dd22299614?w=400"),
                localImageName = "leather_jacket.jpg",
                sizes = listOf("S", "M", "L", "XL"),
                colors = listOf("Black", "Brown", "Tan"),
                rating = 4.9,
                reviewCount = 312,
                stock = 15,
                isFeatured = true,
                isNew = true,
                tags = listOf("leather", "jacket", "premium"),
                brand = "NextGen Premium"
            ),
            ProductEntity(
                id = "6",
                name = "Elegant Maxi Dress",
                description = "Flowing maxi dress with elegant floral pattern. Perfect for special occasions.",
                price = 69.99,
                originalPrice = 89.99,
                category = com.example.nextgenecommerce.data.models.ProductCategory.CLOTHING,
                subCategory = "Dresses",
                images = listOf("https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?w=400"),
                localImageName = "floral_maxi_dress.jpg",
                sizes = listOf("XS", "S", "M", "L", "XL"),
                colors = listOf("Navy Floral", "Red Floral", "Green Floral"),
                rating = 4.7,
                reviewCount = 178,
                stock = 20,
                isFeatured = true,
                isNew = false,
                tags = listOf("maxi", "dress", "elegant"),
                brand = "NextGen Fashion"
            ),
            ProductEntity(
                id = "7",
                name = "Modern Sofa Set",
                description = "Comfortable 3-seater sofa with premium fabric. AR preview available.",
                price = 899.99,
                originalPrice = 1299.99,
                category = com.example.nextgenecommerce.data.models.ProductCategory.FURNITURE,
                subCategory = "Sofas",
                images = listOf("https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=400"),
                arModelUrl = "models/sofa_modern.glb",
                localImageName = "modern_sofa.jpg",
                sizes = listOf("Standard"),
                colors = listOf("Gray", "Beige", "Navy"),
                rating = 4.8,
                reviewCount = 89,
                stock = 10,
                isFeatured = true,
                isNew = true,
                tags = listOf("furniture", "sofa", "modern"),
                brand = "NextGen Home"
            ),
            ProductEntity(
                id = "8",
                name = "Wooden Coffee Table",
                description = "Handcrafted wooden coffee table with storage. AR preview available.",
                price = 249.99,
                originalPrice = 349.99,
                category = com.example.nextgenecommerce.data.models.ProductCategory.FURNITURE,
                subCategory = "Tables",
                images = listOf("https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=400"),
                arModelUrl = "models/coffee_table.glb",
                localImageName = "coffee_table.jpg",
                sizes = listOf("Standard"),
                colors = listOf("Oak", "Walnut", "White"),
                rating = 4.6,
                reviewCount = 67,
                stock = 15,
                isFeatured = false,
                isNew = false,
                tags = listOf("furniture", "table", "wooden"),
                brand = "NextGen Home"
            )
        )
    }
}
