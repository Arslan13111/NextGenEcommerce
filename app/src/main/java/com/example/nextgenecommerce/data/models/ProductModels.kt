package com.example.nextgenecommerce.data.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.nextgenecommerce.data.local.Converters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "products")
@TypeConverters(Converters::class)
data class ProductEntity(
    @PrimaryKey
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val originalPrice: Double = 0.0,
    val category: ProductCategory = ProductCategory.CLOTHING,
    val subCategory: String = "",
    val images: List<String> = emptyList(),
    val arModelUrl: String? = null,
    val lensId: String? = null,
    val localImageName: String = "",
    val sizes: List<String> = listOf("S", "M", "L", "XL"),
    val colors: List<String> = listOf("Black", "White", "Blue", "Red"),
    // Explicit color→imageUrl mapping. Populated by the admin form for all new products.
    // Empty for legacy products — those fall back to positional matching.
    val colorImages: Map<String, String> = emptyMap(),
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val stock: Int = 0,
    val inStock: Boolean = true,
    val isFeatured: Boolean = false,
    val isNew: Boolean = false,
    val tags: List<String> = emptyList(),
    val brand: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toSupabaseProduct(): SupabaseProduct {
        return SupabaseProduct(
            id = id,
            name = name,
            description = description,
            price = price,
            originalPrice = originalPrice,
            category = category.name,
            subCategory = subCategory,
            images = images,
            imageUrl = images.firstOrNull(),
            arModelUrl = arModelUrl,
            lensId = lensId,
            localImageName = localImageName,
            sizes = sizes,
            colors = colors,
            colorImages = colorImages,
            rating = rating,
            reviewCount = reviewCount,
            stock = stock,
            inStock = inStock,
            isFeatured = isFeatured,
            isNew = isNew,
            tags = tags,
            brand = brand,
            createdAt = createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }
}

enum class ProductCategory {
    CLOTHING,
    SHOES,
    ACCESSORIES,
    FURNITURE,
    ELECTRONICS,
    HOME_DECOR,
    JEWELRY,
    BAGS
}

data class Review(
    val id: String = "",
    val productId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userImageUrl: String? = null,
    val rating: Int = 5,
    val title: String = "",
    val comment: String = "",
    val images: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val helpful: Int = 0
)

@Serializable
data class SupabaseReview(
    val id: String = "",
    @SerialName("product_id")  val productId: String = "",
    @SerialName("user_id")     val userId: String = "",
    @SerialName("user_name")   val userName: String = "",
    val rating: Int = 5,
    val comment: String = "",
    @SerialName("created_at")  val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cart_items",
    indices = [Index(value = ["userId", "productId", "selectedSize", "selectedColor"], unique = true)]
)
data class CartItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String = "guest",
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val price: Double = 0.0,
    val originalPrice: Double = 0.0,
    val quantity: Int = 1,
    val selectedSize: String = "M",
    val selectedColor: String = "Black",
    val storeName: String = "Default Store",
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toSupabaseCartItem(): SupabaseCartItem {
        return SupabaseCartItem(
            userId = userId,
            productId = productId,
            productName = productName,
            productImage = productImage,
            price = price,
            originalPrice = originalPrice,
            quantity = quantity,
            selectedSize = selectedSize,
            selectedColor = selectedColor,
            storeName = storeName,
            addedAt = addedAt
        )
    }
}

@Serializable
data class SupabaseCartItem(
    val id: String = "",
    @SerialName("user_id")
    val userId: String = "",
    @SerialName("product_id")
    val productId: String = "",
    @SerialName("product_name")
    val productName: String = "",
    @SerialName("product_image")
    val productImage: String = "",
    val price: Double = 0.0,
    @SerialName("original_price")
    val originalPrice: Double = 0.0,
    val quantity: Int = 1,
    @SerialName("selected_size")
    val selectedSize: String = "M",
    @SerialName("selected_color")
    val selectedColor: String = "Black",
    @SerialName("store_name")
    val storeName: String = "Default Store",
    @SerialName("added_at")
    val addedAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toEntity(): CartItem {
        return CartItem(
            id = 0,
            userId = userId,
            productId = productId,
            productName = productName,
            productImage = productImage,
            price = price,
            originalPrice = originalPrice,
            quantity = quantity,
            selectedSize = selectedSize,
            selectedColor = selectedColor,
            storeName = storeName,
            addedAt = addedAt
        )
    }
}

@Entity(tableName = "wishlist_items")
data class WishlistItem(
    @PrimaryKey
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val price: Double = 0.0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.GENERAL,
    val isRead: Boolean = false,
    val productId: String? = null,
    val orderId: String? = null,
    val imageUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class NotificationType {
    GENERAL,
    ORDER_UPDATE,
    PROMOTION,
    PRODUCT_RESTOCK,
    PRICE_DROP,
    DELIVERY,
    PAYMENT
}

/**
 * Supabase product data class for API serialization
 * Maps to the 'products' table in Supabase
 */
@Serializable
data class SupabaseProduct(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    @SerialName("original_price")
    val originalPrice: Double = 0.0,
    val category: String = "CLOTHING",
    @SerialName("sub_category")
    val subCategory: String = "",
    val images: List<String> = emptyList(),
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("ar_model_url")
    val arModelUrl: String? = null,
    @SerialName("lens_id")
    val lensId: String? = null,
    @SerialName("local_image_name")
    val localImageName: String = "",
    val sizes: List<String> = listOf("S", "M", "L", "XL"),
    val colors: List<String> = listOf("Black", "White", "Blue", "Red"),
    @SerialName("color_images")
    val colorImages: Map<String, String> = emptyMap(),
    val rating: Double = 0.0,
    @SerialName("review_count")
    val reviewCount: Int = 0,
    val stock: Int = 0,
    @SerialName("in_stock")
    val inStock: Boolean = true,
    @SerialName("is_featured")
    val isFeatured: Boolean = false,
    @SerialName("is_new")
    val isNew: Boolean = false,
    val tags: List<String> = emptyList(),
    val brand: String = "",
    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Convert Supabase product to Room entity
     */
    fun toEntity(): ProductEntity {
        val categoryEnum = try {
            ProductCategory.valueOf(category)
        } catch (e: Exception) {
            ProductCategory.CLOTHING
        }

        // Use imageUrl as fallback if images array is empty
        val allImages = if (images.isNotEmpty()) images else listOfNotNull(imageUrl)

        return ProductEntity(
            id = id,
            name = name,
            description = description,
            price = price,
            originalPrice = originalPrice,
            category = categoryEnum,
            subCategory = subCategory,
            images = allImages,
            arModelUrl = arModelUrl,
            lensId = lensId,
            localImageName = localImageName,
            sizes = sizes,
            colors = colors,
            colorImages = colorImages,
            rating = rating,
            reviewCount = reviewCount,
            stock = stock,
            inStock = inStock,
            isFeatured = isFeatured,
            isNew = isNew,
            tags = tags,
            brand = brand,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
