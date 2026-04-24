package com.example.nextgenecommerce.presentation.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nextgenecommerce.data.models.*
import com.example.nextgenecommerce.presentation.viewmodel.OrderViewModel
import com.example.nextgenecommerce.util.Resource
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    navController: NavController,
    orderId: String,
    viewModel: OrderViewModel = hiltViewModel()
) {
    val order by viewModel.selectedOrder.collectAsState()
    val cancelState by viewModel.cancelOrderState.collectAsState()

    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(orderId) {
        viewModel.loadOrderById(orderId)
    }

    LaunchedEffect(cancelState) {
        when (cancelState) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar("Order cancelled successfully")
                viewModel.resetCancelOrderState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar((cancelState as Resource.Error).message ?: "Failed to cancel order")
                viewModel.resetCancelOrderState()
            }
            else -> {}
        }
    }

    // Cancel Order Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            icon = {
                Icon(Icons.Default.Cancel, null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Cancel Order") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Please tell us why you want to cancel this order.")
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Reason for cancellation") },
                        placeholder = { Text("e.g. Changed my mind, Found a better price…") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cancelReason.isNotBlank()) {
                            viewModel.cancelOrder(orderId, cancelReason.trim())
                            showCancelDialog = false
                            cancelReason = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = cancelReason.isNotBlank()
                ) {
                    Text("Confirm Cancel")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showCancelDialog = false
                    cancelReason = ""
                }) {
                    Text("Keep Order")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Order Details",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (order == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val o = order!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Order header
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Order #${o.id.take(8).uppercase()}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        formatDate(o.createdAt),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OrderStatusBadge(o.status)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                PaymentBadge(o.paymentStatus)
                                Text(
                                    formatPaymentMethod(o.paymentMethod),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Order Tracking Timeline
                item {
                    OrderTrackingTimeline(status = o.status)
                }

                // Items
                item {
                    Text(
                        "Items (${o.items.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(o.items) { item ->
                    OrderItemRow(item = item)
                }

                // Shipping Address
                o.shippingAddress?.let { address ->
                    item {
                        DetailSection(title = "Delivery Address") {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        "${address.fullName} · ${address.label}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        address.phone,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "${address.addressLine1}, ${address.city}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (address.province.isNotBlank()) {
                                        Text(
                                            "${address.province}, ${address.country}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Order Summary
                item {
                    DetailSection(title = "Order Summary") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SummaryLine("Subtotal", "Rs. ${"%.0f".format(o.subtotal)}")
                            SummaryLine("Tax", "Rs. ${"%.0f".format(o.tax)}")
                            SummaryLine(
                                "Shipping",
                                if (o.shipping == 0.0) "Free" else "Rs. ${"%.0f".format(o.shipping)}"
                            )
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            SummaryLine(
                                "Total",
                                "Rs. ${"%.0f".format(o.total)}",
                                bold = true
                            )
                        }
                    }
                }

                // Cancellation reason (if cancelled)
                if (o.status == OrderStatus.CANCELLED && !o.cancellationReason.isNullOrBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Cancel,
                                        null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "Cancellation Reason",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    o.cancellationReason,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Cancel Order button (only for cancellable statuses)
                val cancellable = o.status == OrderStatus.PENDING || o.status == OrderStatus.CONFIRMED
                if (cancellable) {
                    item {
                        val isCancelling = cancelState is Resource.Loading
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isCancelling,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
                            )
                        ) {
                            if (isCancelling) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Cancel, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cancel Order")
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun OrderTrackingTimeline(status: OrderStatus) {
    val steps = listOf(
        OrderStatus.PENDING to "Order Placed",
        OrderStatus.CONFIRMED to "Confirmed",
        OrderStatus.PROCESSING to "Processing",
        OrderStatus.SHIPPED to "Shipped",
        OrderStatus.DELIVERED to "Delivered"
    )

    val currentIndex = when (status) {
        OrderStatus.PENDING -> 0
        OrderStatus.CONFIRMED -> 1
        OrderStatus.PROCESSING -> 2
        OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY -> 3
        OrderStatus.DELIVERED -> 4
        else -> 0
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Order Tracking",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            steps.forEachIndexed { index, (_, label) ->
                val isDone = index <= currentIndex
                val isCurrent = index == currentIndex
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    if (isDone) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        if (index < steps.size - 1) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(32.dp)
                                    .background(
                                        if (index < currentIndex) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(top = 2.dp)) {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDone) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        if (index < steps.size - 1) Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderItemRow(item: OrderItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = item.productImage,
                    contentDescription = item.productName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.productName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.selectedSize.isNotBlank() || item.selectedColor.isNotBlank()) {
                    Text(
                        buildString {
                            if (item.selectedSize.isNotBlank()) append("Size: ${item.selectedSize}")
                            if (item.selectedSize.isNotBlank() && item.selectedColor.isNotBlank()) append(" · ")
                            if (item.selectedColor.isNotBlank()) append("Color: ${item.selectedColor}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Qty: ${item.quantity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Rs. ${"%.0f".format(item.price * item.quantity)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = if (bold) MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            else MaterialTheme.typography.bodyMedium,
            color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = if (bold) MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            else MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun OrderStatusBadge(status: OrderStatus) {
    val (color, label) = when (status) {
        OrderStatus.PENDING, OrderStatus.CONFIRMED -> MaterialTheme.colorScheme.secondary to "Pending"
        OrderStatus.PROCESSING -> MaterialTheme.colorScheme.tertiary to "Processing"
        OrderStatus.SHIPPED -> MaterialTheme.colorScheme.primary to "Shipped"
        OrderStatus.OUT_FOR_DELIVERY -> MaterialTheme.colorScheme.primary to "Out for Delivery"
        OrderStatus.DELIVERED -> Color(0xFF4CAF50) to "Delivered"
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error to "Cancelled"
        OrderStatus.RETURNED -> MaterialTheme.colorScheme.error to "Returned"
    }
    Surface(shape = MaterialTheme.shapes.small, color = color.copy(alpha = 0.1f)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
    }
}

@Composable
private fun PaymentBadge(status: PaymentStatus) {
    val (color, label) = when (status) {
        PaymentStatus.PENDING -> MaterialTheme.colorScheme.secondary to "Payment Pending"
        PaymentStatus.COMPLETED -> Color(0xFF4CAF50) to "Paid"
        PaymentStatus.FAILED -> MaterialTheme.colorScheme.error to "Payment Failed"
        PaymentStatus.REFUNDED -> MaterialTheme.colorScheme.tertiary to "Refunded"
    }
    Surface(shape = MaterialTheme.shapes.small, color = color.copy(alpha = 0.1f)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = color
        )
    }
}

private fun formatPaymentMethod(method: PaymentMethod): String = when (method) {
    PaymentMethod.SAFEPAY -> "Safepay"
    PaymentMethod.CASH_ON_DELIVERY -> "Cash on Delivery"
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
