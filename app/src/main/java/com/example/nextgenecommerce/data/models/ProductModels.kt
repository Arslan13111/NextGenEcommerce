package com.example.nextgenecommerce.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.nextgenecommerce.data.local.Converters

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
    val localImageName: String = "",
    val sizes: List<String> = listOf("S", "M", "L", "XL"),
    val colors: List<String> = listOf("Black", "White", "Blue", "Red"),
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
)

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

@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
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
)

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
