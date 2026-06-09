package com.example.nextgenecommerce.presentation.screens.retailer

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.nextgenecommerce.data.models.Order
import com.example.nextgenecommerce.data.models.OrderStatus
import com.example.nextgenecommerce.data.models.PaymentMethod
import com.example.nextgenecommerce.data.models.PaymentStatus
import com.example.nextgenecommerce.data.models.Retailer
import com.example.nextgenecommerce.data.models.SupabaseProduct
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.AuthViewModel
import com.example.nextgenecommerce.presentation.viewmodel.RetailerViewModel
import com.example.nextgenecommerce.util.Resource
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// ─── Brand colours (consistent across tabs) ───────────────────────────────────
private val GreenDark   = Color(0xFF1B5E20)
private val GreenMid    = Color(0xFF2E7D32)
private val BlueDark    = Color(0xFF0D47A1)
private val BlueMid     = Color(0xFF1565C0)
private val OrangeDark  = Color(0xFFE65100)
private val OrangeMid   = Color(0xFFF57C00)
private val PurpleMid   = Color(0xFF6A1B9A)
private val TealMid     = Color(0xFF00695C)
private val RedMid      = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetailerDashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    retailerViewModel: RetailerViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val storeState  by retailerViewModel.storeState.collectAsState()
    val store       by retailerViewModel.store.collectAsState()
    val products    by retailerViewModel.products.collectAsState()
    val orders      by retailerViewModel.orders.collectAsState()
    val actionState by retailerViewModel.actionState.collectAsState()

    var selectedTab       by remember { mutableIntStateOf(0) }
    var showStoreDialog   by remember { mutableStateOf(false) }
    var isEditingStore    by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val pendingCount by remember(orders) {
        derivedStateOf {
            ((orders as? Resource.Success)?.data ?: emptyList())
                .count { it.status == OrderStatus.PENDING }
        }
    }

    val returnCount by remember(orders) {
        derivedStateOf {
            ((orders as? Resource.Success)?.data ?: emptyList())
                .count { it.status == OrderStatus.RETURN_REQUESTED }
        }
    }

    LaunchedEffect(storeState) {
        if (storeState is Resource.Error) { showStoreDialog = true; isEditingStore = false }
    }
    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is Resource.Success -> { snackbarHostState.showSnackbar("Done!"); retailerViewModel.resetActionState() }
            is Resource.Error   -> { snackbarHostState.showSnackbar(s.message ?: "Error"); retailerViewModel.resetActionState() }
            else -> {}
        }
    }

    if (showStoreDialog) {
        StoreProfileDialog(
            existingStore = if (isEditingStore) store else null,
            onConfirm = { name, desc, phone, addr, images ->
                if (isEditingStore) retailerViewModel.updateStore(name, desc, phone, addr)
                else retailerViewModel.createStore(name, desc, phone, addr, images)
                showStoreDialog = false
            },
            onDismiss = { if (store != null) showStoreDialog = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RetailerTopBar(
                store       = store,
                userName    = currentUser?.name,
                onSettings  = { isEditingStore = true; showStoreDialog = true },
                onLogout    = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 1 && store?.isVerified == true) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Screen.RetailerAddProduct.createRoute()) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Add Product", fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    ) { padding ->
        when {
            store == null && storeState is Resource.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            store?.isRejected == true -> {
                RejectedScreen(
                    role = "Retailer",
                    onLogout = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    },
                    modifier = Modifier.padding(padding)
                )
            }
            store == null -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CreateStorePrompt(onCreate = { showStoreDialog = true; isEditingStore = false })
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    // ── Tab Row ──────────────────────────────────────────────────
                    RetailerTabRow(
                        selectedTab  = selectedTab,
                        pendingCount = pendingCount,
                        returnCount  = returnCount,
                        onTabSelect  = { selectedTab = it }
                    )

                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            0 -> HomeTab(
                                store      = store,
                                products   = products,
                                orders     = orders,
                                isVerified = store?.isVerified == true,
                                onNavigateToOrders   = { selectedTab = 2 },
                                onNavigateToProducts = { selectedTab = 1 },
                                onAddProduct = { navController.navigate(Screen.RetailerAddProduct.createRoute()) }
                            )
                            1 -> ProductsTab(
                                state      = products,
                                isVerified = store?.isVerified == true,
                                onAdd      = { navController.navigate(Screen.RetailerAddProduct.createRoute()) },
                                onEdit     = { navController.navigate(Screen.RetailerAddProduct.createRoute(it.id)) },
                                onDelete   = { retailerViewModel.deleteProduct(it.id) },
                                onRefresh  = { retailerViewModel.refreshProducts() }
                            )
                            2 -> OrdersTab(
                                orders         = orders,
                                onUpdateStatus = { id, s -> retailerViewModel.updateOrderStatus(id, s) },
                                onRefresh      = { retailerViewModel.refreshOrders() }
                            )
                            3 -> ReturnsTab(
                                orders            = orders,
                                onAcceptReturn    = { id, note -> retailerViewModel.approveReturnRequest(id, note) },
                                onRejectReturn    = { id, note -> retailerViewModel.rejectReturn(id, note) },
                                onVerifyAndRefund = { id -> retailerViewModel.verifyAndRefund(id) },
                                onRefresh         = { retailerViewModel.refreshOrders() }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Top Bar ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RetailerTopBar(
    store: Retailer?,
    userName: String?,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Store avatar
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (store?.storeLogoUrl != null) {
                            AsyncImage(
                                model = store.storeLogoUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                (store?.storeName ?: userName ?: "R").take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Column {
                    Text(
                        store?.storeName ?: (userName ?: "My Store"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (store?.isVerified == true) {
                            Icon(Icons.Default.Verified, null, modifier = Modifier.size(12.dp), tint = Color(0xFF1E88E5))
                            Text("Verified", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E88E5))
                        } else if (store != null) {
                            Box(
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(OrangeMid)
                            )
                            Text("Pending approval", style = MaterialTheme.typography.labelSmall, color = OrangeMid)
                        }
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, "Store Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onLogout) {
                Icon(Icons.Default.Logout, "Logout", tint = RedMid)
            }
        }
    )
}

// ─── Tab Row ──────────────────────────────────────────────────────────────────

@Composable
private fun RetailerTabRow(
    selectedTab: Int,
    pendingCount: Int,
    returnCount: Int,
    onTabSelect: (Int) -> Unit
) {
    data class TabItem(val label: String, val icon: ImageVector, val badge: Int = 0)

    val tabs = listOf(
        TabItem("Home",     Icons.Default.Dashboard),
        TabItem("Products", Icons.Default.Inventory2),
        TabItem("Orders",   Icons.Default.Receipt, pendingCount),
        TabItem("Returns",  Icons.Default.AssignmentReturn, returnCount)
    )

    ScrollableTabRow(
        edgePadding = 0.dp,
        selectedTabIndex = selectedTab,
        containerColor   = MaterialTheme.colorScheme.surface,
        contentColor     = MaterialTheme.colorScheme.primary,
        indicator        = { positions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(positions[selectedTab]),
                height   = 3.dp,
                color    = MaterialTheme.colorScheme.primary
            )
        },
        divider = { Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp) }
    ) {
        tabs.forEachIndexed { i, tab ->
            Tab(
                selected = selectedTab == i,
                onClick  = { onTabSelect(i) },
                selectedContentColor   = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Icon(tab.icon, null, modifier = Modifier.size(18.dp))
                    Text(
                        tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selectedTab == i) FontWeight.ExtraBold else FontWeight.Medium
                    )
                    if (tab.badge > 0) {
                        Surface(
                            shape = CircleShape,
                            color = RedMid,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("${tab.badge}", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Create Store Prompt ──────────────────────────────────────────────────────

@Composable
private fun CreateStorePrompt(onCreate: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Surface(
            modifier = Modifier.size(96.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Storefront, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Set Up Your Store", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold))
            Text(
                "Create your store profile to start selling on NextGen Ecommerce.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Button(
            onClick = onCreate,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Create My Store", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

// ─── Home Tab ─────────────────────────────────────────────────────────────────

@Composable
private fun HomeTab(
    store: Retailer?,
    products: Resource<List<SupabaseProduct>>,
    orders: Resource<List<Order>>,
    isVerified: Boolean,
    onNavigateToOrders: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onAddProduct: () -> Unit
) {
    val pkrFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val orderList   = remember(orders) { (orders as? Resource.Success)?.data ?: emptyList() }
    val productList = remember(products) { (products as? Resource.Success)?.data ?: emptyList() }

    val pendingOrders    = remember(orderList) { orderList.filter { it.status == OrderStatus.PENDING } }
    val processingOrders = remember(orderList) { orderList.filter { it.status == OrderStatus.CONFIRMED || it.status == OrderStatus.PROCESSING } }
    val deliveredOrders  = remember(orderList) { orderList.filter { it.status == OrderStatus.DELIVERED } }
    val deliveredRevenue = remember(deliveredOrders) { deliveredOrders.sumOf { it.total } }
    val totalSales       = remember(orderList) { orderList.count { it.status != OrderStatus.CANCELLED } }
    val lowStockCount    = remember(productList) { productList.count { it.stock in 1..4 } }
    val outOfStockCount  = remember(productList) { productList.count { it.stock <= 0 } }
    val recentOrders     = remember(orderList) { orderList.sortedByDescending { it.createdAt }.take(3) }

    // Build a 7-day order count for the trend chart
    val calendar = remember { Calendar.getInstance() }
    val weekTrend = remember(orderList) {
        val cal = Calendar.getInstance()
        (6 downTo 0).map { daysAgo ->
            cal.time = Date()
            cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            val dayStart = cal.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
            val dayEnd   = cal.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis
            orderList.count { it.createdAt in dayStart..dayEnd }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Pending approval banner
        if (!isVerified) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = OrangeDark.copy(alpha = 0.07f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OrangeDark.copy(alpha = 0.25f))
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = CircleShape, color = OrangeDark.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.HourglassEmpty, null, modifier = Modifier.size(20.dp), tint = OrangeDark)
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Awaiting Admin Approval", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = OrangeDark)
                            Text(
                                "Your store is under review. Product management will unlock once approved.",
                                style = MaterialTheme.typography.bodySmall,
                                color = OrangeDark.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        // Revenue hero card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = if (!isVerified) 0.dp else 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF1A237E), Color(0xFF283593), Color(0xFF1565C0)))
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Delivered Revenue", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.75f))
                            Text(
                                "PKR ${pkrFormat.format(deliveredRevenue.toLong())}",
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                        }
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.15f), modifier = Modifier.size(52.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(28.dp), tint = Color.White)
                            }
                        }
                    }
                    Divider(color = Color.White.copy(alpha = 0.15f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MiniStat("Orders", totalSales.toString(), Color.White)
                        MiniStat("Delivered", deliveredOrders.size.toString(), Color(0xFFA5D6A7))
                        MiniStat("Pending", pendingOrders.size.toString(), Color(0xFFFFCC80))
                        MiniStat("Processing", processingOrders.size.toString(), Color(0xFF90CAF9))
                    }
                }
            }
        }

        // Stats 2×2 grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeStatCard(
                    modifier = Modifier.weight(1f),
                    label = "Products", value = productList.size.toString(),
                    icon = Icons.Default.Inventory2, color = PurpleMid,
                    sub = if (outOfStockCount > 0) "$outOfStockCount out of stock" else "All in stock",
                    subColor = if (outOfStockCount > 0) RedMid else GreenMid,
                    onClick = onNavigateToProducts
                )
                HomeStatCard(
                    modifier = Modifier.weight(1f),
                    label = "Total Orders", value = orderList.size.toString(),
                    icon = Icons.Default.ShoppingBag, color = OrangeMid,
                    sub = "${pendingOrders.size} need action",
                    subColor = if (pendingOrders.isNotEmpty()) OrangeMid else GreenMid,
                    onClick = onNavigateToOrders
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeStatCard(
                    modifier = Modifier.weight(1f),
                    label = "Delivered", value = deliveredOrders.size.toString(),
                    icon = Icons.Default.CheckCircle, color = GreenMid,
                    sub = "completed orders",
                    subColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onNavigateToOrders
                )
                HomeStatCard(
                    modifier = Modifier.weight(1f),
                    label = "Low Stock", value = lowStockCount.toString(),
                    icon = Icons.Default.Warning, color = if (lowStockCount > 0) RedMid else GreenMid,
                    sub = if (lowStockCount > 0) "restock needed" else "stock healthy",
                    subColor = if (lowStockCount > 0) RedMid else GreenMid,
                    onClick = onNavigateToProducts
                )
            }
        }

        // Weekly trend chart
        item {
            Spacer(Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Orders This Week", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        val weekTotal = weekTrend.sum()
                        Surface(shape = RoundedCornerShape(20.dp), color = BlueMid.copy(alpha = 0.1f)) {
                            Text("$weekTotal total", modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = BlueMid)
                        }
                    }
                    val maxVal = weekTrend.maxOrNull()?.coerceAtLeast(1) ?: 1
                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    val todayDow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                    val todayIndex = (todayDow + 5) % 7
                    Row(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weekTrend.forEachIndexed { i, count ->
                            val fraction = count.toFloat() / maxVal.toFloat()
                            val isToday = i == todayIndex
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.fillMaxHeight()) {
                                if (count > 0) {
                                    Text("$count", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                    Spacer(Modifier.height(2.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .fillMaxHeight(fraction.coerceAtLeast(0.05f))
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            if (isToday) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                        )
                                )
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        days.forEachIndexed { i, day ->
                            val isToday = i == todayIndex
                            Text(
                                day,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Immediate tasks
        val hasTasks = pendingOrders.isNotEmpty() || lowStockCount > 0 || outOfStockCount > 0 || processingOrders.isNotEmpty()
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Action Required", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    if (hasTasks) {
                        val taskCount = listOf(pendingOrders.isNotEmpty(), lowStockCount > 0, outOfStockCount > 0, processingOrders.isNotEmpty()).count { it }
                        Surface(shape = CircleShape, color = RedMid, modifier = Modifier.size(20.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("$taskCount", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 9.sp) }
                        }
                    }
                }

                if (!hasTasks) {
                    Surface(modifier = Modifier.fillMaxWidth(), color = GreenMid.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = GreenMid, modifier = Modifier.size(24.dp))
                            Column {
                                Text("You're all caught up!", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = GreenMid)
                                Text("No urgent tasks right now.", style = MaterialTheme.typography.bodySmall, color = GreenMid.copy(alpha = 0.8f))
                            }
                        }
                    }
                } else {
                    if (pendingOrders.isNotEmpty()) ActionTask("${pendingOrders.size} orders need confirmation", "Tap to review and accept", Icons.Default.NotificationImportant, OrangeMid, onNavigateToOrders)
                    if (processingOrders.isNotEmpty()) ActionTask("${processingOrders.size} orders ready to pack", "Mark as packed to move forward", Icons.Default.Inventory, BlueMid, onNavigateToOrders)
                    if (outOfStockCount > 0) ActionTask("$outOfStockCount products out of stock", "Restock to keep selling", Icons.Default.ErrorOutline, RedMid, onNavigateToProducts)
                    if (lowStockCount > 0) ActionTask("$lowStockCount products running low (<5)", "Consider restocking soon", Icons.Default.Warning, OrangeMid, onNavigateToProducts)
                }
            }
        }

        // Quick actions (only if verified)
        if (isVerified) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Quick Actions", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onAddProduct,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.AddBox, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add Product", fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = onNavigateToOrders,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.Receipt, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Orders", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Recent orders preview
        if (recentOrders.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent Orders", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                        TextButton(onClick = onNavigateToOrders) { Text("See All", style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
            items(recentOrders, key = { it.id }) { order ->
                RecentOrderRow(order = order, modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp), onClick = onNavigateToOrders)
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.75f))
    }
}

@Composable
private fun HomeStatCard(
    modifier: Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    sub: String,
    subColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.1f), modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }
                }
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            }
            Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold))
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Text(sub, style = MaterialTheme.typography.labelSmall, color = subColor)
            }
        }
    }
}

