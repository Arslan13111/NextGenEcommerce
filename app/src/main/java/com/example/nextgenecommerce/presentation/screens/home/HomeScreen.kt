package com.example.nextgenecommerce.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nextgenecommerce.data.models.ProductEntity
import com.example.nextgenecommerce.presentation.components.ProductCard
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.CartViewModel
import com.example.nextgenecommerce.presentation.viewmodel.ProductViewModel
import com.example.nextgenecommerce.presentation.viewmodel.WishlistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    productViewModel: ProductViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel(),
    wishlistViewModel: WishlistViewModel = hiltViewModel()
) {
    val allProducts by productViewModel.allProducts.collectAsState()
    val cartItemCount by cartViewModel.cartItemCount.collectAsState()
    val wishlistItems by wishlistViewModel.wishlistItems.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "NextGen Store",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                    BadgedBox(
                        badge = {
                            if (cartItemCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        "$cartItemCount",
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = { navController.navigate(Screen.Cart.route) }) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Cart"
                            )
                        }
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = true,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Category, contentDescription = "Categories") },
                    label = { Text("Categories") },
                    selected = false,
                    onClick = { navController.navigate("categories") }
                )
                NavigationBarItem(
                    icon = {
                        BadgedBox(
                            badge = {
                                if (wishlistItems.isNotEmpty()) {
                                    Badge {
                                        Text("${wishlistItems.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = "Wishlist")
                        }
                    },
                    label = { Text("Wishlist") },
                    selected = false,
                    onClick = { navController.navigate(Screen.Wishlist.route) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Receipt, contentDescription = "Orders") },
                    label = { Text("Orders") },
                    selected = false,
                    onClick = { navController.navigate(Screen.Orders.route) }
                )
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(allProducts) { product ->
                ProductCard(
                    product = product,
                    onProductClick = {
                        navController.navigate(Screen.ProductDetail.createRoute(product.id))
                    },
                    onFavoriteClick = {
                        wishlistViewModel.toggleWishlistItem(product)
                    },
                    isFavorite = wishlistItems.any { it.productId == product.id }
                )
            }
        }
    }
}

