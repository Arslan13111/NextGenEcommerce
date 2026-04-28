package com.example.nextgenecommerce.data.remote

import com.example.nextgenecommerce.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // Auth endpoints
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/google-login")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<AuthResponse>

    // Product endpoints
    @GET("products")
    suspend fun getProducts(
        @Query("category") category: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ProductsResponse>

    @GET("products/{id}")
    suspend fun getProductById(@Path("id") productId: String): Response<ProductResponse>

    @GET("products/search")
    suspend fun searchProducts(
        @Query("q") query: String,
        @Query("page") page: Int = 1
    ): Response<ProductsResponse>

    @GET("products/featured")
    suspend fun getFeaturedProducts(): Response<ProductsResponse>

    // Try-on endpoint
    @POST("api/try-on")
    suspend fun tryOnClothing(@Body request: TryOnRequest): Response<TryOnResponse>

    // Order endpoints
    @POST("orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): Response<OrderResponse>

    @GET("orders/{id}")
    suspend fun getOrderById(@Path("id") orderId: String): Response<OrderResponse>

    @GET("orders/user/{userId}")
    suspend fun getUserOrders(@Path("userId") userId: String): Response<OrdersResponse>

    @PUT("orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") orderId: String,
        @Body request: UpdateOrderStatusRequest
    ): Response<OrderResponse>

    // Review endpoints
    @POST("reviews")
    suspend fun submitReview(@Body review: Review): Response<ReviewResponse>

    @GET("reviews/product/{productId}")
    suspend fun getProductReviews(@Path("productId") productId: String): Response<ReviewsResponse>

    // User profile endpoints
    @GET("users/{id}")
    suspend fun getUserProfile(@Path("id") userId: String): Response<UserResponse>

    @PUT("users/{id}")
    suspend fun updateUserProfile(
        @Path("id") userId: String,
        @Body user: User
    ): Response<UserResponse>

    // Admin endpoints
    @POST("admin/products")
    suspend fun addProduct(@Body product: ProductEntity): Response<ProductResponse>

    @PUT("admin/products/{id}")
    suspend fun updateProduct(
        @Path("id") productId: String,
        @Body product: ProductEntity
    ): Response<ProductResponse>

    @DELETE("admin/products/{id}")
    suspend fun deleteProduct(@Path("id") productId: String): Response<BaseResponse>
}

// Request/Response models
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class GoogleLoginRequest(
    val idToken: String
)

data class AuthResponse(
    val success: Boolean,
    val token: String?,
    val user: User?,
    val message: String?
)

data class TryOnRequest(
    val userImage: String,
    val productImage: String,
    val productName: String? = null,
    val productColor: String? = null,
    val productType: String? = null
)

data class TryOnResponse(
    val success: Boolean,
    val resultImage: String,
    val message: String?,
    val description: String?
)

data class ProductsResponse(
    val success: Boolean,
    val products: List<ProductEntity>,
    val total: Int,
    val page: Int,
    val totalPages: Int
)

data class ProductResponse(
    val success: Boolean,
    val product: ProductEntity?
)

data class CreateOrderRequest(
    val userId: String,
    val items: List<OrderItem>,
    val shippingAddress: Address,
    val paymentMethod: String,
    val discountAmount: Double = 0.0,
    val cardTier: String = "SILVER"
)

data class OrderResponse(
    val success: Boolean,
    val order: Order?
)

data class OrdersResponse(
    val success: Boolean,
    val orders: List<Order>
)

data class UpdateOrderStatusRequest(
    val status: OrderStatus
)

data class ReviewResponse(
    val success: Boolean,
    val review: Review?
)

data class ReviewsResponse(
    val success: Boolean,
    val reviews: List<Review>,
    val averageRating: Double,
    val totalReviews: Int
)

data class UserResponse(
    val success: Boolean,
    val user: User?
)

data class BaseResponse(
    val success: Boolean,
    val message: String
)
