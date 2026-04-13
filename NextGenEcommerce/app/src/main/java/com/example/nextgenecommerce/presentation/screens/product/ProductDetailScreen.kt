package com.example.nextgenecommerce.presentation.screens.product

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.nextgenecommerce.data.models.CartItem
import com.example.nextgenecommerce.presentation.components.ImageCarousel
import com.example.nextgenecommerce.presentation.viewmodel.AuthViewModel
import com.example.nextgenecommerce.presentation.viewmodel.CartViewModel
import com.example.nextgenecommerce.presentation.viewmodel.ProductViewModel
import com.example.nextgenecommerce.presentation.screens.tryon.LiveARTryOnActivity
import com.example.nextgenecommerce.presentation.viewmodel.WishlistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    navController: NavController,
    productId: String,
    productViewModel: ProductViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel(),
    wishlistViewModel: WishlistViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val product by productViewModel.selectedProduct.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    var selectedSize by remember { mutableStateOf<String?>(null) }
    var selectedColor by remember { mutableStateOf<String?>(null) }
    var quantity by remember { mutableStateOf(1) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showLensLoginDialog by remember { mutableStateOf(false) }
    var currentCarouselPage by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(productId) {
        productViewModel.getProductById(productId)
    }

    // Login Required Dialog (for Try On)
    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = { showLoginDialog = false },
            title = { Text("Login Required") },
            text = { Text("Please login to use the Try On feature.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLoginDialog = false
                        navController.navigate("login")
                    }
                ) {
                    Text("Login")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoginDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Login Required Dialog (for Live AR)
    if (showLensLoginDialog) {
        AlertDialog(
            onDismissRequest = { showLensLoginDialog = false },
            title = { Text("Login Required") },
            text = { Text("Please login to use the Live Try On with AR feature.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLensLoginDialog = false
                        navController.navigate("login")
                    }
                ) {
                    Text("Login")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLensLoginDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    product?.let { prod ->
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                                // Check if user is logged in
                                if (currentUser == null) {
                                    // Show login dialog for guest users
                                    showLoginDialog = true
                                } else {
                                    // Proceed to try-on with currently viewed image
                                    navController.navigate("tryon/${prod.id}/$currentCarouselPage")
                                }
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

                        // Live Try On with AR Button (Snap Camera Kit)
                        if (!prod.lensId.isNullOrBlank()) {
                            Button(
                                onClick = {
                                    if (currentUser == null) {
                                        showLensLoginDialog = true
                                    } else {
                                        // Launch in-app AR camera with product lens
                                        val intent = Intent(context, LiveARTryOnActivity::class.java).apply {
                                            putExtra(LiveARTryOnActivity.EXTRA_LENS_ID, prod.lensId)
                                            putExtra(LiveARTryOnActivity.EXTRA_PRODUCT_NAME, prod.name)
                                        }
                                        context.startActivity(intent)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Icon(Icons.Default.Videocam, "Live AR", modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Live Try On with AR", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Add to Cart and Buy Now Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    // Validate size and color selection
                                    val size = selectedSize
                                    val color = selectedColor

                                    if (size == null || color == null) {
                                        scope.launch {
                                            val message = when {
                                                size == null && color == null ->
                                                    "Please select size and color"
                                                size == null ->
                                                    "Please select a size"
                                                else ->
                                                    "Please select a color"
                                            }
                                            snackbarHostState.showSnackbar(
                                                message = message,
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                        return@OutlinedButton
                                    }

                                    val cartItem = CartItem(
                                        productId = prod.id,
                                        productName = prod.name,
                                        productImage = prod.images.firstOrNull() ?: "",
                                        price = prod.price,
                                        originalPrice = prod.originalPrice,
                                        quantity = quantity,
                                        selectedSize = size,
                                        selectedColor = color,
                                        storeName = prod.brand.ifEmpty { "Default Store" }
                                    )
                                    cartViewModel.addToCart(cartItem)

                                    // Show snackbar to confirm item was added
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "${prod.name} added to cart",
                                            actionLabel = "View Cart",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            navController.navigate("cart")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AddShoppingCart, "Add to Cart", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", fontWeight = FontWeight.Medium)
                            }
                            Button(
                                onClick = {
                                    // Validate size and color selection
                                    val size = selectedSize
                                    val color = selectedColor

                                    if (size == null || color == null) {
                                        scope.launch {
                                            val message = when {
                                                size == null && color == null ->
                                                    "Please select size and color"
                                                size == null ->
                                                    "Please select a size"
                                                else ->
                                                    "Please select a color"
                                            }
                                            snackbarHostState.showSnackbar(
                                                message = message,
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                        return@Button
                                    }

                                    val cartItem = CartItem(
                                        productId = prod.id,
                                        productName = prod.name,
                                        productImage = prod.images.firstOrNull() ?: "",
                                        price = prod.price,
                                        originalPrice = prod.originalPrice,
                                        quantity = quantity,
                                        selectedSize = size,
                                        selectedColor = color,
                                        storeName = prod.brand.ifEmpty { "Default Store" }
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
                ImageCarousel(
                    images = prod.images,
                    contentDescription = prod.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    onPageChanged = { page -> currentCarouselPage = page }
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
                            val isSelected = selectedSize == size
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSize = size },
                                label = {
                                    Text(
                                        size,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 2.dp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Color Selection
                    Text("Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        prod.colors.forEach { color ->
                            val isSelected = selectedColor == color
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedColor = color },
                                label = {
                                    Text(
                                        color,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 2.dp
                                )
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
