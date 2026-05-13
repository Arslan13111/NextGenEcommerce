package com.example.nextgenecommerce.presentation.screens.delivery

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nextgenecommerce.data.models.*
import com.example.nextgenecommerce.data.repository.EarningsStats
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.AuthViewModel
import com.example.nextgenecommerce.presentation.viewmodel.DeliveryViewModel
import com.example.nextgenecommerce.util.Resource
import java.text.SimpleDateFormat
import java.util.*

private val DPrimary = Color(0xFFFF6D00)
private val DSuccess = Color(0xFF2E7D32)
private val DInfo    = Color(0xFF1565C0)
private val DWarning = Color(0xFFE65100)
private val DGold    = Color(0xFFFFAB00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDashboardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    deliveryViewModel: DeliveryViewModel = hiltViewModel()
) {
    val currentUser     by authViewModel.currentUser.collectAsState()
    val profile         by deliveryViewModel.profile.collectAsState()
    val profileState    by deliveryViewModel.profileState.collectAsState()
    val availableOrders by deliveryViewModel.availableOrders.collectAsState()
    val myOrders        by deliveryViewModel.myOrders.collectAsState()
    val earningsStats   by deliveryViewModel.earningsStats.collectAsState()
    val deliveryHistory by deliveryViewModel.deliveryHistory.collectAsState()
    val actionState     by deliveryViewModel.actionState.collectAsState()

    var selectedTab       by remember { mutableIntStateOf(0) }
    var showCreateProfile by remember { mutableStateOf(false) }
    val snackbar          = remember { SnackbarHostState() }

    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is Resource.Success -> { snackbar.showSnackbar("Profile saved!"); deliveryViewModel.resetActionState() }
            is Resource.Error   -> { snackbar.showSnackbar(s.message ?: "Error"); deliveryViewModel.resetActionState() }
            else -> {}
        }
    }
    LaunchedEffect(profileState) { if (profileState is Resource.Error) showCreateProfile = true }

    if (showCreateProfile) {
        CreateProfileDialog(
            onConfirm = { n, p, a, ph -> deliveryViewModel.createProfile(n, p, a, ph); showCreateProfile = false },
            onDismiss = { showCreateProfile = false }
        )
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Compact header (no wasted top space) ──────────────────────────
            val isOnline = profile?.isAvailable ?: false
            Surface(
                color    = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(36.dp).background(DPrimary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.LocalShipping, null, tint = DPrimary, modifier = Modifier.size(20.dp)) }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            profile?.companyName ?: currentUser?.name ?: "Delivery Partner",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Text(
                            profile?.contactPerson ?: currentUser?.email ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    // Online/Offline pill
                    Surface(
                        color  = if (isOnline) DSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape  = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .clickable { deliveryViewModel.setAvailability(!isOnline) }
                            .padding(end = 4.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(Modifier.size(7.dp).background(if (isOnline) DSuccess else Color.Gray, CircleShape))
                            Text(
                                if (isOnline) "ONLINE" else "OFFLINE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) DSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = {
                        authViewModel.logout()
                        navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                    }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Logout, "Logout", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            StatsStrip(earningsStats)
            val tabs = listOf("Overview", "Available", "Active", "History", "Profile")
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp,
                indicator = { pos ->
                    TabRowDefaults.Indicator(Modifier.tabIndicatorOffset(pos[selectedTab]), color = DPrimary)
                }
            ) {
                tabs.forEachIndexed { i, t ->
                    Tab(
                        selected = selectedTab == i,
                        onClick  = { selectedTab = i },
                        text     = { Text(t, fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
            when (selectedTab) {
                0 -> OverviewTab(earningsStats, myOrders) { deliveryViewModel.refreshAll() }
                1 -> AvailableTab(availableOrders, deliveryViewModel) { deliveryViewModel.refreshAvailableOrders() }
                2 -> ActiveTab(myOrders, deliveryViewModel) { deliveryViewModel.refreshMyOrders() }
                3 -> HistoryTab(deliveryHistory) { deliveryViewModel.refreshDeliveryHistory() }
                4 -> ProfileTab(profile, currentUser, deliveryViewModel, authViewModel, navController)
            }
        }
    }
}

@Composable
private fun StatsStrip(stats: Resource<EarningsStats>) {
    val s = (stats as? Resource.Success)?.data ?: EarningsStats()
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatChip("Today",      "${s.todayDeliveries} drops",       Icons.Default.LocalShipping,  DSuccess)
        StatChip("This Week",  "${s.weekDeliveries} drops",        Icons.Default.DateRange,       DInfo)
        StatChip("All Time",   "${s.totalDeliveries} total",       Icons.Default.Inventory,       DPrimary)
        StatChip("Earned Today","PKR ${s.todayEarnings.toInt()}",  Icons.Default.TrendingUp,      DSuccess)
        StatChip("Week Earn",  "PKR ${s.weekEarnings.toInt()}",    Icons.Default.Payments,        DInfo)
        StatChip("Total Earned","PKR ${s.totalEarnings.toInt()}", Icons.Default.MonetizationOn,  DGold)
        if (s.pendingCodAmount > 0)
            StatChip("COD Due", "PKR ${s.pendingCodAmount.toInt()}", Icons.Default.Money,         DWarning)
        StatChip("Rating",     String.format("%.1f ★", s.rating),  Icons.Default.Star,            DGold)
    }
}

@Composable
private fun StatChip(label: String, value: String, icon: ImageVector, color: Color) {
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Overview Tab ─────────────────────────────────────────────────────────────

@Composable
private fun OverviewTab(stats: Resource<EarningsStats>, myOrders: Resource<List<Order>>, onRefresh: () -> Unit) {
    val s      = (stats as? Resource.Success)?.data ?: EarningsStats()
    val active = (myOrders as? Resource.Success)?.data
        ?.filter { it.status in listOf(OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY) }
        ?: emptyList()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Performance Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Today's Drops",   "${s.todayDeliveries}", Icons.Default.LocalShipping, DSuccess, Modifier.weight(1f))
                MetricCard("Today's Earning", "PKR ${s.todayEarnings.toInt()}", Icons.Default.AttachMoney, DInfo, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Week Drops",   "${s.weekDeliveries}", Icons.Default.DateRange, DInfo, Modifier.weight(1f))
                MetricCard("Week Earning", "PKR ${s.weekEarnings.toInt()}", Icons.Default.Payments, DSuccess, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Total Drops",  "${s.totalDeliveries}", Icons.Default.Inventory, DPrimary, Modifier.weight(1f))
                MetricCard("Total Earning","PKR ${s.totalEarnings.toInt()}", Icons.Default.TrendingUp, DGold, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Active Jobs", "${active.size}", Icons.Default.DirectionsBike, DPrimary, Modifier.weight(1f))
                MetricCard("Rating",      String.format("%.1f", s.rating), Icons.Default.Star, DGold, Modifier.weight(1f))
            }
        }
        if (s.pendingCodAmount > 0) {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DWarning.copy(alpha = 0.1f)), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Money, null, tint = DWarning, modifier = Modifier.size(28.dp))
                        Column {
                            Text("Pending COD Collection", fontWeight = FontWeight.Bold, color = DWarning)
                            Text("Collect PKR ${s.pendingCodAmount.toInt()} in cash from customer(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        if (active.isNotEmpty()) {
            item { Text("Active Deliveries", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
            items(active.take(3), key = { it.id }) { CompactOrderRow(it) }
        }
        item {
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Refresh All Data")
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CompactOrderRow(order: Order) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = DPrimary.copy(alpha = 0.06f))) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.LocalShipping, null, tint = DPrimary, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f)) {
                Text(order.orderNumber, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Text(order.status.name.replace("_", " "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("PKR ${order.total.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = DPrimary)
        }
    }
}

// ── Available Tab ─────────────────────────────────────────────────────────────

@Composable
private fun AvailableTab(state: Resource<List<Order>>, deliveryViewModel: DeliveryViewModel, onRefresh: () -> Unit) {
    when (state) {
        is Resource.Loading -> LoadingState()
        is Resource.Error   -> ErrorState(state.message ?: "Error", onRefresh)
        is Resource.Success -> {
            val list = state.data ?: emptyList()
            if (list.isEmpty()) EmptyState("No orders awaiting pickup.\nCheck back soon!", Icons.Default.LocalShipping)
            else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(list, key = { it.id }) { order -> 
                    AvailableOrderCard(order) { deliveryViewModel.acceptOrder(order.id) } 
                }
            }
        }
    }
}

@Composable
private fun AvailableOrderCard(order: Order, onAccept: () -> Unit) {
    var expanded    by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) AlertDialog(
        onDismissRequest = { showConfirm = false },
        title = { Text("Accept Order ${order.orderNumber}?") },
        text  = { Text("You will be responsible for delivering this order to the customer.") },
        confirmButton = { Button(onClick = { showConfirm = false; onAccept() }, colors = ButtonDefaults.buttonColors(containerColor = DSuccess)) { Text("Accept") } },
        dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancel") } }
    )

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(order.orderNumber, fontWeight = FontWeight.Bold)
                    Text("${order.items.sumOf { it.quantity }} item(s) · PKR ${order.total.toInt()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusChip("READY FOR PICKUP", Color(0xFF6A1B9A))
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
            AnimatedVisibility(expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Text("Items", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    order.items.forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.quantity}× ${item.productName}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("PKR ${(item.price * item.quantity).toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    AddressBlock(order.shippingAddress)
                }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            OrderJourney(order.status)
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            PaymentRow(order)
            Button(
                onClick = { showConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = DSuccess)
            ) {
                Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Accept & Pick Up", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Active Tab ────────────────────────────────────────────────────────────────

@Composable
private fun ActiveTab(state: Resource<List<Order>>, deliveryViewModel: DeliveryViewModel, onRefresh: () -> Unit) {
    val active = (state as? Resource.Success)?.data
        ?.filter { it.status in listOf(OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY) }
        ?: emptyList()
    when (state) {
        is Resource.Loading -> LoadingState()
        is Resource.Error   -> ErrorState(state.message ?: "Error", onRefresh)
        is Resource.Success -> {
            if (active.isEmpty())
                EmptyState("No active deliveries.\nAccept an order to get started!", Icons.Default.DirectionsBike)
            else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(active, key = { it.id }) { ActiveOrderCard(it, deliveryViewModel) }
            }
        }
    }
}

@Composable
private fun ActiveOrderCard(order: Order, deliveryViewModel: DeliveryViewModel) {
    val context = LocalContext.current
    var expanded    by remember { mutableStateOf(true) }
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) AlertDialog(
        onDismissRequest = { showConfirm = false },
        title = { Text("Confirm Delivery") },
        text  = { Text("Has the customer received order ${order.orderNumber}?") },
        confirmButton = { Button(onClick = { showConfirm = false; deliveryViewModel.markDelivered(order.id) }, colors = ButtonDefaults.buttonColors(containerColor = DSuccess)) { Text("Yes, Delivered") } },
        dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancel") } }
    )

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(3.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(order.orderNumber, fontWeight = FontWeight.Bold)
                    Text("${order.items.sumOf { it.quantity }} item(s) · PKR ${order.total.toInt()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(order.status.name.replace("_", " "), DPrimary)
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
            AnimatedVisibility(expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    AddressBlock(order.shippingAddress)
                    order.shippingAddress?.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                        OutlinedButton(
                            onClick  = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Phone, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Call Customer: $phone")
                        }
                    }
                }
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            OrderJourney(order.status)
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            PaymentRow(order)
            
            if (order.status == OrderStatus.SHIPPED) {
                Button(
                    onClick  = { deliveryViewModel.startDelivery(order.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = DPrimary)
                ) {
                    Icon(Icons.Default.DirectionsBike, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start Delivery (Out for Delivery)", fontWeight = FontWeight.Bold)
                }
            } else if (order.status == OrderStatus.OUT_FOR_DELIVERY) {
                Button(
                    onClick  = { showConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = DSuccess)
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mark as Delivered", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── History Tab ───────────────────────────────────────────────────────────────

@Composable
private fun HistoryTab(state: Resource<List<Order>>, onRefresh: () -> Unit) {
    when (state) {
        is Resource.Loading -> LoadingState()
        is Resource.Error   -> ErrorState(state.message ?: "Error", onRefresh)
        is Resource.Success -> {
            val list = state.data ?: emptyList()
            if (list.isEmpty()) EmptyState("No delivery history yet.\nComplete your first delivery!", Icons.Default.History)
            else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${list.size} completed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Total earned: PKR ${list.sumOf { it.shipping }.toInt()}", style = MaterialTheme.typography.bodySmall, color = DSuccess, fontWeight = FontWeight.SemiBold)
                    }
                }
                items(list, key = { it.id }) { HistoryCard(it) }
            }
        }
    }
}

@Composable
private fun HistoryCard(order: Order) {
    val fmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).background(DSuccess.copy(alpha = 0.12f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.CheckCircle, null, tint = DSuccess, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(order.orderNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(fmt.format(Date(order.createdAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${order.items.sumOf { it.quantity }} item(s) · ${order.paymentMethod.name.replace("_", " ")}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("PKR ${order.shipping.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = DSuccess)
                Text("earned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Profile Tab ───────────────────────────────────────────────────────────────

@Composable
private fun ProfileTab(
    profile: DeliveryPartner?,
    currentUser: User?,
    deliveryViewModel: DeliveryViewModel,
    authViewModel: AuthViewModel,
    navController: NavController
) {
    val isOnline = profile?.isAvailable ?: false
    // Editable fields pre-filled from profile
    var editCompany  by remember(profile) { mutableStateOf(profile?.companyName ?: "") }
    var editContact  by remember(profile) { mutableStateOf(profile?.contactPerson ?: "") }
    var editPhone    by remember(profile) { mutableStateOf(profile?.contactPhone ?: "") }
    var editAddress  by remember(profile) { mutableStateOf(profile?.companyAddress ?: "") }
    var editing      by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = DPrimary.copy(alpha = 0.08f)), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(72.dp).background(DPrimary.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.LocalShipping, null, tint = DPrimary, modifier = Modifier.size(36.dp))
                    }
                    Text(profile?.companyName ?: currentUser?.name ?: "Courier Company", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(currentUser?.email ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(color = if (isOnline) DSuccess.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(20.dp)) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(8.dp).background(if (isOnline) DSuccess else Color.Gray, CircleShape))
                            Text(if (isOnline) "ONLINE" else "OFFLINE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isOnline) DSuccess else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(
                        onClick = { deliveryViewModel.setAvailability(!isOnline) },
                        colors  = ButtonDefaults.buttonColors(containerColor = if (isOnline) MaterialTheme.colorScheme.error else DSuccess),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(if (isOnline) Icons.Default.WifiTetheringOff else Icons.Default.WifiTethering, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isOnline) "Go Offline" else "Go Online", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        profile?.let { p ->
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Company Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { editing = !editing }) {
                        Icon(if (editing) Icons.Default.Close else Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (editing) "Cancel" else "Edit")
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (editing) {
                            OutlinedTextField(editCompany, { editCompany = it },   label = { Text("Company Name") },    modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Business, null) })
                            OutlinedTextField(editContact, { editContact = it },   label = { Text("Contact Person") },  modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Person, null) })
                            OutlinedTextField(editPhone,   { editPhone = it },     label = { Text("Phone Number") },    modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Phone, null) })
                            OutlinedTextField(editAddress, { editAddress = it },   label = { Text("Head Office Address") }, modifier = Modifier.fillMaxWidth(), minLines = 2, leadingIcon = { Icon(Icons.Default.LocationOn, null) })
                            Button(
                                onClick = {
                                    deliveryViewModel.updateProfile(editCompany, editContact, editAddress, editPhone)
                                    editing = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled  = editCompany.isNotBlank() && editContact.isNotBlank() && editPhone.isNotBlank(),
                                colors   = ButtonDefaults.buttonColors(containerColor = DPrimary)
                            ) {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Save Changes", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            ProfileInfoRow(Icons.Default.Business,    "Company",        p.companyName)
                            ProfileInfoRow(Icons.Default.Person,       "Contact Person", p.contactPerson)
                            ProfileInfoRow(Icons.Default.Phone,        "Phone",          p.contactPhone)
                            ProfileInfoRow(Icons.Default.LocationOn,   "Address",        p.companyAddress)
                        }
                    }
                }
            }
            item { Text("Performance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Total Drops", "${p.totalDeliveries}", Icons.Default.Inventory, DPrimary, Modifier.weight(1f))
                    MetricCard("Rating", String.format("%.1f ★", p.rating), Icons.Default.Star, DGold, Modifier.weight(1f))
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { authViewModel.logout(); navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border   = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Logout", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Order Journey Timeline ───────────────────────────────────────────────────

private val journeySteps = listOf(
    OrderStatus.CONFIRMED         to "Confirmed",
    OrderStatus.PACKED            to "Packed",
    OrderStatus.READY_FOR_PICKUP  to "Ready",
    OrderStatus.OUT_FOR_DELIVERY  to "En Route",
    OrderStatus.DELIVERED         to "Delivered"
)

@Composable
private fun OrderJourney(current: OrderStatus) {
    val currentIdx = journeySteps.indexOfFirst { it.first == current }.coerceAtLeast(0)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Order Journey", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            journeySteps.forEachIndexed { idx, (_, label) ->
                val isDone    = idx < currentIdx
                val isCurrent = idx == currentIdx
                val color     = when { isCurrent -> DPrimary; isDone -> DSuccess; else -> Color.Gray.copy(alpha = 0.4f) }
                // Step dot
                Box(
                    Modifier.size(if (isCurrent) 10.dp else 8.dp).background(color, CircleShape)
                )
                // Label below via Column trick — use weight only on connectors
                if (idx < journeySteps.size - 1) {
                    Box(Modifier.weight(1f).height(2.dp).background(if (isDone) DSuccess else Color.Gray.copy(alpha = 0.25f)))
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            journeySteps.forEachIndexed { idx, (_, label) ->
                val isCurrent = idx == journeySteps.indexOfFirst { it.first == current }
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) DPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, null, tint = DPrimary, modifier = Modifier.size(18.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun AddressBlock(address: Address?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Delivery Address", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (address != null) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.LocationOn, null, tint = DPrimary, modifier = Modifier.size(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (address.fullName.isNotBlank()) Text(address.fullName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    if (address.phone.isNotBlank())    Text(address.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(buildString {
                        append(address.addressLine1)
                        if (address.addressLine2.isNotBlank()) append(", ${address.addressLine2}")
                    }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${address.city}, ${address.province} ${address.postalCode}".trim(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Info, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Contact customer via order number", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PaymentRow(order: Order) {
    val isCod = order.paymentMethod == PaymentMethod.CASH_ON_DELIVERY
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(if (isCod) Icons.Default.Money else Icons.Default.CreditCard, null, tint = if (isCod) DWarning else DInfo, modifier = Modifier.size(18.dp))
        Text(
            if (isCod) "COLLECT CASH: PKR ${order.total.toInt()}" else "Prepaid · PKR ${order.total.toInt()}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isCod) DWarning else DInfo
        )
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(20.dp)) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = DPrimary) }
}

@Composable
private fun EmptyState(message: String, icon: ImageVector) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.ErrorOutline, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = DPrimary)) { Text("Retry") }
        }
    }
}

// ── Create profile dialog ─────────────────────────────────────────────────────

@Composable
private fun CreateProfileDialog(onConfirm: (String, String, String, String) -> Unit, onDismiss: () -> Unit) {
    var companyName   by remember { mutableStateOf("") }
    var contactPerson by remember { mutableStateOf("") }
    var phone         by remember { mutableStateOf("") }
    var address       by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Courier Profile", fontWeight = FontWeight.Bold) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(companyName,   { companyName = it },   label = { Text("Courier Company Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(contactPerson, { contactPerson = it }, label = { Text("Contact Person Name") },  modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(phone,         { phone = it },         label = { Text("Contact Phone") },         modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(address,       { address = it },       label = { Text("Head Office Address") },   modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(companyName, contactPerson, address, phone) },
                enabled = companyName.isNotBlank() && contactPerson.isNotBlank() && phone.isNotBlank()
            ) { Text("Save Profile") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

