package com.example.nextgenecommerce.presentation.screens.cart

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nextgenecommerce.data.models.WishlistItem
import com.example.nextgenecommerce.presentation.components.CartItemCard
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.CartViewModel
import com.example.nextgenecommerce.presentation.viewmodel.ProductViewModel
import com.example.nextgenecommerce.presentation.viewmodel.WishlistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController: NavController,
    viewModel: CartViewModel = hiltViewModel(),
    wishlistViewModel: WishlistViewModel = hiltViewModel(),
    productViewModel: ProductViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.refreshIfNeeded()
    }

    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val wishlistItems by wishlistViewModel.wishlistItems.collectAsStateWithLifecycle()
    val wishlistCount by wishlistViewModel.wishlistCount.collectAsStateWithLifecycle()
    val popularProducts by productViewModel.featuredProducts.collectAsStateWithLifecycle()

    // All items selected by default; kept in sync when cart items change
    var selectedItemIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    LaunchedEffect(cartItems) {
        // Add any new cart item ids that aren't tracked yet (default = selected)
        val currentIds = cartItems.map { it.id }.toSet()
        selectedItemIds = currentIds  // reset to all-selected whenever cart changes
    }

    val selectedItems = cartItems.filter { it.id in selectedItemIds }
    val selectedTotal = selectedItems.sumOf { it.price * it.quantity }
    val allSelected = cartItems.isNotEmpty() && selectedItemIds.size == cartItems.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Cart",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    // Favourites pill — always visible with count
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (wishlistCount > 0) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { navController.navigate(Screen.Wishlist.route) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (wishlistCount > 0) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favourites",
                                tint = if (wishlistCount > 0) Color(0xFFEF5350) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (wishlistCount > 0) "$wishlistCount" else "0",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (wishlistCount > 0) Color(0xFFEF5350) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        "Your cart is empty",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Add products to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { navController.navigate(Screen.Search.route) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                    ) {
                        Text("Browse products", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Animated select-all pill
                val pillBg by animateColorAsState(
                    targetValue = if (allSelected) Color(0xFF111111) else MaterialTheme.colorScheme.surfaceVariant,
                    animationSpec = tween(durationMillis = 250),
                    label = "pillBg"
                )
                val pillText by animateColorAsState(
                    targetValue = if (allSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                    animationSpec = tween(durationMillis = 250),
                    label = "pillText"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(pillBg)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                selectedItemIds = if (allSelected) emptySet()
                                else cartItems.map { it.id }.toSet()
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (allSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = pillText,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (allSelected) "Deselect all" else "Select all",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = pillText
                            )
                        }
                    }
                    Text(
                        text = "${selectedItemIds.size} of ${cartItems.size} selected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(cartItems, key = { it.id }) { item ->
                        val isFav = wishlistItems.any { it.productId == item.productId }
                        CartItemCard(
                            cartItem = item,
                            isChecked = item.id in selectedItemIds,
                            onCheckedChange = { checked ->
                                selectedItemIds = if (checked) selectedItemIds + item.id
                                else selectedItemIds - item.id
                            },
                            isFavourite = isFav,
                            onFavouriteToggle = {
                                if (isFav) {
                                    wishlistViewModel.removeFromWishlist(item.productId)
                                } else {
                                    wishlistViewModel.addToWishlist(
                                        WishlistItem(
                                            productId = item.productId,
                                            productName = item.productName,
                                            productImage = item.productImage,
                                            price = item.price
                                        )
                                    )
                                }
                            },
                            onQuantityIncrease = {
                                viewModel.updateQuantity(item.id, item.quantity + 1)
                            },
                            onQuantityDecrease = {
                                if (item.quantity > 1) viewModel.updateQuantity(item.id, item.quantity - 1)
                                else viewModel.removeFromCart(item)
                            },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        )
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }

                    // Popular items section
                    if (popularProducts.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "You may also like",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(popularProducts.take(8)) { product ->
                                        PopularProductCard(
                                            imageUrl = product.images.firstOrNull() ?: "",
                                            name = product.name,
                                            price = product.price,
                                            isNew = product.isNew,
                                            onClick = {
                                                navController.navigate(
                                                    Screen.ProductDetail.createRoute(product.id)
                                                )
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { navController.navigate(Screen.Search.route) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Show all",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom checkout bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Total",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = Color.White
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${selectedItems.size} of ${cartItems.size} items",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                "Rs ${selectedTotal.toInt()}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { navController.navigate(Screen.Checkout.route) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        enabled = selectedItems.isNotEmpty()
                    ) {
                        Text(
                            if (selectedItems.isEmpty()) "Select items to order"
                            else "Make an order  (${selectedItems.size})",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PopularProductCard(
    imageUrl: String,
    name: String,
    price: Double,
    isNew: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(155.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(195.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            if (isNew) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text("New", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            "Rs ${price.toInt()}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 14.sp
        )
        Text(
            name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp
        )
    }
}
