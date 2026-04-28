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
fun RevenueDetailScreen(
    navController: NavController,
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val allOrders by orderViewModel.allAdminOrders.collectAsState()
    
    LaunchedEffect(Unit) {
        orderViewModel.loadAllOrdersForAdmin()
    }
    
    // Calculations based on logic in AdminDashboardScreen
    val totalRevenue = allOrders.filter {
        it.paymentStatus != PaymentStatus.FAILED &&
        it.status != OrderStatus.CANCELLED &&
        it.paymentStatus != PaymentStatus.REFUNDED
    }.sumOf { it.total }

    val returnedAmount = allOrders.filter {
        it.paymentStatus == PaymentStatus.REFUNDED
    }.sumOf { it.total }

    val rejectedReturnsAmount = allOrders.filter {
        it.status == OrderStatus.RETURN_REJECTED
    }.sumOf { it.total }

    val pendingRefundsAmount = allOrders.filter {
        it.status == OrderStatus.RETURNED && it.paymentStatus != PaymentStatus.REFUNDED
    }.sumOf { it.total }
    
    // Approximate profit (assuming 20% margin for visualization)
    val estimatedProfit = totalRevenue * 0.2 

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Revenue Details", fontWeight = FontWeight.Bold) },
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
                RevenueSummaryCard(
                    totalRevenue = totalRevenue,
                    estimatedProfit = estimatedProfit
                )
            }

            item {
                Text(
                    "Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailRow(
                        title = "Gross Revenue",
                        amount = totalRevenue + returnedAmount,
                        icon = Icons.Default.Payments,
                        color = Color(0xFF0288D1)
                    )
                    DetailRow(
                        title = "Refunded Amount",
                        amount = returnedAmount,
                        icon = Icons.Default.History,
                        color = Color(0xFFD32F2F)
                    )
                    DetailRow(
                        title = "Rejected Returns",
                        amount = rejectedReturnsAmount,
                        icon = Icons.Default.Cancel,
                        color = Color(0xFF388E3C)
                    )
                    DetailRow(
                        title = "Pending Refunds",
                        amount = pendingRefundsAmount,
                        icon = Icons.Default.Schedule,
                        color = Color(0xFFF57C00)
                    )
                    Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    DetailRow(
                        title = "Net Revenue",
                        amount = totalRevenue,
                        icon = Icons.Default.AccountBalanceWallet,
                        color = MaterialTheme.colorScheme.primary,
                        isBold = true
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            val transactions = allOrders.sortedByDescending { it.createdAt }.take(20)
            items(transactions) { order ->
                TransactionItem(order = order)
            }
        }
    }
}

@Composable
private fun RevenueSummaryCard(totalRevenue: Double, estimatedProfit: Double) {
    val pkrFormat = NumberFormat.getCurrencyInstance(Locale("en", "PK")).apply {
        currency = Currency.getInstance("PKR")
        maximumFractionDigits = 0
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Total Net Revenue",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Text(
                pkrFormat.format(totalRevenue),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Est. Profit (20%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        pkrFormat.format(estimatedProfit),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF388E3C)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    title: String,
    amount: Double,
    icon: ImageVector,
    color: Color,
    isBold: Boolean = false
) {
    val pkrFormat = NumberFormat.getNumberInstance(Locale.US)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = if (isBold) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold) 
                    else MaterialTheme.typography.bodyMedium
        )
        
        Text(
            "PKR ${pkrFormat.format(amount)}",
            style = if (isBold) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold) 
                    else MaterialTheme.typography.bodyMedium,
            color = if (isBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TransactionItem(order: com.example.nextgenecommerce.data.models.Order) {
    val pkrFormat = NumberFormat.getNumberInstance(Locale.US)
    val date = java.text.SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(order.createdAt))
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "#${order.orderNumber}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "PKR ${pkrFormat.format(order.total)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                order.status.name.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " "),
                style = MaterialTheme.typography.labelSmall,
                color = when(order.status) {
                    OrderStatus.DELIVERED -> Color(0xFF388E3C)
                    OrderStatus.CANCELLED -> Color(0xFFD32F2F)
                    OrderStatus.RETURN_REJECTED -> Color(0xFFD32F2F)
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        }
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
