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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nextgenecommerce.data.models.DeliveryPartner
import com.example.nextgenecommerce.data.models.Order
import com.example.nextgenecommerce.data.models.OrderStatus
import com.example.nextgenecommerce.data.models.PaymentStatus
import com.example.nextgenecommerce.data.models.ProductEntity
import com.example.nextgenecommerce.data.models.Retailer
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.AuthViewModel
import com.example.nextgenecommerce.presentation.viewmodel.DeliveryViewModel
import com.example.nextgenecommerce.presentation.viewmodel.RetailerViewModel
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
    retailerViewModel: RetailerViewModel = hiltViewModel(),
    deliveryViewModel: DeliveryViewModel = hiltViewModel(),
    adminVaultViewModel: com.example.nextgenecommerce.presentation.viewmodel.AdminVaultViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val isAdmin by authViewModel.isAdmin.collectAsState()
    val vaultSummary by adminVaultViewModel.vaultSummary.collectAsState()
    val allRetailers by retailerViewModel.allRetailers.collectAsState()
    val retailerApprovalState by retailerViewModel.retailerApprovalState.collectAsState()
    val allDeliveryPartners by deliveryViewModel.allDeliveryPartners.collectAsState()
    val deliveryPartnerApprovalState by deliveryViewModel.deliveryPartnerApprovalState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Retailers", "Delivery")

    var showAccessDeniedDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isAdmin, currentUser) {
        if (currentUser != null && !isAdmin) showAccessDeniedDialog = true
    }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            0    -> adminVaultViewModel.loadVaultSummary()
            1    -> retailerViewModel.loadAllRetailersForAdmin()
            2    -> deliveryViewModel.loadAllDeliveryPartnersForAdmin()
        }
    }

    LaunchedEffect(retailerApprovalState) {
        when (val s = retailerApprovalState) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar("Retailer approval updated")
                retailerViewModel.resetRetailerApprovalState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(s.message ?: "Failed to update retailer")
                retailerViewModel.resetRetailerApprovalState()
            }
            else -> {}
        }
    }

    LaunchedEffect(deliveryPartnerApprovalState) {
        when (val s = deliveryPartnerApprovalState) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar("Delivery partner approval updated")
                deliveryViewModel.resetDeliveryPartnerApprovalState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(s.message ?: "Failed to update delivery partner")
                deliveryViewModel.resetDeliveryPartnerApprovalState()
            }
            else -> {}
        }
    }

    LaunchedEffect(vaultSummary) {
        if (vaultSummary is Resource.Error) {
            snackbarHostState.showSnackbar(
                "Commission fetch failed: ${(vaultSummary as Resource.Error).message}"
            )
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
                            adminVaultViewModel.loadVaultSummary()
                        }) {
                            Icon(Icons.Default.Refresh, "Refresh")
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
                    val tabIcons = listOf(
                        Icons.Default.Dashboard,
                        Icons.Default.Store,
                        Icons.Default.LocalShipping
                    )
                    val unverifiedCount = (allRetailers as? Resource.Success)?.data
                        ?.count { !it.isVerified } ?: 0
                    val pendingDeliveryCount = (allDeliveryPartners as? Resource.Success)?.data
                        ?.count { !it.isVerified } ?: 0

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
                                    if (index == 1 && unverifiedCount > 0) {
                                        Surface(shape = CircleShape, color = Color(0xFFE65100), modifier = Modifier.size(16.dp)) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                Text("$unverifiedCount", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 8.sp)
                                            }
                                        }
                                    }
                                    if (index == 2 && pendingDeliveryCount > 0) {
                                        Surface(shape = CircleShape, color = Color(0xFF1565C0), modifier = Modifier.size(16.dp)) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                Text("$pendingDeliveryCount", style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 8.sp)
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (selectedTab) {
            0 -> OverviewTab(
                modifier = Modifier.padding(padding),
                vaultSummary = vaultSummary,
                onNavigateToRevenue = { navController.navigate(Screen.RevenueDetail.route) },
                onRefreshVault = { adminVaultViewModel.loadVaultSummary() }
            )
            1 -> RetailersTab(
                modifier = Modifier.padding(padding),
                retailersState = allRetailers,
                onApprove = { rId -> retailerViewModel.setRetailerApproval(rId, true) },
                onRevoke  = { rId -> retailerViewModel.setRetailerApproval(rId, false) },
                onRefresh = { retailerViewModel.loadAllRetailersForAdmin() },
                onViewProducts = { retailer ->
                    navController.navigate(
                        Screen.AdminRetailerProducts.createRoute(retailer.id, retailer.storeName)
                    )
                }
            )
            2 -> DeliveryPartnersTab(
                modifier = Modifier.padding(padding),
                partnersState = allDeliveryPartners,
                onApprove = { pId -> deliveryViewModel.setDeliveryPartnerApproval(pId, true) },
                onRevoke  = { pId -> deliveryViewModel.setDeliveryPartnerApproval(pId, false) },
                onRefresh = { deliveryViewModel.loadAllDeliveryPartnersForAdmin() }
            )
        }
    }
}

