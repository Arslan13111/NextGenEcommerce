package com.example.nextgenecommerce.presentation.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nextgenecommerce.data.models.Order
import com.example.nextgenecommerce.data.models.OrderStatus
import com.example.nextgenecommerce.data.models.PaymentStatus
import com.example.nextgenecommerce.data.models.ProductEntity
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.AuthViewModel
import com.example.nextgenecommerce.presentation.viewmodel.OrderViewModel
import com.example.nextgenecommerce.presentation.viewmodel.ProductViewModel
import com.example.nextgenecommerce.util.ColorUtils
import com.example.nextgenecommerce.util.Resource
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    productViewModel: ProductViewModel = hiltViewModel(),
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val products by productViewModel.allProducts.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val isAdmin by authViewModel.isAdmin.collectAsState()
    val allOrders by orderViewModel.allAdminOrders.collectAsState()
    val productOperationState by productViewModel.productOperationState.collectAsState()
    val updateOrderStatusState by orderViewModel.updateOrderStatusState.collectAsState()
    val returnRefundState by orderViewModel.returnRefundState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Products", "Orders", "Returns")

    // Products tab state
    var productSearch by remember { mutableStateOf("") }
    var productFilter by remember { mutableStateOf("All") }

    // Orders tab state
    var orderStatusFilter by remember { mutableStateOf<OrderStatus?>(null) }
    var showStatusDialog by remember { mutableStateOf(false) }
    var orderToUpdate by remember { mutableStateOf<Order?>(null) }
    var expandedOrderId by remember { mutableStateOf<String?>(null) }

    // Delete product state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<String?>(null) }
    var lastProductOperation by remember { mutableStateOf("") } // "delete" | "color" | "update"

    // Access denied
    var showAccessDeniedDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isAdmin, currentUser) {
        if (currentUser != null && !isAdmin) showAccessDeniedDialog = true
    }

    LaunchedEffect(Unit) {
        orderViewModel.loadAllOrdersForAdmin()
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 2 || selectedTab == 3) {
            orderViewModel.loadAllOrdersForAdmin()
        }
    }

    LaunchedEffect(productOperationState) {
        when (productOperationState) {
            is Resource.Success -> {
                val msg = when (lastProductOperation) {
                    "delete" -> "Product deleted successfully"
                    "color"  -> "Color updated successfully"
                    else     -> "Product updated successfully"
                }
                snackbarHostState.showSnackbar(msg)
                productViewModel.resetProductOperationState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(
                    (productOperationState as Resource.Error).message ?: "Operation failed"
                )
                productViewModel.resetProductOperationState()
            }
            else -> {}
        }
    }

    LaunchedEffect(updateOrderStatusState) {
        when (updateOrderStatusState) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar("Order status updated")
                orderViewModel.resetUpdateOrderStatusState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(
                    (updateOrderStatusState as Resource.Error).message ?: "Failed to update status"
                )
                orderViewModel.resetUpdateOrderStatusState()
            }
            else -> {}
        }
    }

    LaunchedEffect(returnRefundState) {
        when (returnRefundState) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar("Refund processed — customer notified")
                orderViewModel.resetReturnRefundState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(
                    (returnRefundState as Resource.Error).message ?: "Failed to process refund"
                )
                orderViewModel.resetReturnRefundState()
            }
            else -> {}
        }
    }

    if (showAccessDeniedDialog) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Default.Block, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Access Denied") },
            text = { Text("You don't have admin privileges. Please login with an admin account.") },
            confirmButton = {
                Button(onClick = {
                    showAccessDeniedDialog = false
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }) { Text("Go to Login") }
            }
        )
    }

    if (showStatusDialog && orderToUpdate != null) {
        OrderStatusDialog(
            order = orderToUpdate!!,
            onDismiss = { showStatusDialog = false; orderToUpdate = null },
            onConfirm = { newStatus ->
                orderViewModel.updateOrderStatus(orderToUpdate!!.id, newStatus)
                showStatusDialog = false
                orderToUpdate = null
            },
            onAcceptReturn = { adminNote ->
                orderViewModel.acceptReturn(orderToUpdate!!.id, adminNote)
                showStatusDialog = false
                orderToUpdate = null
            },
            onProcessRefund = {
                orderViewModel.processReturnRefund(orderToUpdate!!.id)
                showStatusDialog = false
                orderToUpdate = null
            }
        )
    }

    if (showDeleteDialog && productToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Product") },
            text = { Text("Are you sure you want to delete this product? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        productToDelete?.let {
                            lastProductOperation = "delete"
                            productViewModel.deleteProductFromSupabase(it)
                        }
                        showDeleteDialog = false
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false; productToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Admin Panel", fontWeight = FontWeight.Bold)
                            currentUser?.let {
                                Text(
                                    it.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            productViewModel.syncProductsFromSupabase()
                            orderViewModel.loadAllOrdersForAdmin()
                        }) {
                            Icon(Icons.Default.Refresh, "Refresh")
                        }
                        IconButton(onClick = { navController.navigate(Screen.AddProduct.createRoute()) }) {
                            Icon(Icons.Default.Add, "Add Product")
                        }
                        IconButton(onClick = {
                            authViewModel.logout()
                            navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                        }) {
                            Icon(Icons.Default.Logout, "Logout")
                        }
                    }
                )
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 0.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            height = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    divider = {}
                ) {
                    val tabIcons = listOf(Icons.Default.Dashboard, Icons.Default.Inventory, Icons.Default.ShoppingCart, Icons.Default.Undo)
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Icon(
                                        tabIcons[index],
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    
                                    // Badges for Orders and Returns
                                    if (index == 2 && allOrders.isNotEmpty()) {
                                        val pending = allOrders.count { it.status == OrderStatus.PENDING }
                                        if (pending > 0) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                    Text("$pending", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 8.sp)
                                                }
                                            }
                                        }
                                    }
                                    if (index == 3 && allOrders.isNotEmpty()) {
                                        val returns = allOrders.count { it.status == OrderStatus.RETURN_REQUESTED }
                                        if (returns > 0) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF6A1B9A),
                                                modifier = Modifier.size(16.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                    Text("$returns", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 8.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 1) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Screen.AddProduct.createRoute()) },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Add Product") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> OverviewTab(
                modifier = Modifier.padding(padding),
                products = products,
                orders = allOrders,
                onNavigateToProducts = { selectedTab = 1 },
                onNavigateToOrders = { selectedTab = 2 },
                onAddProduct = { navController.navigate(Screen.AddProduct.createRoute()) },
                onNavigateToRevenue = { navController.navigate(Screen.RevenueDetail.route) },
                onNavigateToSales = { navController.navigate(Screen.SalesDetail.route) }
            )
            1 -> ProductsTab(
                modifier = Modifier.padding(padding),
                products = products,
                searchQuery = productSearch,
                onSearchChange = { productSearch = it },
                filter = productFilter,
                onFilterChange = { productFilter = it },
                onEdit = { navController.navigate(Screen.AddProduct.createRoute(it)) },
                onDelete = { productToDelete = it; showDeleteDialog = true },
                onColorChange = { productId, colorIndex, newColorName ->
                    products.find { it.id == productId }?.let { product ->
                        val oldColorName = product.colors.getOrElse(colorIndex) { "" }
                        val newColors = product.colors.toMutableList().also {
                            if (colorIndex in it.indices) it[colorIndex] = newColorName
                        }
                        val imageUrl = product.colorImages[oldColorName]
                            ?: product.images.getOrElse(colorIndex) { "" }
                        val newColorImages = product.colorImages.toMutableMap().also { m ->
                            m.remove(oldColorName)
                            m[newColorName] = imageUrl
                        }
                        lastProductOperation = "color"
                        productViewModel.updateProductInSupabase(
                            product.copy(colors = newColors, colorImages = newColorImages)
                        )
                    }
                }
            )
            2 -> OrdersTab(
                modifier = Modifier.padding(padding),
                orders = allOrders.filter {
                    it.status != OrderStatus.RETURN_REQUESTED && it.status != OrderStatus.RETURNED
                },
                statusFilter = orderStatusFilter,
                onFilterChange = { orderStatusFilter = it },
                expandedOrderId = expandedOrderId,
                onExpandToggle = { id -> expandedOrderId = if (expandedOrderId == id) null else id },
                onUpdateStatus = { order -> orderToUpdate = order; showStatusDialog = true }
            )
            3 -> ReturnsTab(
                modifier = Modifier.padding(padding),
                orders = allOrders,
                onSubmitReview = { orderId, reviewNote ->
                    orderViewModel.acceptReturn(orderId, reviewNote)
                },
                onRejectReturn = { orderId, reviewNote ->
                    orderViewModel.rejectReturn(orderId, reviewNote)
                },
                onReturnMoney = { orderId ->
                    orderViewModel.processReturnRefund(orderId)
                }
            )
        }
    }
}