@Composable
private fun ActionTask(title: String, desc: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.07f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ArrowForwardIos, null, modifier = Modifier.size(14.dp), tint = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun RecentOrderRow(order: Order, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM d, hh:mm a", Locale.getDefault()) }
    val statusColor = orderStatusColor(order.status)
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(orderStatusIcon(order.status), null, tint = statusColor, modifier = Modifier.size(20.dp)) }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("#${order.orderNumber}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                Text(dateFormat.format(Date(order.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("PKR ${order.total.toInt()}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.1f)) {
                    Text(order.status.name.replace("_", " "), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = statusColor, fontSize = 9.sp)
                }
            }
        }
    }
}

// ─── Products Tab ─────────────────────────────────────────────────────────────

@Composable
private fun ProductsTab(
    state: Resource<List<SupabaseProduct>>,
    isVerified: Boolean,
    onAdd: () -> Unit,
    onEdit: (SupabaseProduct) -> Unit,
    onDelete: (SupabaseProduct) -> Unit,
    onRefresh: () -> Unit
) {
    if (!isVerified) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Surface(modifier = Modifier.size(88.dp), shape = CircleShape, color = OrangeDark.copy(alpha = 0.1f)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.HourglassEmpty, null, modifier = Modifier.size(44.dp), tint = OrangeDark)
                    }
                }
                Text("Awaiting Approval", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
                Text(
                    "Once an admin approves your store, you can upload and manage your products here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Surface(shape = RoundedCornerShape(12.dp), color = OrangeDark.copy(alpha = 0.07f), modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, OrangeDark.copy(alpha = 0.2f))
                ) {
                    Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp), tint = OrangeDark)
                        Text("Contact the admin if your account has been pending for a while.", style = MaterialTheme.typography.bodySmall, color = OrangeDark.copy(alpha = 0.9f))
                    }
                }
            }
        }
        return
    }

    when (state) {
        is Resource.Loading -> CenteredIndicator()
        is Resource.Error   -> RetryState(state.message ?: "Failed to load products", onRefresh)
        is Resource.Success -> {
            val allProducts = state.data ?: emptyList()

            var searchQuery  by remember { mutableStateOf("") }
            var activeFilter by remember { mutableStateOf("All") }
            val filters = listOf("All", "In Stock", "Low Stock", "Out of Stock")

            val filtered = remember(allProducts, searchQuery, activeFilter) {
                allProducts
                    .filter { p ->
                        searchQuery.isBlank() || p.name.contains(searchQuery, true) || p.category.contains(searchQuery, true)
                    }
                    .filter { p ->
                        when (activeFilter) {
                            "In Stock"     -> p.stock > 4
                            "Low Stock"    -> p.stock in 1..4
                            "Out of Stock" -> p.stock <= 0
                            else           -> true
                        }
                    }
                    .sortedWith(compareBy { it.stock })
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("${allProducts.size} Products", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold))
                                val outOf = allProducts.count { it.stock <= 0 }
                                if (outOf > 0) Text("$outOf out of stock", style = MaterialTheme.typography.labelSmall, color = RedMid)
                            }
                            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search products…", style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp)) }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filters) { f ->
                                val count = when (f) {
                                    "In Stock"     -> allProducts.count { it.stock > 4 }
                                    "Low Stock"    -> allProducts.count { it.stock in 1..4 }
                                    "Out of Stock" -> allProducts.count { it.stock <= 0 }
                                    else           -> allProducts.size
                                }
                                FilterChip(
                                    selected = activeFilter == f,
                                    onClick = { activeFilter = f },
                                    label = { Text("$f ($count)") },
                                    leadingIcon = if (activeFilter == f) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) } } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    EmptyState(if (searchQuery.isNotBlank()) "No products match \"$searchQuery\"" else "No products in this category")
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { product ->
                            RetailerProductCard(product = product, onEdit = { onEdit(product) }, onDelete = { onDelete(product) })
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RetailerProductCard(product: SupabaseProduct, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            icon = { Icon(Icons.Default.Delete, null, tint = RedMid) },
            title = { Text("Delete Product") },
            text  = { Text("Delete \"${product.name}\"? This cannot be undone.") },
            confirmButton   = { Button(onClick = { showConfirm = false; onDelete() }, colors = ButtonDefaults.buttonColors(containerColor = RedMid)) { Text("Delete") } },
            dismissButton   = { TextButton(onClick = { showConfirm = false }) { Text("Cancel") } }
        )
    }

    val stockColor = when {
        product.stock <= 0 -> RedMid
        product.stock < 5  -> OrangeMid
        else               -> GreenMid
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (product.stock <= 0) androidx.compose.foundation.BorderStroke(1.dp, RedMid.copy(alpha = 0.3f)) else null
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            // Product image
            Box(modifier = Modifier.size(80.dp)) {
                AsyncImage(
                    model = product.imageUrl ?: product.images.firstOrNull(),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                if (product.stock <= 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                            .background(RedMid),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("OUT", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 2.dp), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(product.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("PKR ${product.price.toInt()}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)) {
                        Text(product.category, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = stockColor.copy(alpha = 0.1f)) {
                        Text(
                            when {
                                product.stock <= 0 -> "Out of stock"
                                product.stock < 5  -> "${product.stock} left"
                                else               -> "${product.stock} in stock"
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = stockColor
                        )
                    }
                }

                // Stock bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val fraction = (product.stock.toFloat() / 20f).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(stockColor)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilledTonalIconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(18.dp))
                }
                FilledTonalIconButton(
                    onClick = { showConfirm = true },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = RedMid.copy(alpha = 0.1f), contentColor = RedMid)
                ) {
                    Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ─── Orders Tab ───────────────────────────────────────────────────────────────

private val returnStatuses = setOf(
    OrderStatus.RETURN_REQUESTED, OrderStatus.RETURN_APPROVED,
    OrderStatus.RETURN_IN_TRANSIT, OrderStatus.RETURN_RECEIVED,
    OrderStatus.RETURNED, OrderStatus.RETURN_REJECTED
)

@Composable
private fun OrdersTab(
    orders: Resource<List<Order>>,
    onUpdateStatus: (String, OrderStatus) -> Unit,
    onRefresh: () -> Unit
) {
    when (orders) {
        is Resource.Loading -> CenteredIndicator()
        is Resource.Error   -> RetryState(orders.message ?: "Error", onRefresh)
        is Resource.Success -> {
            val list = (orders.data ?: emptyList()).filter { it.status !in returnStatuses }
            var statusFilter by remember { mutableStateOf<OrderStatus?>(null) }

            val filterStatuses = remember(list) {
                listOf(null) + OrderStatus.values().filter { s -> list.any { it.status == s } }
            }
            val filtered = remember(list, statusFilter) {
                (if (statusFilter == null) list else list.filter { it.status == statusFilter })
                    .sortedByDescending { it.createdAt }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("${list.size} Orders", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold))
                                val pendingN = list.count { it.status == OrderStatus.PENDING }
                                if (pendingN > 0) Text("$pendingN pending action", style = MaterialTheme.typography.labelSmall, color = OrangeMid)
                            }
                            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filterStatuses) { status ->
                                val count = if (status == null) list.size else list.count { it.status == status }
                                FilterChip(
                                    selected = statusFilter == status,
                                    onClick  = { statusFilter = status },
                                    label    = {
                                        Text(
                                            if (status == null) "All ($count)"
                                            else "${status.name.replace("_", " ")} ($count)"
                                        )
                                    },
                                    colors = if (status != null) FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = orderStatusColor(status).copy(alpha = 0.15f),
                                        selectedLabelColor = orderStatusColor(status)
                                    ) else FilterChipDefaults.filterChipColors()
                                )
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    EmptyState("No orders here yet.")
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { order ->
                            RetailerOrderCard(order = order, onUpdateStatus = onUpdateStatus)
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RetailerOrderCard(
    order: Order,
    onUpdateStatus: (String, OrderStatus) -> Unit
) {
    val dateFormat  = remember { SimpleDateFormat("MMM d, yyyy  •  hh:mm a", Locale.getDefault()) }
    val statusColor = orderStatusColor(order.status)
    var expanded    by remember(order.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(if (expanded) 3.dp else 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Header row (always visible) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(shape = RoundedCornerShape(10.dp), color = statusColor.copy(alpha = 0.1f), modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(orderStatusIcon(order.status), null, tint = statusColor, modifier = Modifier.size(22.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("#${order.orderNumber}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    Text(dateFormat.format(Date(order.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("PKR ${order.total.toInt()}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)
                    Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.12f)) {
                        Text(
                            order.status.name.replace("_", " "),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor, fontSize = 9.sp
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)
                )
            }

            // ── Expanded body ──
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    // Items
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${order.items.size} item(s)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        order.items.forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                AsyncImage(
                                    model = item.productImage,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentScale = ContentScale.Crop
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.productName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${item.quantity}× • ${item.selectedSize} • ${item.selectedColor}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("PKR ${(item.price * item.quantity).toInt()}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                    // Delivery address
                    val addr = order.shippingAddress
                    if (addr != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Deliver To",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    if (addr.fullName.isNotBlank())
                                        Text(addr.fullName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                    if (addr.phone.isNotBlank())
                                        Text(addr.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        buildString {
                                            append(addr.addressLine1)
                                            if (addr.addressLine2.isNotBlank()) append(", ${addr.addressLine2}")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${addr.city}, ${addr.province} ${addr.postalCode}".trim(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                    }

                    // Totals
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            PriceRowSmall("Subtotal", "PKR ${order.subtotal.toInt()}")
                            PriceRowSmall("Shipping", "PKR ${order.shipping.toInt()}")
                            PriceRowSmall("Tax", "PKR ${order.tax.toInt()}")
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Grand Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("PKR ${order.total.toInt()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Payment badge
                    val isSafepay = order.paymentMethod == PaymentMethod.SAFEPAY
                    val isPaid    = order.paymentStatus == PaymentStatus.COMPLETED
                    val payColor  = when {
                        isSafepay && isPaid -> GreenDark
                        isSafepay          -> BlueDark
                        else               -> OrangeDark
                    }
                    Surface(shape = RoundedCornerShape(20.dp), color = payColor.copy(alpha = 0.1f)) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(if (isSafepay) Icons.Default.Lock else Icons.Default.Money, null, modifier = Modifier.size(14.dp), tint = payColor)
                            Text(
                                when { isSafepay && isPaid -> "Paid via Safepay"; isSafepay -> "Safepay (Pending)"; else -> "Cash on Delivery" },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = payColor
                            )
                        }
                    }

                    // Action buttons
                    OrderActionButtons(order = order, onUpdateStatus = onUpdateStatus)
                }
            }
        }
    }
}

@Composable
private fun PriceRowSmall(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(52.dp))
        Text(value, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun OrderActionButtons(
    order: Order,
    onUpdateStatus: (String, OrderStatus) -> Unit
) {
    val isFinal = order.status == OrderStatus.DELIVERED
        || order.status == OrderStatus.CANCELLED
        || order.status == OrderStatus.RETURNED
        || order.status == OrderStatus.RETURN_REJECTED

    if (isFinal) {
        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(orderStatusIcon(order.status), null, modifier = Modifier.size(16.dp), tint = orderStatusColor(order.status))
                Spacer(Modifier.width(8.dp))
                Text("Order ${order.status.name.replace("_", " ").lowercase()}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = orderStatusColor(order.status))
            }
        }
        return
    }

    when (order.status) {
        OrderStatus.PENDING -> {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onUpdateStatus(order.id, OrderStatus.CONFIRMED) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenMid),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Accept", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { onUpdateStatus(order.id, OrderStatus.CANCELLED) },
                    modifier = Modifier.weight(0.6f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedMid),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("Reject", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        OrderStatus.CONFIRMED, OrderStatus.PROCESSING -> {
            Button(
                onClick = { onUpdateStatus(order.id, OrderStatus.PACKED) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BlueMid),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.Inventory, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Mark as Packed", fontWeight = FontWeight.SemiBold)
            }
        }
        OrderStatus.PACKED -> {
            Button(
                onClick = { onUpdateStatus(order.id, OrderStatus.READY_FOR_PICKUP) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleMid),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ready for Pickup", fontWeight = FontWeight.SemiBold)
            }
        }
        else -> {
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text("Waiting for courier", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ─── Returns Tab ──────────────────────────────────────────────────────────────

@Composable
private fun ReturnsTab(
    orders: Resource<List<Order>>,
    onAcceptReturn: (String, String) -> Unit,
    onRejectReturn: (String, String) -> Unit,
    onVerifyAndRefund: (String) -> Unit,
    onRefresh: () -> Unit
) {
    when (orders) {
        is Resource.Loading -> CenteredIndicator()
        is Resource.Error   -> RetryState(orders.message ?: "Error", onRefresh)
        is Resource.Success -> {
            val returnOrders = (orders.data ?: emptyList())
                .filter { it.status in returnStatuses }
                .sortedByDescending { it.createdAt }

            var selectedFilter by remember { mutableStateOf<OrderStatus?>(null) }
            val filterOptions = remember(returnOrders) {
                listOf(null) + returnStatuses.filter { s -> returnOrders.any { it.status == s } }
            }
            val filtered = remember(returnOrders, selectedFilter) {
                if (selectedFilter == null) returnOrders else returnOrders.filter { it.status == selectedFilter }
            }

            var enlargedImageUrl by remember { mutableStateOf<String?>(null) }
            if (enlargedImageUrl != null) {
                FullscreenImageDialog(url = enlargedImageUrl!!, onDismiss = { enlargedImageUrl = null })
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("${returnOrders.size} Return${if (returnOrders.size != 1) "s" else ""}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold))
                                val requested = returnOrders.count { it.status == OrderStatus.RETURN_REQUESTED }
                                if (requested > 0) Text("$requested need review", style = MaterialTheme.typography.labelSmall, color = PurpleMid)
                            }
                            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filterOptions) { status ->
                                val count = if (status == null) returnOrders.size else returnOrders.count { it.status == status }
                                FilterChip(
                                    selected = selectedFilter == status,
                                    onClick  = { selectedFilter = status },
                                    label    = { Text(if (status == null) "All ($count)" else "${status.name.replace("_", " ")} ($count)") },
                                    colors   = if (status != null) FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = orderStatusColor(status).copy(alpha = 0.15f),
                                        selectedLabelColor     = orderStatusColor(status)
                                    ) else FilterChipDefaults.filterChipColors()
                                )
                            }
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    EmptyState("No return requests yet.")
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { order ->
                            ReturnOrderCard(
                                order             = order,
                                onAcceptReturn    = onAcceptReturn,
                                onRejectReturn    = onRejectReturn,
                                onVerifyAndRefund = onVerifyAndRefund,
                                onImageClick      = { url -> enlargedImageUrl = url }
                            )
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReturnOrderCard(
    order: Order,
    onAcceptReturn: (String, String) -> Unit,
    onRejectReturn: (String, String) -> Unit,
    onVerifyAndRefund: (String) -> Unit,
    onImageClick: (String) -> Unit
) {
    val dateFormat  = remember { SimpleDateFormat("MMM d, yyyy  •  hh:mm a", Locale.getDefault()) }
    val statusColor = orderStatusColor(order.status)

    var showReturnDialog by remember { mutableStateOf(false) }
    var isAccepting      by remember { mutableStateOf(true) }
    var showVerifyDialog by remember { mutableStateOf(false) }

    if (showReturnDialog) {
        ReturnActionDialog(
            isAccept  = isAccepting,
            onConfirm = { note ->
                if (isAccepting) onAcceptReturn(order.id, note) else onRejectReturn(order.id, note)
                showReturnDialog = false
            },
            onDismiss = { showReturnDialog = false }
        )
    }
    if (showVerifyDialog) {
        AlertDialog(
            onDismissRequest = { showVerifyDialog = false },
            title = { Text("Verify & Refund") },
            text  = { Text("Confirm the returned item is in acceptable condition. This will process the refund and deduct PKR ${order.total.toInt()} from your revenue.") },
            confirmButton = {
                Button(onClick = { onVerifyAndRefund(order.id); showVerifyDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = GreenMid)) { Text("Confirm Refund") }
            },
            dismissButton = { TextButton(onClick = { showVerifyDialog = false }) { Text("Cancel") } }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // Header
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(10.dp), color = statusColor.copy(alpha = 0.1f), modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(orderStatusIcon(order.status), null, tint = statusColor, modifier = Modifier.size(22.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("#${order.orderNumber}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                    Text(dateFormat.format(Date(order.createdAt)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.12f)) {
                    Text(
                        order.status.name.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            // Return reason
            if (!order.returnReason.isNullOrBlank()) {
                Surface(
                    shape  = RoundedCornerShape(10.dp),
                    color  = PurpleMid.copy(alpha = 0.06f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PurpleMid.copy(alpha = 0.2f))
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(16.dp), tint = PurpleMid)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Return Reason", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = PurpleMid)
                            Text(order.returnReason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // Return images — tap to enlarge
            if (order.returnImages.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Image, null, modifier = Modifier.size(16.dp), tint = PurpleMid)
                        Text(
                            "Return Photos (${order.returnImages.size})  •  Tap to enlarge",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = PurpleMid
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(order.returnImages) { url ->
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.5.dp, PurpleMid.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                    .clickable { onImageClick(url) }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Return photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Zoom hint overlay
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .size(18.dp)
                                        .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.ZoomIn, null, modifier = Modifier.size(12.dp), tint = Color.White)
                                }
                            }
                        }
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            }

            // Items
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${order.items.size} item(s)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                order.items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AsyncImage(
                            model = item.productImage,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.productName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${item.quantity}× • ${item.selectedSize} • ${item.selectedColor}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("PKR ${(item.price * item.quantity).toInt()}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            // Total row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Order Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("PKR ${order.total.toInt()}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.primary)
                }
            }

            // Action area per status
            val isFinalReturn = order.status == OrderStatus.RETURNED || order.status == OrderStatus.RETURN_REJECTED
            when {
                isFinalReturn -> {
                    Surface(shape = RoundedCornerShape(10.dp), color = statusColor.copy(alpha = 0.08f)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(orderStatusIcon(order.status), null, modifier = Modifier.size(16.dp), tint = statusColor)
                            Spacer(Modifier.width(8.dp))
                            Text("Return ${order.status.name.replace("_", " ").lowercase()}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = statusColor)
                        }
                    }
                }
                order.status == OrderStatus.RETURN_REQUESTED -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { isAccepting = true; showReturnDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenMid),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Approve", fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = { isAccepting = false; showReturnDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedMid),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Block, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reject", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                order.status == OrderStatus.RETURN_APPROVED -> {
                    Surface(shape = RoundedCornerShape(10.dp), color = GreenMid.copy(alpha = 0.08f)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(16.dp), tint = GreenMid)
                            Spacer(Modifier.width(8.dp))
                            Text("Approved – awaiting delivery partner pickup", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = GreenMid)
                        }
                    }
                }
                order.status == OrderStatus.RETURN_IN_TRANSIT -> {
                    Surface(shape = RoundedCornerShape(10.dp), color = BlueMid.copy(alpha = 0.08f)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TwoWheeler, null, modifier = Modifier.size(16.dp), tint = BlueMid)
                            Spacer(Modifier.width(8.dp))
                            Text("Return in transit – delivery partner en route", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = BlueMid)
                        }
                    }
                }
                order.status == OrderStatus.RETURN_RECEIVED -> {
                    Button(
                        onClick = { showVerifyDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleMid),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.VerifiedUser, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Verify & Process Refund", fontWeight = FontWeight.SemiBold)
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun FullscreenImageDialog(url: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = url,
                contentDescription = "Return photo enlarged",
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }
        }
    }
}

// ─── Dialogs ──────────────────────────────────────────────────────────────────

@Composable
private fun ReturnActionDialog(isAccept: Boolean, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(if (isAccept) Icons.Default.CheckCircle else Icons.Default.Cancel, null, tint = if (isAccept) GreenMid else RedMid) },
        title = { Text(if (isAccept) "Accept Return" else "Reject Return", fontWeight = FontWeight.Bold) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Leave a note for the customer (optional):", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Return approved, please send the item back.") },
                    minLines = 3,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(note) },
                colors = if (isAccept) ButtonDefaults.buttonColors(containerColor = GreenMid)
                         else ButtonDefaults.buttonColors(containerColor = RedMid)
            ) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreProfileDialog(
    existingStore: Retailer?,
    onConfirm: (name: String, desc: String, phone: String, addr: String, images: List<Uri>) -> Unit,
    onDismiss: () -> Unit
) {
    val isCreating = existingStore == null
    var name    by remember { mutableStateOf(existingStore?.storeName ?: "") }
    var desc    by remember { mutableStateOf(existingStore?.storeDescription ?: "") }
    var phone   by remember { mutableStateOf(existingStore?.contactPhone ?: "") }
    var address by remember { mutableStateOf(existingStore?.storeAddress ?: "") }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> selectedImages = (selectedImages + uris).distinct() }

    if (isCreating) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Create Your Store", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
                        },
                        actions = {
                            TextButton(
                                onClick = { if (name.isNotBlank()) onConfirm(name, desc, phone, address, selectedImages) },
                                enabled = name.isNotBlank()
                            ) { Text("Create", fontWeight = FontWeight.Bold) }
                        }
                    )
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Store Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
                    }
                    item {
                        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2, shape = RoundedCornerShape(10.dp))
                    }
                    item {
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Contact Phone") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
                    }
                    item {
                        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Store Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
                    }
                    item {
                        Divider()
                        Spacer(Modifier.height(4.dp))
                        Text("Verification Documents", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Upload your CNIC, business license, or store photos so the admin can verify your account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selectedImages.isNotEmpty()) {
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(selectedImages) { uri ->
                                    Box {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(90.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentScale = ContentScale.Crop
                                        )
                                        IconButton(
                                            onClick = { selectedImages = selectedImages.filter { it != uri } },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(24.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = { imagePicker.launch("image/*") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (selectedImages.isEmpty()) "Upload Documents / Photos" else "Add More Images")
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon  = { Icon(Icons.Default.Storefront, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Edit Store Profile", fontWeight = FontWeight.Bold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Store Name *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2, shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Contact Phone") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Store Address") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(10.dp))
                }
            },
            confirmButton = {
                Button(onClick = { if (name.isNotBlank()) onConfirm(name, desc, phone, address, emptyList()) }, enabled = name.isNotBlank(), shape = RoundedCornerShape(10.dp)) {
                    Text("Save Changes")
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
    }
}

// ─── Shared helpers ───────────────────────────────────────────────────────────

@Composable
private fun CenteredIndicator() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun EmptyState(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.Inbox, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RetryState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(52.dp), tint = RedMid)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Button(onClick = onRetry, shape = RoundedCornerShape(10.dp)) { Text("Retry") }
        }
    }
}

private fun orderStatusColor(status: OrderStatus): Color = when (status) {
    OrderStatus.PENDING           -> Color(0xFFF57C00)
    OrderStatus.CONFIRMED         -> Color(0xFF0288D1)
    OrderStatus.PROCESSING        -> Color(0xFF7B1FA2)
    OrderStatus.PACKED            -> Color(0xFF00897B)
    OrderStatus.READY_FOR_PICKUP  -> Color(0xFF43A047)
    OrderStatus.SHIPPED           -> Color(0xFF00838F)
    OrderStatus.OUT_FOR_DELIVERY  -> Color(0xFF558B2F)
    OrderStatus.DELIVERED         -> Color(0xFF388E3C)
    OrderStatus.CANCELLED         -> Color(0xFFD32F2F)
    OrderStatus.RETURN_REQUESTED  -> Color(0xFF6A1B9A)
    OrderStatus.RETURN_APPROVED   -> Color(0xFF2E7D32)
    OrderStatus.RETURN_IN_TRANSIT -> Color(0xFF1565C0)
    OrderStatus.RETURN_RECEIVED   -> Color(0xFF00838F)
    OrderStatus.RETURNED          -> Color(0xFF6D4C41)
    OrderStatus.RETURN_REJECTED   -> Color(0xFFD32F2F)
}

private fun orderStatusIcon(status: OrderStatus) = when (status) {
    OrderStatus.PENDING           -> Icons.Default.Schedule
    OrderStatus.CONFIRMED         -> Icons.Default.CheckCircleOutline
    OrderStatus.PROCESSING        -> Icons.Default.Autorenew
    OrderStatus.PACKED            -> Icons.Default.Inventory
    OrderStatus.READY_FOR_PICKUP  -> Icons.Default.Store
    OrderStatus.SHIPPED           -> Icons.Default.LocalShipping
    OrderStatus.OUT_FOR_DELIVERY  -> Icons.Default.TwoWheeler
    OrderStatus.DELIVERED         -> Icons.Default.CheckCircle
    OrderStatus.CANCELLED         -> Icons.Default.Cancel
    OrderStatus.RETURN_REQUESTED  -> Icons.Default.AssignmentReturn
    OrderStatus.RETURN_APPROVED   -> Icons.Default.CheckCircle
    OrderStatus.RETURN_IN_TRANSIT -> Icons.Default.TwoWheeler
    OrderStatus.RETURN_RECEIVED   -> Icons.Default.Inbox
    OrderStatus.RETURNED          -> Icons.Default.Undo
    OrderStatus.RETURN_REJECTED   -> Icons.Default.Cancel
}

@Composable
fun RejectedScreen(role: String, onLogout: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                Modifier
                    .size(96.dp)
                    .background(RedMid.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    tint = RedMid,
                    modifier = Modifier.size(52.dp)
                )
            }

            Text(
                "Application Rejected",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = RedMid
            )

            Text(
                "Your $role application has been reviewed and rejected by the platform admin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Surface(
                color = RedMid.copy(alpha = 0.07f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, null, tint = RedMid, modifier = Modifier.size(18.dp))
                        Text(
                            "What does this mean?",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = RedMid
                        )
                    }
                    Text(
                        "• Your account has been restricted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "• You cannot perform any $role actions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "• Contact platform support for more information",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = RedMid)
            ) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Logout", fontWeight = FontWeight.Bold)
            }
        }
    }
}
