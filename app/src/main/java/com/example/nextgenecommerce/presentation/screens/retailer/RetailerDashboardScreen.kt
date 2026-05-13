package com.example.nextgenecommerce.presentation.screens.retailer

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nextgenecommerce.data.models.Order
import com.example.nextgenecommerce.data.models.OrderStatus
import com.example.nextgenecommerce.data.models.SupabaseProduct
import com.example.nextgenecommerce.data.models.Retailer
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.AuthViewModel
import com.example.nextgenecommerce.presentation.viewmodel.RetailerViewModel
import com.example.nextgenecommerce.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetailerDashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    retailerViewModel: RetailerViewModel = hiltViewModel()
) {
    val currentUser  by authViewModel.currentUser.collectAsState()
    val storeState   by retailerViewModel.storeState.collectAsState()
    val store        by retailerViewModel.store.collectAsState()
    val products     by retailerViewModel.products.collectAsState()
    val orders       by retailerViewModel.orders.collectAsState()
    val actionState  by retailerViewModel.actionState.collectAsState()

    var selectedTab           by remember { mutableIntStateOf(0) }
    var showStoreDialog       by remember { mutableStateOf(false) }
    var isEditingStore        by remember { mutableStateOf(false) }
    val snackbarHostState     = remember { SnackbarHostState() }

    // ── Automatically prompt for store creation if none exists ────────────────
    LaunchedEffect(storeState) {
        if (storeState is Resource.Error) {
            showStoreDialog = true
            isEditingStore = false
        }
    }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is Resource.Success -> { snackbarHostState.showSnackbar("Success!"); retailerViewModel.resetActionState() }
            is Resource.Error   -> { snackbarHostState.showSnackbar(s.message ?: "Error"); retailerViewModel.resetActionState() }
            else -> {}
        }
    }

    if (showStoreDialog) {
        StoreProfileDialog(
            existingStore = if (isEditingStore) store else null,
            onConfirm = { name, desc, phone, addr ->
                if (isEditingStore) {
                    retailerViewModel.updateStore(name, desc, phone, addr)
                } else {
                    retailerViewModel.createStore(name, desc, phone, addr)
                }
                showStoreDialog = false
            },
            onDismiss = { if (store != null) showStoreDialog = false } // Only allow dismiss if store exists
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (store?.storeLogoUrl != null) {
                                    AsyncImage(
                                        model = store?.storeLogoUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        (store?.storeName ?: currentUser?.name ?: "S").take(1).uppercase(),
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                store?.storeName ?: (currentUser?.name ?: "Retailer"),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (store?.isVerified == true) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Verified, null, modifier = Modifier.size(14.dp), tint = Color(0xFF1E88E5))
                                    Text("Verified Retailer", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E88E5))
                                }
                            } else {
                                Text(if (store != null) "Active Store" else "Account Pending Store", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        isEditingStore = true
                        showStoreDialog = true 
                    }) {
                        Icon(Icons.Default.Settings, "Store Settings")
                    }
                    IconButton(onClick = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    }) {
                        Icon(Icons.Default.Logout, "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 1 && store != null) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Screen.RetailerAddProduct.createRoute()) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Add Product") }
                )
            }
        }
    ) { padding ->
        if (store == null && storeState is Resource.Loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (store == null && storeState is Resource.Error) {
             Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.Store, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("Welcome! Create your store to start selling.", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Button(onClick = { showStoreDialog = true; isEditingStore = false }) {
                        Text("Create My Store")
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                // ── Tabs ──────────────────────────────────────────────────────────
                val tabs = listOf(
                    Triple("Home", Icons.Default.Dashboard, Icons.Default.Dashboard),
                    Triple("Products", Icons.Default.Inventory2, Icons.Default.Inventory2),
                    Triple("Orders", Icons.Default.ShoppingBag, Icons.Default.ShoppingBag)
                )

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { positions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(positions[selectedTab]),
                            height = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    divider = { Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp) }
                ) {
                    tabs.forEachIndexed { i, (title, icon, selectedIcon) ->
                        Tab(
                            selected = selectedTab == i,
                            onClick = { selectedTab = i },
                            text = {
                                Text(
                                    title,
                                    style = if (selectedTab == i) MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    else MaterialTheme.typography.labelLarge
                                )
                            },
                            icon = {
                                Icon(
                                    if (selectedTab == i) selectedIcon else icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> HomeTab(
                            store    = store,
                            products = products,
                            orders   = orders,
                            onNavigateToOrders = { selectedTab = 2 },
                            onNavigateToProducts = { selectedTab = 1 }
                        )
                        1 -> ProductsTab(
                            state     = products,
                            onAdd     = { navController.navigate(Screen.RetailerAddProduct.createRoute()) },
                            onEdit    = { navController.navigate(Screen.RetailerAddProduct.createRoute(it.id)) },
                            onDelete  = { retailerViewModel.deleteProduct(it.id) },
                            onRefresh = { retailerViewModel.refreshProducts() }
                        )
                        2 -> OrdersTab(
                            orders          = orders,
                            onUpdateStatus  = { id, s -> retailerViewModel.updateOrderStatus(id, s) },
                            onAcceptReturn  = { id, note -> retailerViewModel.acceptReturn(id, note) },
                            onRejectReturn  = { id, note -> retailerViewModel.rejectReturn(id, note) },
                            onRefresh       = { retailerViewModel.refreshOrders() }
                        )
                    }
                }
            }
        }
    }
}

// ── Home Tab (Overview) ────────────────────────────────────────────────────────

@Composable
private fun HomeTab(
    store: Retailer?,
    products: Resource<List<SupabaseProduct>>,
    orders: Resource<List<Order>>,
    onNavigateToOrders: () -> Unit,
    onNavigateToProducts: () -> Unit
) {
    val orderList = (orders as? Resource.Success)?.data ?: emptyList()
    val productList = (products as? Resource.Success)?.data ?: emptyList()

    val pendingOrders = orderList.filter { it.status == OrderStatus.PENDING }
    val processingOrders = orderList.filter { it.status == OrderStatus.CONFIRMED || it.status == OrderStatus.PROCESSING }
    
    val totalSales = orderList.filter { it.status != OrderStatus.CANCELLED }.size
    val lowStockCount = productList.filter { it.stock < 5 }.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Store Overview",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    "Here's what's happening with your store today.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCardLarge(
                        modifier = Modifier.weight(1f),
                        label = "Total Revenue",
                        value = "PKR ${store?.totalRevenue?.toInt() ?: 0}",
                        icon = Icons.Default.Payments,
                        color = Color(0xFF2E7D32),
                        trend = "+12.5% this month"
                    )
                    StatCardLarge(
                        modifier = Modifier.weight(1f),
                        label = "Sales Count",
                        value = totalSales.toString(),
                        icon = Icons.Default.TrendingUp,
                        color = Color(0xFF1565C0),
                        trend = "+4.2% since yesterday"
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCardLarge(
                        modifier = Modifier.weight(1f),
                        label = "Active Products",
                        value = productList.size.toString(),
                        icon = Icons.Default.Inventory,
                        color = Color(0xFF6A1B9A),
                        onClick = onNavigateToProducts
                    )
                    StatCardLarge(
                        modifier = Modifier.weight(1f),
                        label = "Total Orders",
                        value = orderList.size.toString(),
                        icon = Icons.Default.Assignment,
                        color = Color(0xFFEF6C00),
                        onClick = onNavigateToOrders
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Weekly Sales Trend", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(20.dp))
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().height(120.dp).padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val heights = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.6f, 0.8f, 1.0f)
                        val days = listOf("M", "T", "W", "T", "F", "S", "S")
                        
                        heights.forEachIndexed { i, h ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .fillMaxHeight(h)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (i == 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(days[i], style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Immediate Tasks",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                
                if (pendingOrders.isNotEmpty()) {
                    TaskItem(
                        title = "${pendingOrders.size} Pending Orders",
                        desc = "Orders that need your confirmation",
                        icon = Icons.Default.NotificationImportant,
                        color = Color(0xFFF57C00),
                        onClick = onNavigateToOrders
                    )
                }

                if (lowStockCount > 0) {
                    TaskItem(
                        title = "$lowStockCount Low Stock Products",
                        desc = "Items with less than 5 units left",
                        icon = Icons.Default.Warning,
                        color = Color(0xFFD32F2F),
                        onClick = onNavigateToProducts
                    )
                }

                if (processingOrders.isNotEmpty()) {
                    TaskItem(
                        title = "${processingOrders.size} Orders to Ship",
                        desc = "Items ready for packing and delivery",
                        icon = Icons.Default.LocalShipping,
                        color = Color(0xFF1976D2),
                        onClick = onNavigateToOrders
                    )
                }
                
                if (pendingOrders.isEmpty() && lowStockCount == 0 && processingOrders.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DoneAll, null, tint = Color(0xFF2E7D32))
                            Spacer(Modifier.width(12.dp))
                            Text("All caught up! No urgent tasks.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun StatCardLarge(
    modifier: Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    trend: String? = null,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.clickable(enabled = onClick != null) { onClick?.invoke() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Column {
                Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (trend != null) {
                Text(trend, style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TaskItem(title: String, desc: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun ProductsTab(
    state: Resource<List<SupabaseProduct>>,
    onAdd: () -> Unit,
    onEdit: (SupabaseProduct) -> Unit,
    onDelete: (SupabaseProduct) -> Unit,
    onRefresh: () -> Unit
) {
    when (state) {
        is Resource.Loading -> CenteredIndicator()
        is Resource.Error   -> ErrorState(state.message ?: "Error", onRefresh)
        is Resource.Success -> {
            val list = state.data ?: emptyList()
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${list.size} Products listed",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Refresh")
                        }
                    }
                }

                if (list.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.Inventory, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Text("No products yet", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Button(onClick = onAdd) { Text("Add your first product") }
                        }
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(list, key = { it.id }) { product ->
                            RetailerProductCard(
                                product  = product,
                                onEdit   = { onEdit(product) },
                                onDelete = { onDelete(product) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RetailerProductCard(
    product: SupabaseProduct,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title   = { Text("Delete Product") },
            text    = { Text("Delete \"${product.name}\"? This cannot be undone.") },
            confirmButton   = { TextButton(onClick = { showConfirm = false; onDelete() }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton   = { TextButton(onClick = { showConfirm = false }) { Text("Cancel") } }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box {
                AsyncImage(
                    model = product.imageUrl ?: product.images.firstOrNull(),
                    contentDescription = product.name,
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                )
                if (product.stock < 5) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
                        color = Color.Red,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("LOW STOCK", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
            }
            
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("PKR ${product.price.toInt()}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    SmallChip(product.category)
                    Text("•", color = MaterialTheme.colorScheme.outline)
                    Text("${product.stock} in stock", style = MaterialTheme.typography.bodySmall, color = if (product.stock < 5) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { showConfirm = true }) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun OrdersTab(
    orders: Resource<List<Order>>,
    onUpdateStatus: (String, OrderStatus) -> Unit,
    onAcceptReturn: (String, String) -> Unit,
    onRejectReturn: (String, String) -> Unit,
    onRefresh: () -> Unit
) {
    when (orders) {
        is Resource.Loading -> CenteredIndicator()
        is Resource.Error   -> ErrorState(orders.message ?: "Error", onRefresh)
        is Resource.Success -> {
            val list = orders.data ?: emptyList()
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${list.size} Total Orders", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${list.filter { it.status == OrderStatus.PENDING }.size} Pending Action", style = MaterialTheme.typography.labelSmall, color = Color(0xFFF57C00))
                        }
                        IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null) }
                    }
                }

                if (list.isEmpty()) {
                    EmptyState("No orders for your store yet.")
                } else {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(list, key = { it.id }) { order -> 
                            RetailerOrderCard(
                                order = order, 
                                onUpdateStatus = onUpdateStatus,
                                onAcceptReturn = onAcceptReturn,
                                onRejectReturn = onRejectReturn
                            ) 
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RetailerOrderCard(
    order: Order, 
    onUpdateStatus: (String, OrderStatus) -> Unit,
    onAcceptReturn: (String, String) -> Unit,
    onRejectReturn: (String, String) -> Unit
) {
    val statusColor = when (order.status) {
        OrderStatus.PENDING, OrderStatus.CONFIRMED                        -> Color(0xFFF57F17)
        OrderStatus.PROCESSING, OrderStatus.PACKED                        -> Color(0xFF1565C0)
        OrderStatus.READY_FOR_PICKUP                                      -> Color(0xFF6A1B9A)
        OrderStatus.OUT_FOR_DELIVERY, OrderStatus.SHIPPED                 -> Color(0xFF00838F)
        OrderStatus.DELIVERED                                             -> Color(0xFF2E7D32)
        OrderStatus.CANCELLED, OrderStatus.RETURNED, OrderStatus.RETURN_REJECTED -> MaterialTheme.colorScheme.error
        else                                                              -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(order.orderNumber, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(order.createdAt.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(color = statusColor.copy(alpha = 0.12f), shape = RoundedCornerShape(20.dp)) {
                    Text(
                        order.status.name.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color    = statusColor
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape, modifier = Modifier.size(24.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Text("Customer ID: ${order.userId.take(8)}...", style = MaterialTheme.typography.bodySmall)
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                order.items.forEach { item ->
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("${item.quantity}× ${item.productName}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        Text("PKR ${(item.price * item.quantity).toInt()}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("Payment Method", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(order.paymentMethod.name.replace("_", " "), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Grand Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("PKR ${order.total.toInt()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)
                }
            }

            if (order.status != OrderStatus.DELIVERED && order.status != OrderStatus.CANCELLED && order.status != OrderStatus.RETURNED && order.status != OrderStatus.RETURN_REJECTED) {
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (order.status) {
                        OrderStatus.PENDING -> {
                            Button(
                                onClick = { onUpdateStatus(order.id, OrderStatus.CONFIRMED) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57F17)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Accept Order")
                            }
                            OutlinedButton(
                                onClick = { onUpdateStatus(order.id, OrderStatus.CANCELLED) },
                                modifier = Modifier.weight(0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Reject")
                            }
                        }
                        OrderStatus.CONFIRMED, OrderStatus.PROCESSING -> {
                            Button(
                                onClick = { onUpdateStatus(order.id, OrderStatus.PACKED) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Inventory, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Mark as Packed")
                            }
                        }
                        OrderStatus.PACKED -> {
                            Button(
                                onClick = { onUpdateStatus(order.id, OrderStatus.READY_FOR_PICKUP) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Ready for Pickup")
                            }
                        }
                        OrderStatus.RETURN_REQUESTED -> {
                            var showReturnDialog by remember { mutableStateOf(false) }
                            var isAccepting by remember { mutableStateOf(true) }

                            if (showReturnDialog) {
                                ReturnActionDialog(
                                    isAccept = isAccepting,
                                    onConfirm = { note -> 
                                        if (isAccepting) {
                                            onAcceptReturn(order.id, note)
                                        } else {
                                            onRejectReturn(order.id, note)
                                        }
                                        showReturnDialog = false
                                    },
                                    onDismiss = { showReturnDialog = false }
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { 
                                        isAccepting = true
                                        showReturnDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Accept Return")
                                }
                                OutlinedButton(
                                    onClick = { 
                                        isAccepting = false
                                        showReturnDialog = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Reject Return")
                                }
                            }
                        }
                        else -> {
                             Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "Waiting for courier to pick up",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReturnActionDialog(
    isAccept: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isAccept) "Accept Return" else "Reject Return") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter a note for the customer:")
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Return approved, please pack the item.") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(note) }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun StoreProfileDialog(
    existingStore: Retailer?,
    onConfirm: (String, String, String, String) -> Unit, 
    onDismiss: () -> Unit
) {
    var name    by remember { mutableStateOf(existingStore?.storeName ?: "") }
    var desc    by remember { mutableStateOf(existingStore?.storeDescription ?: "") }
    var phone   by remember { mutableStateOf(existingStore?.contactPhone ?: "") }
    var address by remember { mutableStateOf(existingStore?.storeAddress ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingStore != null) "Edit Store Profile" else "Create Your Store", fontWeight = FontWeight.Bold) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Store Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Contact Phone") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Store Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }
        },
        confirmButton = { 
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, desc, phone, address) }, 
                enabled = name.isNotBlank()
            ) { 
                Text(if (existingStore != null) "Save Changes" else "Create Store") 
            } 
        },
        dismissButton = { 
            if (existingStore != null) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun SmallChip(label: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(20.dp)) {
        Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun CenteredIndicator() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Inbox, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}