// ─── Overview Tab ────────────────────────────────────────────────────────────

@Composable
private fun OverviewTab(
    modifier: Modifier,
    vaultSummary: Resource<com.example.nextgenecommerce.data.models.VaultSummary>?,
    onNavigateToRevenue: () -> Unit,
    onRefreshVault: () -> Unit
) {
    val totalCommission = (vaultSummary as? Resource.Success)?.data?.total ?: 0.0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            RevenueCard(
                totalRevenue = totalCommission,
                onClick = onNavigateToRevenue
            )
        }
        item {
            AdminVaultCard(vaultSummary = vaultSummary, onRefresh = onRefreshVault)
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun AdminVaultCard(
    vaultSummary: Resource<com.example.nextgenecommerce.data.models.VaultSummary>?,
    onRefresh: () -> Unit
) {
    val pkrFormat = NumberFormat.getNumberInstance(Locale.US)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AccountBalance, null, tint = Color(0xFF81C784), modifier = Modifier.size(22.dp))
                    Text("Admin Vault", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, null, tint = Color(0xFF81C784), modifier = Modifier.size(20.dp))
                }
            }

            when (vaultSummary) {
                null, is Resource.Loading -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF81C784), trackColor = Color(0xFF2E7D32))
                }
                is Resource.Error -> {
                    Text(
                        "Unable to load commission data",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFFEF9A9A)
                    )
                    Text(
                        vaultSummary.message ?: "Ensure the admin_commissions table exists in Supabase",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFEF9A9A).copy(alpha = 0.8f)
                    )
                    TextButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, null, tint = Color(0xFF81C784), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Retry", color = Color(0xFF81C784))
                    }
                }
                is Resource.Success -> {
                    val data = vaultSummary.data ?: com.example.nextgenecommerce.data.models.VaultSummary(0.0, 0.0, 0.0)
                    Text(
                        "PKR ${pkrFormat.format(data.total.toLong())}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color.White
                    )
                    Divider(color = Color(0xFF2E7D32))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Retailer (5%)", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA5D6A7))
                            Text("PKR ${pkrFormat.format(data.retailerCommission.toLong())}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Delivery (2%)", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA5D6A7))
                            Text("PKR ${pkrFormat.format(data.deliveryCommission.toLong())}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RevenueCard(totalRevenue: Double, onClick: () -> Unit) {
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
                        "Total Commission",
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
                    "Tap to see commission breakdown",
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
    onAssignToNextGen: () -> Unit,
    onAssignToSaif: () -> Unit,
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
        // Next Gen Ecommerce assign card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable { onAssignToNextGen() },
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF2E7D32),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Store, null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Assign All to Next Gen Ecommerce",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                    Text(
                        "Move all products to Next Gen Ecommerce store",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = Color.White)
            }
        }

        // Saif Retailers assign card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable { onAssignToSaif() },
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1565C0),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Store, null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Assign All to Saif Retailers",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                    Text(
                        "Move all products to Saif Retailers store",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = Color.White)
            }
        }

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
    val returnStatuses = setOf(
        OrderStatus.RETURN_REQUESTED, OrderStatus.RETURN_APPROVED,
        OrderStatus.RETURN_IN_TRANSIT, OrderStatus.RETURN_RECEIVED,
        OrderStatus.RETURNED, OrderStatus.RETURN_REJECTED
    )
    val statuses = listOf(null) + OrderStatus.values().filter { it !in returnStatuses }
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

                    // Payment info — rich badge
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Payment method icon + label
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isSafepay = order.paymentMethod == com.example.nextgenecommerce.data.models.PaymentMethod.SAFEPAY
                            Icon(
                                if (isSafepay) Icons.Default.Lock else Icons.Default.Money,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (isSafepay) Color(0xFF00C853) else Color(0xFFF57C00)
                            )
                            Text(
                                if (isSafepay) "Safepay" else "Cash on Delivery",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Payment status chip
                        val (statusColor, statusLabel) = when (order.paymentStatus) {
                            PaymentStatus.COMPLETED -> Pair(Color(0xFF2E7D32), "✓ PAID")
                            PaymentStatus.PENDING   -> Pair(Color(0xFFF57C00), "⧖ PENDING")
                            PaymentStatus.FAILED    -> Pair(Color(0xFFD32F2F), "✗ FAILED")
                            PaymentStatus.REFUNDED  -> Pair(Color(0xFF6A1B9A), "↩ REFUNDED")
                        }
                        Surface(
                            color = statusColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                if (order.paymentMethod == com.example.nextgenecommerce.data.models.PaymentMethod.CASH_ON_DELIVERY
                                    && order.paymentStatus == PaymentStatus.PENDING) "💵 COD"
                                else statusLabel,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (order.paymentMethod == com.example.nextgenecommerce.data.models.PaymentMethod.CASH_ON_DELIVERY
                                    && order.paymentStatus == PaymentStatus.PENDING) Color(0xFFF57C00)
                                else statusColor
                            )
                        }
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
    onConfirm: (OrderStatus) -> Unit
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
                val returnStatusSet = setOf(
                    OrderStatus.RETURN_REQUESTED, OrderStatus.RETURN_APPROVED,
                    OrderStatus.RETURN_IN_TRANSIT, OrderStatus.RETURN_RECEIVED,
                    OrderStatus.RETURNED, OrderStatus.RETURN_REJECTED
                )
                OrderStatus.values().filter { it !in returnStatusSet }.forEach { status ->
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
        .filter { it.status in setOf(
            OrderStatus.RETURN_REQUESTED, OrderStatus.RETURN_APPROVED,
            OrderStatus.RETURN_IN_TRANSIT, OrderStatus.RETURN_RECEIVED
        ) }
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

// ─── Retailers Tab ────────────────────────────────────────────────────────────

@Composable
private fun RetailersTab(
    modifier: Modifier,
    retailersState: Resource<List<Retailer>>,
    onApprove: (String) -> Unit,
    onRevoke: (String) -> Unit,
    onRefresh: () -> Unit,
    onViewProducts: (Retailer) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var pendingApprovalId by remember { mutableStateOf<String?>(null) }
    var pendingApprove by remember { mutableStateOf(true) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog && pendingApprovalId != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                Icon(
                    if (pendingApprove) Icons.Default.VerifiedUser else Icons.Default.Block,
                    null,
                    tint = if (pendingApprove) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
            },
            title = { Text(if (pendingApprove) "Approve Retailer" else "Revoke Approval") },
            text = {
                Text(
                    if (pendingApprove)
                        "This retailer will be able to upload and manage products."
                    else
                        "This retailer will no longer be able to upload or manage products."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pendingApprove) onApprove(pendingApprovalId!!)
                        else onRevoke(pendingApprovalId!!)
                        showConfirmDialog = false
                    },
                    colors = if (pendingApprove)
                        ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    else
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(if (pendingApprove) "Approve" else "Revoke") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header bar
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val count = (retailersState as? Resource.Success)?.data?.size ?: 0
                val unverified = (retailersState as? Resource.Success)?.data?.count { !it.isVerified } ?: 0
                Column {
                    Text("$count Retailers", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (unverified > 0) {
                        Text("$unverified pending approval", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100))
                    }
                }
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null) }
            }
        }

        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search retailers…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null) }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        when (retailersState) {
            is Resource.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Text(retailersState.message ?: "Failed to load retailers", style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = onRefresh) { Text("Retry") }
                    }
                }
            }
            is Resource.Success -> {
                val list = (retailersState.data ?: emptyList())
                    .filter { if (searchQuery.isBlank()) true else it.storeName.contains(searchQuery, ignoreCase = true) || it.contactPhone.contains(searchQuery) }
                    .sortedWith(compareBy({ it.isVerified }, { it.storeName }))

                if (list.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Store, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Text("No retailers found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(list, key = { it.id }) { retailer ->
                            RetailerAdminCard(
                                retailer = retailer,
                                onApprove = {
                                    pendingApprovalId = retailer.id
                                    pendingApprove = true
                                    showConfirmDialog = true
                                },
                                onRevoke = {
                                    pendingApprovalId = retailer.id
                                    pendingApprove = false
                                    showConfirmDialog = true
                                },
                                onViewProducts = { onViewProducts(retailer) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RetailerAdminCard(
    retailer: Retailer,
    onApprove: () -> Unit,
    onRevoke: () -> Unit,
    onViewProducts: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val createdDate = runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(retailer.createdAt)
    }.getOrNull()
    var isExpanded by remember { mutableStateOf(false) }
    var fullScreenUrl by remember { mutableStateOf<String?>(null) }

    fullScreenUrl?.let { url ->
        Dialog(
            onDismissRequest = { fullScreenUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { fullScreenUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { fullScreenUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) { Icon(Icons.Default.Close, null, tint = Color.White) }
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Header row (always visible, tap to expand) ──────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (retailer.isVerified) Color(0xFF2E7D32).copy(alpha = 0.1f) else Color(0xFFE65100).copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (retailer.storeLogoUrl != null) {
                            AsyncImage(model = retailer.storeLogoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Text(retailer.storeName.take(1).uppercase(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = if (retailer.isVerified) Color(0xFF2E7D32) else Color(0xFFE65100))
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(retailer.storeName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (retailer.isVerified) Icon(Icons.Default.Verified, null, modifier = Modifier.size(14.dp), tint = Color(0xFF1E88E5))
                    }
                    if (retailer.contactPhone.isNotBlank()) {
                        Text(retailer.contactPhone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (createdDate != null) {
                        Text("Joined ${dateFormat.format(createdDate)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
                val (badgeColor, badgeLabel) = when {
                    retailer.isVerified -> Color(0xFF2E7D32) to "Approved"
                    retailer.isRejected -> MaterialTheme.colorScheme.error to "Rejected"
                    else                -> Color(0xFFE65100) to "Pending"
                }
                Surface(shape = RoundedCornerShape(20.dp), color = badgeColor.copy(alpha = 0.12f)) {
                    Text(badgeLabel, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = badgeColor)
                }
                Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }

            // ── Expandable details section ───────────────────────────────────────
            AnimatedVisibility(visible = isExpanded, enter = expandVertically(tween(200)), exit = shrinkVertically(tween(200))) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    if (retailer.storeDescription.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Info, null, modifier = Modifier.size(14.dp).padding(top = 2.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(retailer.storeDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    if (retailer.storeAddress.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(retailer.storeAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    // Verification images
                    if (retailer.verificationImages.isNotEmpty()) {
                        Text("Verification Documents  (${retailer.verificationImages.size})", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(retailer.verificationImages) { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { fullScreenUrl = url },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    } else {
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BrokenImage, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("No verification documents uploaded", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // ── Action buttons ───────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val approveActive = retailer.isVerified
                val rejectActive  = retailer.isRejected
                val green = Color(0xFF2E7D32)
                val red   = MaterialTheme.colorScheme.error
                Button(
                    onClick = onApprove, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), enabled = !approveActive,
                    colors = ButtonDefaults.buttonColors(containerColor = green, contentColor = Color.White, disabledContainerColor = green.copy(alpha = 0.35f), disabledContentColor = Color.White.copy(alpha = 0.6f))
                ) {
                    Icon(if (approveActive) Icons.Default.CheckCircle else Icons.Default.VerifiedUser, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (approveActive) "Approved" else "Approve", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onRevoke, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), enabled = !rejectActive,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = red, disabledContentColor = red.copy(alpha = 0.35f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (rejectActive) red.copy(alpha = 0.35f) else red)
                ) {
                    Icon(if (rejectActive) Icons.Default.Cancel else Icons.Default.Block, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (rejectActive) "Rejected" else "Reject", fontWeight = FontWeight.SemiBold)
                }
            }
            OutlinedButton(onClick = onViewProducts, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Default.Inventory, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("View Products", fontWeight = FontWeight.SemiBold)
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
    OrderStatus.PACKED -> Color(0xFF00897B)
    OrderStatus.READY_FOR_PICKUP -> Color(0xFF43A047)
    OrderStatus.SHIPPED -> Color(0xFF00838F)
    OrderStatus.OUT_FOR_DELIVERY -> Color(0xFF558B2F)
    OrderStatus.DELIVERED -> Color(0xFF388E3C)
    OrderStatus.CANCELLED -> Color(0xFFD32F2F)
    OrderStatus.RETURN_REQUESTED  -> Color(0xFF6A1B9A)
    OrderStatus.RETURN_APPROVED   -> Color(0xFF2E7D32)
    OrderStatus.RETURN_IN_TRANSIT -> Color(0xFF1565C0)
    OrderStatus.RETURN_RECEIVED   -> Color(0xFF00838F)
    OrderStatus.RETURNED          -> Color(0xFF6D4C41)
    OrderStatus.RETURN_REJECTED   -> Color(0xFFD32F2F)
}

fun orderStatusIcon(status: OrderStatus): ImageVector = when (status) {
    OrderStatus.PENDING            -> Icons.Default.Schedule
    OrderStatus.CONFIRMED          -> Icons.Default.CheckCircleOutline
    OrderStatus.PROCESSING         -> Icons.Default.Refresh
    OrderStatus.PACKED             -> Icons.Default.Inventory
    OrderStatus.READY_FOR_PICKUP   -> Icons.Default.Store
    OrderStatus.SHIPPED            -> Icons.Default.LocalShipping
    OrderStatus.OUT_FOR_DELIVERY   -> Icons.Default.LocalShipping
    OrderStatus.DELIVERED          -> Icons.Default.CheckCircle
    OrderStatus.CANCELLED          -> Icons.Default.Cancel
    OrderStatus.RETURN_REQUESTED   -> Icons.Default.AssignmentReturn
    OrderStatus.RETURN_APPROVED    -> Icons.Default.CheckCircle
    OrderStatus.RETURN_IN_TRANSIT  -> Icons.Default.TwoWheeler
    OrderStatus.RETURN_RECEIVED    -> Icons.Default.Inbox
    OrderStatus.RETURNED           -> Icons.Default.Undo
    OrderStatus.RETURN_REJECTED    -> Icons.Default.Cancel
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

// ─── Delivery Partners Tab ───────────────────────────────────────────────────

@Composable
private fun DeliveryPartnersTab(
    modifier: Modifier,
    partnersState: Resource<List<DeliveryPartner>>,
    onApprove: (String) -> Unit,
    onRevoke: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var pendingApprovalId by remember { mutableStateOf<String?>(null) }
    var pendingApprove by remember { mutableStateOf(true) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog && pendingApprovalId != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = {
                Icon(
                    if (pendingApprove) Icons.Default.VerifiedUser else Icons.Default.Block,
                    null,
                    tint = if (pendingApprove) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
            },
            title = { Text(if (pendingApprove) "Approve Delivery Partner" else "Revoke Approval") },
            text = {
                Text(
                    if (pendingApprove)
                        "This delivery partner will become the active platform-wide courier. Any existing active partner will be automatically revoked and their in-progress orders reassigned."
                    else
                        "This delivery partner will lose access and can no longer accept or manage deliveries."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pendingApprove) onApprove(pendingApprovalId!!)
                        else onRevoke(pendingApprovalId!!)
                        showConfirmDialog = false
                    },
                    colors = if (pendingApprove)
                        ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    else
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(if (pendingApprove) "Approve" else "Revoke") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val count = (partnersState as? Resource.Success)?.data?.size ?: 0
                val active = (partnersState as? Resource.Success)?.data?.count { it.isVerified } ?: 0
                val pending = (partnersState as? Resource.Success)?.data?.count { !it.isVerified } ?: 0
                Column {
                    Text("$count Delivery Partners", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (active > 0) Text("$active active", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
                        if (pending > 0) Text("$pending pending", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1565C0))
                    }
                }
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null) }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search delivery partners…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null) }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        when (partnersState) {
            is Resource.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is Resource.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                    Text(partnersState.message ?: "Failed to load", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onRefresh) { Text("Retry") }
                }
            }
            is Resource.Success -> {
                val list = (partnersState.data ?: emptyList())
                    .filter { if (searchQuery.isBlank()) true else it.companyName.contains(searchQuery, ignoreCase = true) || it.contactPerson.contains(searchQuery, ignoreCase = true) }
                    .sortedWith(compareBy({ it.isVerified }, { it.companyName }))

                if (list.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Text("No delivery partners yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(list, key = { it.id }) { partner ->
                            DeliveryPartnerAdminCard(
                                partner = partner,
                                onApprove = {
                                    pendingApprovalId = partner.id
                                    pendingApprove = true
                                    showConfirmDialog = true
                                },
                                onRevoke = {
                                    pendingApprovalId = partner.id
                                    pendingApprove = false
                                    showConfirmDialog = true
                                }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryPartnerAdminCard(
    partner: DeliveryPartner,
    onApprove: () -> Unit,
    onRevoke: () -> Unit
) {
    val approvedColor = Color(0xFF2E7D32)
    val pendingColor  = Color(0xFF1565C0)
    var isExpanded    by remember { mutableStateOf(false) }
    var fullScreenUrl by remember { mutableStateOf<String?>(null) }

    fullScreenUrl?.let { url ->
        Dialog(
            onDismissRequest = { fullScreenUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { fullScreenUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
                IconButton(
                    onClick = { fullScreenUrl = null },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) { Icon(Icons.Default.Close, null, tint = Color.White) }
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // ── Header row ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(10.dp), color = if (partner.isVerified) approvedColor.copy(alpha = 0.1f) else pendingColor.copy(alpha = 0.1f)) {
                    Box(contentAlignment = Alignment.Center) {
                        if (partner.companyLogoUrl != null) {
                            AsyncImage(model = partner.companyLogoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Text(partner.companyName.take(1).uppercase(), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = if (partner.isVerified) approvedColor else pendingColor)
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(partner.companyName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        val (badgeCol, badgeLbl) = when {
                            partner.isVerified -> approvedColor to "ACTIVE"
                            partner.isRejected -> MaterialTheme.colorScheme.error to "REJECTED"
                            else               -> pendingColor to "PENDING"
                        }
                        Surface(color = badgeCol.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) {
                            Text(badgeLbl, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = badgeCol)
                        }
                    }
                    Text(partner.contactPerson, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(partner.contactPhone, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }

            // ── Expandable details ───────────────────────────────────────────────
            AnimatedVisibility(visible = isExpanded, enter = expandVertically(tween(200)), exit = shrinkVertically(tween(200))) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    if (partner.companyAddress.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(partner.companyAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                            Text("${partner.totalDeliveries} deliveries", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                            Text(String.format("%.1f ★", partner.rating), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Verification images
                    if (partner.verificationImages.isNotEmpty()) {
                        Text("Verification Documents  (${partner.verificationImages.size})", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(partner.verificationImages) { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { fullScreenUrl = url },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    } else {
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BrokenImage, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("No verification documents uploaded", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            // ── Action buttons ───────────────────────────────────────────────────
            val approveActive = partner.isVerified
            val rejectActive  = partner.isRejected
            val red = MaterialTheme.colorScheme.error
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApprove, enabled = !approveActive, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = approvedColor, contentColor = Color.White, disabledContainerColor = approvedColor.copy(alpha = 0.35f), disabledContentColor = Color.White.copy(alpha = 0.6f))
                ) {
                    Icon(if (approveActive) Icons.Default.CheckCircle else Icons.Default.VerifiedUser, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (approveActive) "Approved" else "Approve", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onRevoke, enabled = !rejectActive, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = red, disabledContentColor = red.copy(alpha = 0.35f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (rejectActive) red.copy(alpha = 0.35f) else red)
                ) {
                    Icon(if (rejectActive) Icons.Default.Cancel else Icons.Default.Block, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (rejectActive) "Rejected" else "Reject", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
