package com.example.nextgenecommerce.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    // Auth
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")

    // Main
    object Home : Screen("home")
    object ProductList : Screen("product_list/{category}") {
        fun createRoute(category: String) = "product_list/$category"
    }
    object ProductDetail : Screen("product_detail/{productId}") {
        fun createRoute(productId: String) = "product_detail/$productId"
    }
    object Search : Screen("search")
    object AllProducts : Screen("all_products")

    // AR
    object ARViewer : Screen("ar_viewer/{productId}") {
        fun createRoute(productId: String) = "ar_viewer/$productId"
    }
    object TryOn : Screen("try_on/{productId}") {
        fun createRoute(productId: String) = "try_on/$productId"
    }

    // Shopping
    object Cart : Screen("cart")
    object Wishlist : Screen("wishlist")
    object Checkout : Screen("checkout")

    // Orders
    object Orders : Screen("orders")
    object OrderDetail : Screen("order_detail/{orderId}") {
        fun createRoute(orderId: String) = "order_detail/$orderId"
    }
    object OrderTracking : Screen("order_tracking/{orderId}") {
        fun createRoute(orderId: String) = "order_tracking/$orderId"
    }

    // Notifications
    object Notifications : Screen("notifications")

    // Profile
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object MyAccount : Screen("my_account")
    object DeliveryTerms : Screen("delivery_terms")
    object ProductReturn : Screen("product_return")
    object Addresses : Screen("addresses")
    object Settings : Screen("settings")
    object ChangePassword : Screen("change_password")
    object HelpCenter : Screen("help_center")
    object About : Screen("about")
    object DiscountCard : Screen("discount_card")

    // Payment
    object SafepayCheckout : Screen("safepay_checkout/{orderId}/{amount}") {
        fun createRoute(orderId: String, amount: String) = "safepay_checkout/$orderId/$amount"
    }

    // Order Success
    object OrderSuccess : Screen("order_success/{orderId}") {
        fun createRoute(orderId: String) = "order_success/$orderId"
    }

    // Admin
    object AdminLogin : Screen("admin_login")
    object AdminDashboard : Screen("admin_dashboard")
    object AddProduct : Screen("add_product?productId={productId}") {
        fun createRoute(productId: String? = null) = if (productId != null) {
            "add_product?productId=$productId"
        } else {
            "add_product"
        }
    }
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem(
        Screen.Home.route,
        "Home",
        Icons.Outlined.Home
    )

    object Wishlist : BottomNavItem(
        Screen.Wishlist.route,
        "Saved",
        Icons.Outlined.FavoriteBorder
    )

    object Search : BottomNavItem(
        Screen.Search.route,
        "Search",
        Icons.Filled.FormatListBulleted
    )

    object Cart : BottomNavItem(
        Screen.Cart.route,
        "Cart",
        Icons.Outlined.ShoppingCart
    )

    object Profile : BottomNavItem(
        Screen.Profile.route,
        "Profile",
        Icons.Outlined.Person
    )
}
