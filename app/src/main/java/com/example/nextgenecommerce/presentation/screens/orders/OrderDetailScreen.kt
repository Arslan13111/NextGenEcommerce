package com.example.nextgenecommerce.presentation.screens.orders

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import com.example.nextgenecommerce.util.ColorUtils
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
    val returnRequestState by viewModel.returnRequestState.collectAsState()
    val selectedReturnImages by viewModel.selectedReturnImages.collectAsState()

    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }
    var showReturnDialog by remember { mutableStateOf(false) }
    var returnReason by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { viewModel.addReturnImage(it) }
    }

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

    LaunchedEffect(returnRequestState) {
        when (returnRequestState) {
            is Resource.Success -> {
                snackbarHostState.showSnackbar("Return request submitted. We'll review it shortly.")
                viewModel.resetReturnRequestState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar((returnRequestState as Resource.Error).message ?: "Failed to submit return request")
                viewModel.resetReturnRequestState()
            }
            else -> {}
        }
    }

    // Return Product Dialog
    if (showReturnDialog) {
        AlertDialog(
            onDismissRequest = { 
                showReturnDialog = false
                viewModel.clearReturnImages()
            },
            icon = { Icon(Icons.Default.AssignmentReturn, null, tint = Color(0xFF7B1FA2)) },
            title = { Text("Request Return") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Please describe the issue and provide photos of the product.")
                    OutlinedTextField(
                        value = returnReason,
                        onValueChange = { returnReason = it },
                        label = { Text("Reason for return") },
                        placeholder = { Text("e.g. Product is defective, Wrong item received…") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Image selection section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Product Photos", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                                Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Photos")
                            }
                        }

                        if (selectedReturnImages.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(selectedReturnImages) { uri ->
                                    Box(modifier = Modifier.size(80.dp)) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        IconButton(
                                            onClick = { viewModel.removeReturnImage(uri) },
                                            modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF7B1FA2).copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Info, null, tint = Color(0xFF7B1FA2), modifier = Modifier.size(16.dp))
                            Text(
                                "Your request will be reviewed. Refund is processed after we receive and inspect the item.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7B1FA2)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (returnReason.isNotBlank()) {
                            viewModel.requestReturn(orderId, returnReason.trim(), selectedReturnImages)
                            showReturnDialog = false
                            returnReason = ""
                        }
                    },
                    enabled = returnReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))
                ) {
                    Text("Submit Return Request")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { 
                    showReturnDialog = false
                    returnReason = ""
                    viewModel.clearReturnImages()
                }) {
                    Text("Cancel")
                }
            }
        )
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
                            SummaryLine("Subtotal", "PKR ${"%.0f".format(o.subtotal)}")
                            SummaryLine("Tax", "PKR ${"%.0f".format(o.tax)}")
                            SummaryLine(
                                "Shipping",
                                if (o.shipping == 0.0) "Free" else "PKR ${"%.0f".format(o.shipping)}"
                            )
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            SummaryLine(
                                "Total",
                                "PKR ${"%.0f".format(o.total)}",
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

                // Return status banners
                val returnStatuses = setOf(
                    OrderStatus.RETURN_REQUESTED, OrderStatus.RETURN_APPROVED,
                    OrderStatus.RETURN_IN_TRANSIT, OrderStatus.RETURN_RECEIVED,
                    OrderStatus.RETURNED, OrderStatus.RETURN_REJECTED
                )
                if (o.status in returnStatuses) {
                    item {
                        val (bannerColor, bannerIcon, bannerTitle, bannerBody) = when (o.status) {
                            OrderStatus.RETURN_REQUESTED -> arrayOf(
                                Color(0xFF7B1FA2), Icons.Default.AssignmentReturn,
                                "Awaiting Retailer Review",
                                "Your return request has been submitted and is pending review by the retailer."
                            )
                            OrderStatus.RETURN_APPROVED -> arrayOf(
                                Color(0xFF2E7D32), Icons.Default.CheckCircle,
                                "Return Approved – Awaiting Pickup",
                                "The retailer has approved your return. A delivery partner will contact you shortly to collect the item."
                            )
                            OrderStatus.RETURN_IN_TRANSIT -> arrayOf(
                                Color(0xFF1565C0), Icons.Default.LocalShipping,
                                "Return In Transit",
                                "The delivery partner has picked up your item and is returning it to the retailer."
                            )
                            OrderStatus.RETURN_RECEIVED -> arrayOf(
                                Color(0xFF00838F), Icons.Default.Inbox,
                                "Item Received by Retailer",
                                "The retailer has received your returned item and is inspecting it. Your refund will be processed shortly."
                            )
                            OrderStatus.RETURN_REJECTED -> arrayOf(
                                Color(0xFFD32F2F), Icons.Default.Cancel,
                                "Return Rejected",
                                o.adminReturnNote ?: "Your return request has been rejected by the retailer. Please contact support for more details."
                            )
                            else -> arrayOf( // RETURNED
                                Color(0xFF388E3C), Icons.Default.CheckCircle,
                                "Return Complete – Refund Processed",
                                o.adminReturnNote ?: "Your returned item has been verified. Your refund has been processed successfully."
                            )
                        }
                        @Suppress("UNCHECKED_CAST")
                        val color = bannerColor as Color
                        @Suppress("UNCHECKED_CAST")
                        val icon  = bannerIcon as androidx.compose.ui.graphics.vector.ImageVector

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors   = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                                    Text(bannerTitle as String, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = color)
                                }
                                Text(bannerBody as String, style = MaterialTheme.typography.bodyMedium)

                                // Show return images + reason for all return states
                                if (o.returnReason?.isNotBlank() == true) {
                                    Text("Reason: ${o.returnReason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (o.returnImages.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        items(o.returnImages) { imageUrl ->
                                            AsyncImage(
                                                model = imageUrl,
                                                contentDescription = null,
                                                modifier = Modifier.size(90.dp).clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                }

                                if (o.status == OrderStatus.RETURNED && o.paymentStatus == PaymentStatus.REFUNDED) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.15f)) {
                                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Payments, null, tint = color, modifier = Modifier.size(16.dp))
                                            Text("Refund processed successfully", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = color)
                                        }
                                    }
                                }
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
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Cancel, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cancel Order")
                            }
                        }
                    }
                }

                // Return button (only for DELIVERED orders, not already requested)
                if (o.status == OrderStatus.DELIVERED) {
                    item {
                        val isRequesting = returnRequestState is Resource.Loading
                        Button(
                            onClick = { showReturnDialog = true },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isRequesting,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2))
                        ) {
                            if (isRequesting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Icon(Icons.Default.AssignmentReturn, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Request Return", fontWeight = FontWeight.SemiBold)
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
        OrderStatus.PACKED to "Packed by Store",
        OrderStatus.READY_FOR_PICKUP to "Ready for Pickup",
        OrderStatus.SHIPPED to "Shipped",
        OrderStatus.DELIVERED to "Delivered"
    )

    val currentIndex = when (status) {
        OrderStatus.PENDING -> 0
        OrderStatus.CONFIRMED -> 1
        OrderStatus.PROCESSING -> 2
        OrderStatus.PACKED -> 3
        OrderStatus.READY_FOR_PICKUP -> 4
        OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY -> 5
        OrderStatus.DELIVERED -> 6
        OrderStatus.CANCELLED -> -1 // Special case handled by UI if needed
        else -> 0
    }

    val nonTrackable = setOf(
        OrderStatus.CANCELLED, OrderStatus.RETURN_REJECTED,
        OrderStatus.RETURN_REQUESTED, OrderStatus.RETURN_APPROVED,
        OrderStatus.RETURN_IN_TRANSIT, OrderStatus.RETURN_RECEIVED, OrderStatus.RETURNED
    )
    if (status in nonTrackable) return

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
                val isDone = index <= currentIndex && currentIndex != -1
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
                                        if (index < currentIndex && currentIndex != -1) MaterialTheme.colorScheme.primary
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
                            if (item.selectedColor.isNotBlank()) append("Color: ${ColorUtils.getColorDisplayName(item.selectedColor)}")
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
                        "PKR ${"%.0f".format(item.price * item.quantity)}",
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
        OrderStatus.PACKED -> MaterialTheme.colorScheme.tertiary to "Packed"
        OrderStatus.READY_FOR_PICKUP -> MaterialTheme.colorScheme.primary to "Ready for Pickup"
        OrderStatus.SHIPPED -> MaterialTheme.colorScheme.primary to "Shipped"
        OrderStatus.OUT_FOR_DELIVERY -> MaterialTheme.colorScheme.primary to "Out for Delivery"
        OrderStatus.DELIVERED -> Color(0xFF4CAF50) to "Delivered"
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error to "Cancelled"
        OrderStatus.RETURN_REQUESTED  -> Color(0xFF6A1B9A) to "Return Requested"
        OrderStatus.RETURN_APPROVED   -> Color(0xFF2E7D32) to "Return Approved"
        OrderStatus.RETURN_IN_TRANSIT -> Color(0xFF1565C0) to "Return In Transit"
        OrderStatus.RETURN_RECEIVED   -> Color(0xFF00838F) to "Return Received"
        OrderStatus.RETURNED          -> Color(0xFF6D4C41) to "Returned & Refunded"
        OrderStatus.RETURN_REJECTED   -> MaterialTheme.colorScheme.error to "Return Rejected"
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
    PaymentMethod.EASYPAISA -> "EasyPaisa"
    PaymentMethod.JAZZCASH -> "JazzCash"
    PaymentMethod.SAFEPAY -> "Safepay"
    PaymentMethod.CASH_ON_DELIVERY -> "Cash on Delivery"
    PaymentMethod.VAULT -> "My Vault"
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
