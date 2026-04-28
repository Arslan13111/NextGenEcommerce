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

                // Return reason banner (if return was requested or accepted)
                if (o.status == OrderStatus.RETURN_REQUESTED) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF7B1FA2).copy(alpha = 0.08f)),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.AssignmentReturn, null, tint = Color(0xFF7B1FA2), modifier = Modifier.size(18.dp))
                                    Text("Under Review", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF7B1FA2))
                                }
                                Text(o.returnReason ?: "Return request submitted and is pending review.", style = MaterialTheme.typography.bodyMedium)
                                
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
                                
                                Text("Your return request is being reviewed by our team. Once we inspect the product, we will notify you about the refund.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Admin return rejection banner
                if (o.status == OrderStatus.RETURN_REJECTED) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.Cancel, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    Text("Return Rejected", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.error)
                                }
                                Text(
                                    o.adminReturnNote ?: "Your return request has been rejected. Please contact support for more details.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text("If you believe this was an error, please reach out to our customer service.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Admin return note banner (return accepted by admin)
                if (o.status == OrderStatus.RETURNED) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF388E3C).copy(alpha = 0.08f)),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF388E3C), modifier = Modifier.size(18.dp))
                                    Text("Return Accepted", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF388E3C))
                                }
                                Text(
                                    o.adminReturnNote ?: "Your return has been accepted. After review of the product, your payment will be refunded.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (o.paymentStatus == PaymentStatus.REFUNDED) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF388E3C).copy(alpha = 0.15f)) {
                                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Payments, null, tint = Color(0xFF388E3C), modifier = Modifier.size(16.dp))
                                            Text("Refund processed successfully", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF388E3C))
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
        OrderStatus.SHIPPED -> MaterialTheme.colorScheme.primary to "Shipped"
        OrderStatus.OUT_FOR_DELIVERY -> MaterialTheme.colorScheme.primary to "Out for Delivery"
        OrderStatus.DELIVERED -> Color(0xFF4CAF50) to "Delivered"
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error to "Cancelled"
        OrderStatus.RETURN_REQUESTED -> Color(0xFF6A1B9A) to "Return Requested"
        OrderStatus.RETURNED -> Color(0xFF6D4C41) to "Returned"
        OrderStatus.RETURN_REJECTED -> MaterialTheme.colorScheme.error to "Return Rejected"
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
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