// ─── Overview Tab ────────────────────────────────────────────────────────────

@Composable
private fun OverviewTab(
    modifier: Modifier,
    products: List<ProductEntity>,
    orders: List<Order>,
    onNavigateToProducts: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onAddProduct: () -> Unit,
    onNavigateToRevenue: () -> Unit,
    onNavigateToSales: () -> Unit
) {
    // ─── Revenue calculation logic ──────────────────────────────────────────
    // We only deduct if the payment has actually been refunded.
    // Rejected returns (RETURN_REJECTED) should NOT be removed from revenue.
    val totalRevenue = orders.filter {
        it.paymentStatus != PaymentStatus.FAILED &&
        it.status != OrderStatus.CANCELLED &&
        it.paymentStatus != PaymentStatus.REFUNDED
    }.sumOf { it.total }

    val pendingOrders = orders.count { it.status == OrderStatus.PENDING }
    val deliveredOrders = orders.count { it.status == OrderStatus.DELIVERED }
    val processingOrders = orders.count { it.status == OrderStatus.PROCESSING || it.status == OrderStatus.CONFIRMED || it.status == OrderStatus.SHIPPED }
    val recentOrders = orders.sortedByDescending { it.createdAt }.take(5)

    // ─── Sales stats ────────────────────────────────────────────────────────
    val activeOrders = orders.filter { it.status != OrderStatus.CANCELLED }
    val totalItemsSold = activeOrders.sumOf { it.items.sumOf { item -> item.quantity } }
    val returnedItemsCount = orders.filter { it.status == OrderStatus.RETURNED || it.paymentStatus == PaymentStatus.REFUNDED }
        .sumOf { it.items.sumOf { item -> item.quantity } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Revenue card
        item {
            RevenueCard(
                totalRevenue = totalRevenue, 
                orderCount = orders.size,
                onClick = onNavigateToRevenue
            )
        }

        // Sales card
        item {
            SalesCard(
                totalItemsSold = totalItemsSold,
                returnedItems = returnedItemsCount,
                deliveredCount = deliveredOrders,
                onClick = onNavigateToSales
            )
        }

        // Stats grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Products",
                    value = products.size.toString(),
                    icon = Icons.Default.Inventory,
                    iconColor = Color(0xFF6750A4),
                    subtitle = "${products.count { it.isFeatured }} featured",
                    onClick = onNavigateToProducts
                )
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Pending",
                    value = pendingOrders.toString(),
                    icon = Icons.Default.Schedule,
                    iconColor = Color(0xFFF57C00),
                    subtitle = "need attention",
                    onClick = onNavigateToOrders
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Processing",
                    value = processingOrders.toString(),
                    icon = Icons.Default.LocalShipping,
                    iconColor = Color(0xFF0288D1),
                    subtitle = "in transit",
                    onClick = onNavigateToOrders
                )
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Delivered",
                    value = deliveredOrders.toString(),
                    icon = Icons.Default.CheckCircle,
                    iconColor = Color(0xFF388E3C),
                    subtitle = "completed",
                    onClick = onNavigateToOrders
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Out of Stock",
                    value = products.count { it.stock <= 0 }.toString(),
                    icon = Icons.Default.Warning,
                    iconColor = Color(0xFFD32F2F),
                    subtitle = "restock needed",
                    onClick = onNavigateToProducts
                )
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "New Arrivals",
                    value = products.count { it.isNew }.toString(),
                    icon = Icons.Default.NewReleases,
                    iconColor = Color(0xFF7B1FA2),
                    subtitle = "tagged as new",
                    onClick = onNavigateToProducts
                )
            }
        }

        // Quick actions
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onAddProduct,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Product")
                        }
                        OutlinedButton(
                            onClick = onNavigateToOrders,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FormatListBulleted, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("All Orders")
                        }
                    }
                }
            }
        }

        // Recent orders
        if (recentOrders.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Orders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onNavigateToOrders) { Text("See All") }
                }
            }
            items(recentOrders) { order ->
                RecentOrderRow(order = order, onTap = onNavigateToOrders)
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun RevenueCard(totalRevenue: Double, orderCount: Int, onClick: () -> Unit) {
    val pkrFormat = NumberFormat.getNumberInstance(Locale.US)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TrendingUp,
                        null,
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Total Revenue",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "PKR ${pkrFormat.format(totalRevenue.toLong())}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "from $orderCount orders (excl. cancelled)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SalesCard(totalItemsSold: Int, returnedItems: Int, deliveredCount: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF512DA8))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF512DA8),
                            Color(0xFF673AB7).copy(alpha = 0.9f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Product Sales Overview",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            "$totalItemsSold",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White
                        )
                        Text(
                            "Items Sold",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$deliveredCount",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            "Delivered",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "$returnedItems",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFFFEB3B) // Yellowish for visibility on secondary
                        )
                        Text(
                            "Returns",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Total products quantity moved across all orders",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun DashboardStatCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Default.ChevronRight,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecentOrderRow(order: Order, onTap: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(orderStatusColor(order.status).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    orderStatusIcon(order.status),
                    null,
                    tint = orderStatusColor(order.status),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "#${order.orderNumber}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    dateFormat.format(Date(order.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "PKR ${order.total.toInt()}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                OrderStatusChip(status = order.status, compact = true)
            }
        }
    }
}

