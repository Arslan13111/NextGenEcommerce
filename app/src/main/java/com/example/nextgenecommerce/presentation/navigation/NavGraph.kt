package com.example.nextgenecommerce.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.nextgenecommerce.presentation.viewmodel.CartViewModel
import com.example.nextgenecommerce.presentation.screens.*
import com.example.nextgenecommerce.presentation.screens.auth.LoginScreen
import com.example.nextgenecommerce.presentation.screens.auth.RegisterScreen
import com.example.nextgenecommerce.presentation.screens.auth.SplashScreen
import com.example.nextgenecommerce.presentation.screens.cart.CartScreen
import com.example.nextgenecommerce.presentation.screens.cart.CheckoutScreen
import com.example.nextgenecommerce.presentation.screens.home.HomeScreen
import com.example.nextgenecommerce.presentation.screens.notifications.NotificationScreen
import com.example.nextgenecommerce.presentation.screens.orders.OrderDetailScreen
import com.example.nextgenecommerce.presentation.screens.orders.OrderSuccessScreen
import com.example.nextgenecommerce.presentation.screens.orders.OrdersScreen
import com.example.nextgenecommerce.presentation.screens.cart.SafepayCheckoutScreen
import com.example.nextgenecommerce.presentation.screens.product.AllProductsScreen
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
import com.example.nextgenecommerce.presentation.screens.admin.RevenueDetailScreen
import com.example.nextgenecommerce.presentation.screens.admin.SalesDetailScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(durationMillis = 300)) },
        exitTransition = { fadeOut(animationSpec = tween(durationMillis = 300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(durationMillis = 300)) },
        popExitTransition = { fadeOut(animationSpec = tween(durationMillis = 300)) }
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

        composable(Screen.AllProducts.route) {
            AllProductsScreen(navController = navController)
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

        composable(Screen.Checkout.route) { entry ->
            // Share CartViewModel with CartScreen so checkoutItems set there are visible here
            val cartEntry = remember(entry) {
                navController.getBackStackEntry(Screen.Cart.route)
            }
            val cartViewModel: CartViewModel = hiltViewModel(cartEntry)
            CheckoutScreen(
                navController = navController,
                cartViewModel = cartViewModel
            )
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

        composable(Screen.MyAccount.route) {
            com.example.nextgenecommerce.presentation.screens.profile.MyAccountScreen(navController = navController)
        }

        composable(Screen.DeliveryTerms.route) {
            com.example.nextgenecommerce.presentation.screens.profile.DeliveryTermsScreen(navController = navController)
        }

        composable(Screen.ProductReturn.route) {
            com.example.nextgenecommerce.presentation.screens.profile.ProductReturnScreen(navController = navController)
        }

        composable(Screen.DiscountCard.route) {
            com.example.nextgenecommerce.presentation.screens.profile.DiscountCardScreen(navController = navController)
        }

        composable(Screen.HelpCenter.route) {
            com.example.nextgenecommerce.presentation.screens.settings.HelpCenterScreen(navController = navController)
        }

        composable(Screen.About.route) {
            com.example.nextgenecommerce.presentation.screens.settings.AboutScreen(navController = navController)
        }

        // Try-On Screen
        composable(
            route = "tryon/{productId}/{imageIndex}",
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType },
                navArgument("imageIndex") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            val imageIndex = backStackEntry.arguments?.getInt("imageIndex") ?: 0
            TryOnScreen(
                navController = navController,
                productId = productId,
                imageIndex = imageIndex
            )
        }

        // Categories Screen (placeholder)
        composable("categories") {
            // TODO: Implement categories screen
            HomeScreen(navController = navController)
        }

        // Order Success Screen
        composable(
            route = Screen.OrderSuccess.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderSuccessScreen(navController = navController, orderId = orderId)
        }

        // Safepay WebView Checkout
        composable(
            route = Screen.SafepayCheckout.route,
            arguments = listOf(
                navArgument("orderId") { type = NavType.StringType },
                navArgument("amount") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            val amount = backStackEntry.arguments?.getString("amount") ?: "0"
            SafepayCheckoutScreen(
                navController = navController,
                orderId = orderId,
                amount = amount
            )
        }

        // Admin Screens
        composable(Screen.AdminLogin.route) {
            AdminLoginScreen(navController = navController)
        }

        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(navController = navController)
        }

        composable(Screen.RevenueDetail.route) {
            RevenueDetailScreen(navController = navController)
        }

        composable(Screen.SalesDetail.route) {
            SalesDetailScreen(navController = navController)
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
