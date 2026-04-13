package com.example.nextgenecommerce.presentation.screens.admin

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.AuthViewModel
import com.example.nextgenecommerce.presentation.viewmodel.ProductViewModel
import com.example.nextgenecommerce.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    productViewModel: ProductViewModel = hiltViewModel()
) {
    val products by productViewModel.allProducts.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val isAdmin by authViewModel.isAdmin.collectAsState()
    val productOperationState by productViewModel.productOperationState.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<String?>(null) }
    var showAccessDeniedDialog by remember { mutableStateOf(false) }

    // Check admin status on screen load
    LaunchedEffect(Unit) {
        authViewModel.checkAdminStatus()
    }

    // Show access denied if not admin
    LaunchedEffect(isAdmin, currentUser) {
        if (currentUser != null && !isAdmin) {
            showAccessDeniedDialog = true
        }
    }

    // Access Denied Dialog
    if (showAccessDeniedDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Access Denied") },
            text = {
                Text("You don't have admin privileges to access this page. Please login with an admin account.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAccessDeniedDialog = false
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                ) {
                    Text("Go to Login")
                }
            }
        )
    }

    // Show snackbar for product operations
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(productOperationState) {
        when (productOperationState) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar("Product deleted successfully")
                productViewModel.resetProductOperationState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(
                    (productOperationState as Resource.Error).message ?: "Operation failed"
                )
                productViewModel.resetProductOperationState()
            }
            else -> { }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Admin Dashboard")
                        currentUser?.let {
                            Text(
                                text = it.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(onClick = { productViewModel.syncProductsFromSupabase() }) {
                        Icon(Icons.Default.Refresh, "Refresh Products")
                    }
                    // Add product button
                    IconButton(onClick = { navController.navigate(Screen.AddProduct.createRoute()) }) {
                        Icon(Icons.Default.Add, "Add Product")
                    }
                    // Logout button
                    IconButton(onClick = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.Logout, "Logout")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.AddProduct.createRoute()) },
                icon = { Icon(Icons.Default.Add, "Add") },
                text = { Text("Add Product") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Statistics Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Products",
                    value = products.size.toString(),
                    icon = Icons.Default.Inventory,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Featured",
                    value = products.count { it.isFeatured }.toString(),
                    icon = Icons.Default.Star,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Out of Stock",
                    value = products.count { it.stock <= 0 }.toString(),
                    icon = Icons.Default.Warning,
                    modifier = Modifier.weight(1f)
                )
            }

            // Products List
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No products yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the + button to add your first product",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        ProductAdminCard(
                            productName = product.name,
                            productImage = product.images.firstOrNull() ?: "",
                            price = product.price,
                            stock = product.stock,
                            isFeatured = product.isFeatured,
                            isNew = product.isNew,
                            onEdit = {
                                navController.navigate(Screen.AddProduct.createRoute(product.id))
                            },
                            onDelete = {
                                productToDelete = product.id
                                showDeleteDialog = true
                            }
                        )
                    }
                    // Add bottom spacing for FAB
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog && productToDelete != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                icon = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("Delete Product") },
                text = {
                    Text("Are you sure you want to delete this product? This action cannot be undone.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            productToDelete?.let { id ->
                                productViewModel.deleteProductFromSupabase(id)
                            }
                            showDeleteDialog = false
                            productToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = {
                        showDeleteDialog = false
                        productToDelete = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProductAdminCard(
    productName: String,
    productImage: String,
    price: Double,
    stock: Int,
    isFeatured: Boolean = false,
    isNew: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image
            Card(
                modifier = Modifier.size(70.dp),
                shape = MaterialTheme.shapes.small
            ) {
                AsyncImage(
                    model = productImage,
                    contentDescription = productName,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Product Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (isFeatured) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Featured",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (isNew) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.NewReleases,
                            contentDescription = "New",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$${String.format("%.2f", price)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (stock > 0) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (stock > 0) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (stock > 0) "In Stock: $stock" else "Out of Stock",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (stock > 0) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.error
                    )
                }
            }

            // Action Buttons
            Column {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        "Edit",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
