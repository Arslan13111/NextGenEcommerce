package com.example.nextgenecommerce.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.nextgenecommerce.presentation.screens.*
import com.example.nextgenecommerce.presentation.screens.auth.LoginScreen
import com.example.nextgenecommerce.presentation.screens.auth.RegisterScreen
import com.example.nextgenecommerce.presentation.screens.auth.SplashScreen
import com.example.nextgenecommerce.presentation.screens.cart.CartScreen
import com.example.nextgenecommerce.presentation.screens.cart.CheckoutScreen
import com.example.nextgenecommerce.presentation.screens.home.HomeScreen
import com.example.nextgenecommerce.presentation.screens.notifications.NotificationScreen
import com.example.nextgenecommerce.presentation.screens.orders.OrderDetailScreen
import com.example.nextgenecommerce.presentation.screens.orders.OrdersScreen
import com.example.nextgenecommerce.presentation.screens.product.ProductDetailScreen
import com.example.nextgenecommerce.presentation.screens.product.ProductListScreen
import com.example.nextgenecommerce.presentation.screens.profile.ProfileScreen
import com.example.nextgenecommerce.presentation.screens.search.SearchScreen
import com.example.nextgenecommerce.presentation.screens.settings.SettingsScreen
import com.example.nextgenecommerce.presentation.screens.tryon.TryOnScreen
import com.example.nextgenecommerce.presentation.screens.wishlist.WishlistScreen
import com.example.nextgenecommerce.presentation.screens.admin.AdminLoginScreen
import com.example.nextgenecommerce.presentation.screens.admin.AdminDashboardScreen
import com.example.nextgenecommerce.presentation.screens.admin.AddProductScreen
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Auth Screens
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }

        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }

        // Main Screens
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(Screen.Search.route) {
            SearchScreen(navController = navController)
        }

        composable(
            route = Screen.ProductList.route,
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: ""
            ProductListScreen(
                navController = navController,
                category = category
            )
        }

        composable(
            route = Screen.ProductDetail.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailScreen(
                navController = navController,
                productId = productId
            )
        }

        // Shopping Screens
        composable(Screen.Cart.route) {
            CartScreen(navController = navController)
        }

        composable(Screen.Wishlist.route) {
            WishlistScreen(navController = navController)
        }

        composable(Screen.Checkout.route) {
            CheckoutScreen(navController = navController)
        }

        // Orders Screens
        composable(Screen.Orders.route) {
            OrdersScreen(navController = navController)
        }

        composable(
            route = Screen.OrderDetail.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderDetailScreen(
                navController = navController,
                orderId = orderId
            )
        }

        // Notifications Screen
        composable(Screen.Notifications.route) {
            NotificationScreen(navController = navController)
        }

        // Profile Screens
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }

        composable(Screen.ChangePassword.route) {
            com.example.nextgenecommerce.presentation.screens.settings.ChangePasswordScreen(navController = navController)
        }

        composable(Screen.Addresses.route) {
            com.example.nextgenecommerce.presentation.screens.settings.ManageAddressesScreen(navController = navController)
        }

        composable(Screen.HelpCenter.route) {
            com.example.nextgenecommerce.presentation.screens.settings.HelpCenterScreen(navController = navController)
        }

        composable(Screen.About.route) {
            com.example.nextgenecommerce.presentation.screens.settings.AboutScreen(navController = navController)
        }

        // Try-On Screen
        composable(
            route = "tryon/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            TryOnScreen(
                navController = navController,
                productId = productId
            )
        }

        // Categories Screen (placeholder)
        composable("categories") {
            // TODO: Implement categories screen
            HomeScreen(navController = navController)
        }

        // Order Success Screen (placeholder)
        composable("order_success") {
            // TODO: Implement order success screen
            OrdersScreen(navController = navController)
        }

        // Admin Screens
        composable("admin_login") {
            val isAdminAuthenticated = remember { mutableStateOf(false) }
            AdminLoginScreen(
                navController = navController,
                onAdminAuthenticated = { isAdminAuthenticated.value = true }
            )
        }

        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(navController = navController)
        }

        composable(
            route = Screen.AddProduct.route,
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            AddProductScreen(
                navController = navController,
                productId = productId
            )
        }
    }
}
