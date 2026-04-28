package com.example.nextgenecommerce.presentation.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nextgenecommerce.data.models.OrderStatus
import com.example.nextgenecommerce.data.models.PaymentStatus
import com.example.nextgenecommerce.presentation.viewmodel.OrderViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesDetailScreen(
    navController: NavController,
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val allOrders by orderViewModel.allAdminOrders.collectAsState()
    
    LaunchedEffect(Unit) {
        orderViewModel.loadAllOrdersForAdmin()
    }

    // Sales calculations
    val activeOrders = allOrders.filter { it.status != OrderStatus.CANCELLED }
    val totalItemsSold = activeOrders.sumOf { it.items.sumOf { item -> item.quantity } }
    val returnedItemsCount = allOrders.filter { it.status == OrderStatus.RETURNED || it.paymentStatus == PaymentStatus.REFUNDED }
        .sumOf { it.items.sumOf { item -> item.quantity } }
    
    val deliveredCount = allOrders.count { it.status == OrderStatus.DELIVERED }
    val processingCount = allOrders.count { it.status == OrderStatus.PROCESSING || it.status == OrderStatus.CONFIRMED || it.status == OrderStatus.SHIPPED }
    val pendingCount = allOrders.count { it.status == OrderStatus.PENDING }

    // Top selling products logic
    val productSalesMap = mutableMapOf<String, Int>()
    activeOrders.forEach { order ->
        order.items.forEach { item ->
            productSalesMap[item.productName] = (productSalesMap[item.productName] ?: 0) + item.quantity
        }
    }
    val topProducts = productSalesMap.toList().sortedByDescending { it.second }.take(10)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SalesSummaryHeader(
                    totalSold = totalItemsSold,
                    returnedCount = returnedItemsCount
                )
            }

            item {
                Text(
                    "Order Status Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusBox(
                        modifier = Modifier.weight(1f),
                        title = "Delivered",
                        count = deliveredCount,
                        color = Color(0xFF388E3C),
                        icon = Icons.Default.CheckCircle
                    )
                    StatusBox(
                        modifier = Modifier.weight(1f),
                        title = "In Progress",
                        count = processingCount,
                        color = Color(0xFF0288D1),
                        icon = Icons.Default.LocalShipping
                    )
                    StatusBox(
                        modifier = Modifier.weight(1f),
                        title = "Pending",
                        count = pendingCount,
                        color = Color(0xFFF57C00),
                        icon = Icons.Default.Schedule
                    )
                }
            }

            item {
                Text(
                    "Top Selling Products",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (topProducts.isEmpty()) {
                item {
                    Text(
                        "No sales data available yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(topProducts) { (name, count) ->
                    TopProductRow(name = name, count = count, totalSold = totalItemsSold)
                }
            }
        }
    }
}

@Composable
private fun SalesSummaryHeader(totalSold: Int, returnedCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF512DA8))
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Total Products Sold",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    "$totalSold",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Undo, null, tint = Color.Yellow, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "$returnedCount Returned",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBox(
    modifier: Modifier,
    title: String,
    count: Int,
    color: Color,
    icon: ImageVector
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "$count",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                title,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun TopProductRow(name: String, count: Int, totalSold: Int) {
    val percentage = if (totalSold > 0) count.toFloat() / totalSold else 0f
    
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Text(
                "$count sold",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = percentage,
            modifier = Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent
        )
    }
}