// ─── Products Tab ─────────────────────────────────────────────────────────────

@Composable
private fun ProductsTab(
    modifier: Modifier,
    products: List<ProductEntity>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filter: String,
    onFilterChange: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onColorChange: (productId: String, colorIndex: Int, newColorName: String) -> Unit,
) {
    val filters = listOf("All", "Featured", "New", "Out of Stock")
    val filtered = products
        .filter { p ->
            if (searchQuery.isBlank()) true
            else p.name.contains(searchQuery, ignoreCase = true) ||
                 p.brand.contains(searchQuery, ignoreCase = true)
        }
        .filter { p ->
            when (filter) {
                "Featured" -> p.isFeatured
                "New" -> p.isNew
                "Out of Stock" -> p.stock <= 0
                else -> true
            }
        }

    Column(modifier = modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search products...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Filter chips
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { onFilterChange(f) },
                    label = { Text(f) },
                    leadingIcon = if (filter == f) {
                        { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Result count
        Text(
            "${filtered.size} products",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Search,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No products found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { product ->
                    ProductAdminCard(
                        productName = product.name,
                        productImage = product.images.firstOrNull() ?: "",
                        price = product.price,
                        stock = product.stock,
                        isFeatured = product.isFeatured,
                        isNew = product.isNew,
                        brand = product.brand,
                        category = product.category.name,
                        colors = product.colors,
                        onEdit = { onEdit(product.id) },
                        onDelete = { onDelete(product.id) },
                        onColorChange = { colorIndex, newColorName ->
                            onColorChange(product.id, colorIndex, newColorName)
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

// ─── Orders Tab ───────────────────────────────────────────────────────────────

@Composable
private fun OrdersTab(
    modifier: Modifier,
    orders: List<Order>,
    statusFilter: OrderStatus?,
    onFilterChange: (OrderStatus?) -> Unit,
    expandedOrderId: String?,
    onExpandToggle: (String) -> Unit,
    onUpdateStatus: (Order) -> Unit
) {
    val statuses = listOf(null) + OrderStatus.values().filter {
        it != OrderStatus.RETURN_REQUESTED && it != OrderStatus.RETURNED
    }
    val filtered = if (statusFilter == null) orders else orders.filter { it.status == statusFilter }
    val sorted = filtered.sortedByDescending { it.createdAt }

    Column(modifier = modifier.fillMaxSize()) {
        // Status filter row
        LazyRow(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(statuses) { status ->
                val count = if (status == null) orders.size else orders.count { it.status == status }
                FilterChip(
                    selected = statusFilter == status,
                    onClick = { onFilterChange(status) },
                    label = {
                        Text(
                            if (status == null) "All ($count)"
                            else "${status.name.replace("_", " ")} ($count)"
                        )
                    },
                    colors = if (status != null) FilterChipDefaults.filterChipColors(
                        selectedContainerColor = orderStatusColor(status).copy(alpha = 0.2f),
                        selectedLabelColor = orderStatusColor(status)
                    ) else FilterChipDefaults.filterChipColors()
                )
            }
        }

        if (sorted.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Inventory,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No orders",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sorted, key = { it.id }) { order ->
                    AdminOrderCard(
                        order = order,
                        isExpanded = expandedOrderId == order.id,
                        onExpandToggle = { onExpandToggle(order.id) },
                        onUpdateStatus = { onUpdateStatus(order) }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun AdminOrderCard(
    order: Order,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onUpdateStatus: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row (always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpandToggle)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(orderStatusColor(order.status).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        orderStatusIcon(order.status),
                        null,
                        tint = orderStatusColor(order.status),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "#${order.orderNumber}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        dateFormat.format(Date(order.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "PKR ${order.total.toInt()}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    OrderStatusChip(status = order.status)
                }

                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expanded details
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(200))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Items list
                    Text(
                        "${order.items.size} item(s)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    order.items.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AsyncImage(
                                model = item.productImage,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.productName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Qty: ${item.quantity}  •  Size: ${item.selectedSize}  •  Color: ${ColorUtils.getColorDisplayName(item.selectedColor)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "PKR ${(item.price * item.quantity).toInt()}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Pricing breakdown
                    OrderPriceRow("Subtotal", "PKR ${order.subtotal.toInt()}")
                    OrderPriceRow("Shipping", "PKR ${order.shipping.toInt()}")
                    OrderPriceRow("Tax", "PKR ${order.tax.toInt()}")
                    OrderPriceRow("Total", "PKR ${order.total.toInt()}", bold = true)

                    // Shipping address
                    order.shippingAddress?.let { addr ->
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Text(
                            "Ship to",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            buildString {
                                append(addr.fullName)
                                if (addr.phone.isNotBlank()) append(" • ${addr.phone}")
                                append("\n${addr.addressLine1}")
                                if (addr.addressLine2.isNotBlank()) append(", ${addr.addressLine2}")
                                append("\n${addr.city}, ${addr.province} ${addr.postalCode}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Payment info
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Payment: ${order.paymentMethod.name.replace("_", " ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            order.paymentStatus.name,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = when (order.paymentStatus.name) {
                                "COMPLETED" -> Color(0xFF388E3C)
                                "FAILED" -> Color(0xFFD32F2F)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    // Update status button
                    Button(
                        onClick = onUpdateStatus,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Update Order Status")
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderPriceRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = if (bold) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    else MaterialTheme.typography.bodySmall,
            color = if (bold) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = if (bold) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    else MaterialTheme.typography.bodySmall,
            color = if (bold) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─── Status Update Dialog ─────────────────────────────────────────────────────

@Composable
private fun OrderStatusDialog(
    order: Order,
    onDismiss: () -> Unit,
    onConfirm: (OrderStatus) -> Unit,
    onAcceptReturn: (String) -> Unit = {},
    onProcessRefund: () -> Unit = {}
) {
    // ── Customer cancelled: admin only accepts ────────────────────────────────
    if (order.status == OrderStatus.CANCELLED) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.Cancel, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Order Cancelled by Customer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Order #${order.orderNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("The customer has cancelled this order. Accept the cancellation to acknowledge it.", style = MaterialTheme.typography.bodyMedium)
                    order.cancellationReason?.takeIf { it.isNotBlank() }?.let { reason ->
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Customer reason:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error)
                                Text(reason, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onConfirm(OrderStatus.CANCELLED) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Accept") }
            },
            dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Close") } }
        )
        return
    }

    // ── Normal status picker ──────────────────────────────────────────────────
    var selectedStatus by remember { mutableStateOf(order.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Update Order Status") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Order #${order.orderNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                OrderStatus.values().filter {
                    it != OrderStatus.RETURN_REQUESTED && it != OrderStatus.RETURNED
                }.forEach { status ->
                    val isSelected = selectedStatus == status
                    val statusColor = orderStatusColor(status)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) statusColor.copy(alpha = 0.12f) else Color.Transparent)
                            .border(width = if (isSelected) 1.5.dp else 0.dp, color = if (isSelected) statusColor else Color.Transparent, shape = RoundedCornerShape(10.dp))
                            .clickable { selectedStatus = status }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(orderStatusIcon(status), null, tint = if (isSelected) statusColor else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        Text(
                            status.name.replace("_", " "),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                            color = if (isSelected) statusColor else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = statusColor, modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedStatus) }) { Text("Update") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ─── Returns Tab ─────────────────────────────────────────────────────────────

@Composable
private fun ReturnsTab(
    modifier: Modifier,
    orders: List<Order>,
    onSubmitReview: (orderId: String, reviewNote: String) -> Unit,
    onRejectReturn: (orderId: String, reviewNote: String) -> Unit,
    onReturnMoney: (orderId: String) -> Unit
) {
    val pendingReturns = orders
        .filter { it.status == OrderStatus.RETURN_REQUESTED }
        .sortedByDescending { it.updatedAt }
    val reviewedReturns = orders
        .filter { it.status == OrderStatus.RETURNED || it.status == OrderStatus.RETURN_REJECTED }
        .sortedByDescending { it.updatedAt }

    if (pendingReturns.isEmpty() && reviewedReturns.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.AssignmentReturn, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                Text("No return requests", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (pendingReturns.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF6A1B9A).copy(alpha = 0.12f)) {
                        Text(
                            "Pending Review  ${pendingReturns.size}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF6A1B9A)
                        )
                    }
                }
            }
            items(pendingReturns, key = { it.id }) { order ->
                PendingReturnCard(
                    order = order, 
                    onSubmitReview = { note -> onSubmitReview(order.id, note) },
                    onRejectReturn = { note -> onRejectReturn(order.id, note) }
                )
            }
        }

        if (reviewedReturns.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF6D4C41).copy(alpha = 0.12f)) {
                        Text(
                            "Reviewed  ${reviewedReturns.size}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF6D4C41)
                        )
                    }
                }
            }
            items(reviewedReturns, key = { it.id }) { order ->
                ReviewedReturnCard(order = order, onReturnMoney = { onReturnMoney(order.id) })
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun PendingReturnCard(
    order: Order,
    onSubmitReview: (String) -> Unit,
    onRejectReturn: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault()) }
    var reviewNote by remember(order.id) { mutableStateOf("") }
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    if (selectedImageUrl != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { selectedImageUrl = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { selectedImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = selectedImageUrl,
                    contentDescription = "Full return image",
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { selectedImageUrl = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF6A1B9A).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AssignmentReturn, null, tint = Color(0xFF6A1B9A), modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("#${order.orderNumber}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    Text(dateFormat.format(Date(order.createdAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("PKR ${order.total.toInt()}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF6A1B9A).copy(alpha = 0.12f)) {
                        Text("Under Review", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF6A1B9A), fontSize = 9.sp)
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // Items preview
            Text("${order.items.size} item(s)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant)
            order.items.take(2).forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AsyncImage(
                        model = item.productImage, contentDescription = null,
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    Text(item.productName, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text("×${item.quantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // Customer's return reason
            Text("Customer's reason for return:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF6A1B9A).copy(alpha = 0.07f), modifier = Modifier.fillMaxWidth()) {
                Text(
                    order.returnReason ?: "No reason provided",
                    modifier = Modifier.padding(10.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (order.returnImages.isNotEmpty()) {
                Text("Product photos from customer:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(order.returnImages) { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedImageUrl = imageUrl },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Admin review text field
            Text("Your review response to customer:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
            OutlinedTextField(
                value = reviewNote,
                onValueChange = { reviewNote = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Describe what you found upon inspecting the product…", style = MaterialTheme.typography.bodySmall) },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val note = reviewNote.trim().ifBlank {
                            "We have reviewed your return request. Your payment will be refunded after receiving and inspecting the product."
                        }
                        onSubmitReview(note)
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A))
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Accept", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        val note = reviewNote.trim().ifBlank {
                            "We have reviewed your return request. Unfortunately, we cannot accept it as the provided details do not match our inspection criteria."
                        }
                        onRejectReturn(note)
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reject", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ReviewedReturnCard(
    order: Order,
    onReturnMoney: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault()) }
    val alreadyRefunded = order.paymentStatus == PaymentStatus.REFUNDED
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }

    if (selectedImageUrl != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { selectedImageUrl = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { selectedImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = selectedImageUrl,
                    contentDescription = "Full return image",
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { selectedImageUrl = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
                        .background(
                            if (order.status == OrderStatus.RETURN_REJECTED) Color(0xFFD32F2F).copy(alpha = 0.12f)
                            else Color(0xFF6D4C41).copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (order.status == OrderStatus.RETURN_REJECTED) Icons.Default.Cancel
                        else Icons.Default.Undo,
                        null,
                        tint = if (order.status == OrderStatus.RETURN_REJECTED) Color(0xFFD32F2F) else Color(0xFF6D4C41),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("#${order.orderNumber}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    Text(dateFormat.format(Date(order.createdAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("PKR ${order.total.toInt()}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    when {
                        alreadyRefunded -> {
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF388E3C).copy(alpha = 0.12f)) {
                                Text("Refunded", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF388E3C), fontSize = 9.sp)
                            }
                        }
                        order.status == OrderStatus.RETURN_REJECTED -> {
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFD32F2F).copy(alpha = 0.12f)) {
                                Text("Rejected", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFFD32F2F), fontSize = 9.sp)
                            }
                        }
                        else -> {
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF6D4C41).copy(alpha = 0.12f)) {
                                Text("Returned", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF6D4C41), fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // Customer's return reason
            Text("Customer's return reason:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF6D4C41).copy(alpha = 0.07f), modifier = Modifier.fillMaxWidth()) {
                Text(order.returnReason ?: "No reason provided", modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodyMedium)
            }

            if (order.returnImages.isNotEmpty()) {
                Text("Product photos from customer:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(order.returnImages) { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedImageUrl = imageUrl },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Admin's review note
            Text("Your review response:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth()) {
                Text(order.adminReturnNote ?: "—", modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodyMedium)
            }

            if (alreadyRefunded) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF388E3C).copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF388E3C), modifier = Modifier.size(18.dp))
                        Text("Refund has been processed successfully", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFF388E3C))
                    }
                }
            } else if (order.status == OrderStatus.RETURN_REJECTED) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFD32F2F).copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Cancel, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                        Text("Return request rejected", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color(0xFFD32F2F))
                    }
                }
            } else {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Text(
                    "If the product matches the defect described by the customer, return the payment.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onReturnMoney,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                ) {
                    Icon(Icons.Default.Payments, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Return Money", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─── Shared helpers ───────────────────────────────────────────────────────────

@Composable
fun OrderStatusChip(status: OrderStatus, compact: Boolean = false) {
    val color = orderStatusColor(status)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            status.name.replace("_", " "),
            modifier = Modifier.padding(
                horizontal = if (compact) 4.dp else 6.dp,
                vertical = if (compact) 2.dp else 3.dp
            ),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color,
            fontSize = if (compact) 9.sp else 10.sp
        )
    }
}

fun orderStatusColor(status: OrderStatus): Color = when (status) {
    OrderStatus.PENDING -> Color(0xFFF57C00)
    OrderStatus.CONFIRMED -> Color(0xFF0288D1)
    OrderStatus.PROCESSING -> Color(0xFF7B1FA2)
    OrderStatus.SHIPPED -> Color(0xFF00838F)
    OrderStatus.OUT_FOR_DELIVERY -> Color(0xFF558B2F)
    OrderStatus.DELIVERED -> Color(0xFF388E3C)
    OrderStatus.CANCELLED -> Color(0xFFD32F2F)
    OrderStatus.RETURN_REQUESTED -> Color(0xFF6A1B9A)
    OrderStatus.RETURNED -> Color(0xFF6D4C41)
    OrderStatus.RETURN_REJECTED -> Color(0xFFD32F2F)
}

fun orderStatusIcon(status: OrderStatus): ImageVector = when (status) {
    OrderStatus.PENDING -> Icons.Default.Schedule
    OrderStatus.CONFIRMED -> Icons.Default.CheckCircleOutline
    OrderStatus.PROCESSING -> Icons.Default.Refresh
    OrderStatus.SHIPPED -> Icons.Default.LocalShipping
    OrderStatus.OUT_FOR_DELIVERY -> Icons.Default.LocalShipping
    OrderStatus.DELIVERED -> Icons.Default.CheckCircle
    OrderStatus.CANCELLED -> Icons.Default.Cancel
    OrderStatus.RETURN_REQUESTED -> Icons.Default.AssignmentReturn
    OrderStatus.RETURNED -> Icons.Default.Undo
    OrderStatus.RETURN_REJECTED -> Icons.Default.Cancel
}

// ─── Product Admin Card ───────────────────────────────────────────────────────

@Composable
fun ProductAdminCard(
    productName: String,
    productImage: String,
    price: Double,
    stock: Int,
    isFeatured: Boolean = false,
    isNew: Boolean = false,
    brand: String = "",
    category: String = "",
    colors: List<String> = emptyList(),
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onColorChange: (colorIndex: Int, newColorName: String) -> Unit = { _, _ -> },
) {
    var colorPickerIndex by remember { mutableStateOf<Int?>(null) }

    colorPickerIndex?.let { idx ->
        ColorPickerDialog(
            currentColorName = colors.getOrElse(idx) { "" },
            onDismiss = { colorPickerIndex = null },
            onColorSelected = { newColor ->
                onColorChange(idx, newColor)
                colorPickerIndex = null
            }
        )
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                AsyncImage(
                    model = productImage,
                    contentDescription = productName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        productName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (brand.isNotBlank()) {
                    Text(brand, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "PKR ${price.toInt()}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stock badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (stock > 0) Color(0xFF388E3C).copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            if (stock > 0) "Stock: $stock" else "Out of Stock",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (stock > 0) Color(0xFF388E3C) else MaterialTheme.colorScheme.error
                        )
                    }
                    if (isFeatured) {
                        Icon(Icons.Default.Star, "Featured", tint = Color(0xFFF9A825), modifier = Modifier.size(14.dp))
                    }
                    if (isNew) {
                        Icon(Icons.Default.NewReleases, "New", tint = Color(0xFF7B1FA2), modifier = Modifier.size(14.dp))
                    }
                }

                // Tappable color swatches
                if (colors.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        colors.take(7).forEachIndexed { idx, colorName ->
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(colorNameToPreview(colorName))
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), CircleShape)
                                    .clickable { colorPickerIndex = idx }
                            )
                        }
                        if (colors.size > 7) {
                            Text(
                                "+${colors.size - 7}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Text(
                        "Tap color to edit",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Keep old StatCard for backward compatibility (not used in new dashboard but avoid compile errors)
@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
