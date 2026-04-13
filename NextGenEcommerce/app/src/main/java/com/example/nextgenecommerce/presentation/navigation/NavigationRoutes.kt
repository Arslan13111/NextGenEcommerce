package com.example.nextgenecommerce.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
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
    object Addresses : Screen("addresses")
    object Settings : Screen("settings")
    object ChangePassword : Screen("change_password")
    object HelpCenter : Screen("help_center")
    object About : Screen("about")

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
        Icons.Filled.Home
    )

    object Search : BottomNavItem(
        Screen.Search.route,
        "Search",
        Icons.Filled.Search
    )

    object Cart : BottomNavItem(
        Screen.Cart.route,
        "Cart",
        Icons.Filled.ShoppingCart
    )

    object Orders : BottomNavItem(
        Screen.Orders.route,
        "Orders",
        Icons.Filled.Receipt
    )

    object Profile : BottomNavItem(
        Screen.Profile.route,
        "Profile",
        Icons.Filled.Person
    )
}
