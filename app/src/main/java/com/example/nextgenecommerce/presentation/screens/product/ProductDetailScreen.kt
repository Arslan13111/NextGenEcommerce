package com.example.nextgenecommerce.presentation.screens.product

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nextgenecommerce.data.models.CartItem
import com.example.nextgenecommerce.presentation.viewmodel.CartViewModel
import com.example.nextgenecommerce.presentation.viewmodel.ProductViewModel
import com.example.nextgenecommerce.presentation.viewmodel.WishlistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    navController: NavController,
    productId: String,
    productViewModel: ProductViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel(),
    wishlistViewModel: WishlistViewModel = hiltViewModel()
) {
    val product by productViewModel.selectedProduct.collectAsState()
    var selectedSize by remember { mutableStateOf("M") }
    var selectedColor by remember { mutableStateOf("Black") }
    var quantity by remember { mutableStateOf(1) }

    LaunchedEffect(productId) {
        productViewModel.getProductById(productId)
    }

    product?.let { prod ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Toggle wishlist */ }) {
                            Icon(Icons.Default.FavoriteBorder, "Wishlist")
                        }
                        IconButton(onClick = { /* Share */ }) {
                            Icon(Icons.Default.Share, "Share")
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Try On Button
                        OutlinedButton(
                            onClick = {
                                navController.navigate("tryon/${prod.id}")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.CameraAlt, "Try On", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Try On with AR", fontWeight = FontWeight.SemiBold)
                        }

                        // Add to Cart and Buy Now Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val cartItem = CartItem(
                                        productId = prod.id,
                                        productName = prod.name,
                                        productImage = prod.images.firstOrNull() ?: "",
                                        price = prod.price,
                                        quantity = quantity,
                                        selectedSize = selectedSize,
                                        selectedColor = selectedColor
                                    )
                                    cartViewModel.addToCart(cartItem)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AddShoppingCart, "Add to Cart", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", fontWeight = FontWeight.Medium)
                            }
                            Button(
                                onClick = {
                                    val cartItem = CartItem(
                                        productId = prod.id,
                                        productName = prod.name,
                                        productImage = prod.images.firstOrNull() ?: "",
                                        price = prod.price,
                                        quantity = quantity,
                                        selectedSize = selectedSize,
                                        selectedColor = selectedColor
                                    )
                                    cartViewModel.addToCart(cartItem)
                                    navController.navigate("checkout")
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Buy Now", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                AsyncImage(
                    model = prod.images.firstOrNull(),
                    contentDescription = prod.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = prod.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = prod.brand,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$${prod.price}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        if (prod.originalPrice > prod.price) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$${prod.originalPrice}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Size Selection
                    Text("Size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        prod.sizes.forEach { size ->
                            FilterChip(
                                selected = selectedSize == size,
                                onClick = { selectedSize = size },
                                label = { Text(size) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Color Selection
                    Text("Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        prod.colors.forEach { color ->
                            FilterChip(
                                selected = selectedColor == color,
                                onClick = { selectedColor = color },
                                label = { Text(color) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = prod.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
