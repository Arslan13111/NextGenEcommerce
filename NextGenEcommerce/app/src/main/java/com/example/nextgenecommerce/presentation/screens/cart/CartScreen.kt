package com.example.nextgenecommerce.presentation.screens.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nextgenecommerce.data.models.CartItem
import com.example.nextgenecommerce.presentation.components.CartItemCard
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.CartViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    navController: NavController,
    viewModel: CartViewModel = hiltViewModel()
) {
    // Ensure cart data is for the current user (handles restored nav state after user switch)
    LaunchedEffect(Unit) {
        viewModel.refreshIfNeeded()
    }

    val cartItems by viewModel.cartItems.collectAsState()
    val cartTotal by viewModel.cartTotal.collectAsState()
    val shippingAmount = viewModel.getShippingAmount()

    var selectedItems by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var selectedStores by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectAll by remember { mutableStateOf(false) }

    // Group items by store
    val groupedItems = cartItems.groupBy { it.storeName }

    // Calculate selected items total
    val selectedTotal = remember(selectedItems, cartItems) {
        cartItems.filter { selectedItems.contains(it.id) }
            .sumOf { it.price * it.quantity }
    }

    // Calculate shipping for selected items
    val selectedShipping = if (selectedTotal > 50) 0.0 else if (selectedTotal > 0) 5.99 else 0.0

    // Update selectAll based on selected items
    LaunchedEffect(selectedItems, cartItems) {
        selectAll = cartItems.isNotEmpty() && selectedItems.size == cartItems.size
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Cart",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    )
                },
                actions = {
                    if (selectedItems.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                selectedItems.forEach { itemId ->
                                    cartItems.find { it.id == itemId }?.let { item ->
                                        viewModel.removeFromCart(item)
                                    }
                                }
                                selectedItems = emptySet()
                                selectedStores = emptySet()
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete selected items",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Your cart is empty",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Add products to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(padding),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    groupedItems.forEach { (storeName, items) ->
                        item {
                            // Store Header with Checkbox
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = items.all { selectedItems.contains(it.id) },
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            selectedItems = selectedItems + items.map { it.id }
                                            selectedStores = selectedStores + storeName
                                        } else {
                                            selectedItems = selectedItems - items.map { it.id }.toSet()
                                            selectedStores = selectedStores - storeName
                                        }
                                    }
                                )
                                Icon(
                                    Icons.Default.Store,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    storeName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(items) { item ->
                            CartItemCard(
                                cartItem = item,
                                isSelected = selectedItems.contains(item.id),
                                onSelectionChange = { selected ->
                                    selectedItems = if (selected) {
                                        selectedItems + item.id
                                    } else {
                                        selectedItems - item.id
                                    }
                                },
                                onQuantityIncrease = {
                                    viewModel.updateQuantity(item.id, item.quantity + 1)
                                },
                                onQuantityDecrease = {
                                    if (item.quantity > 1) {
                                        viewModel.updateQuantity(item.id, item.quantity - 1)
                                    }
                                },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // Bottom Checkout Section
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Price Summary
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Subtotal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "$${"%.2f".format(selectedTotal)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Shipping Fee",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "$${"%.2f".format(selectedShipping)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp
                                )
                            }

                            Divider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Total",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp
                                )
                                Text(
                                    "$${"%.2f".format(selectedTotal + selectedShipping)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 18.sp
                                )
                            }
                        }

                        // Compact Checkout Button
                        Button(
                            onClick = {
                                if (selectedItems.isNotEmpty()) {
                                    navController.navigate(Screen.Checkout.route)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            enabled = selectedItems.isNotEmpty(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Checkout ${if (selectedItems.isNotEmpty()) "(${selectedItems.size})" else ""}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
