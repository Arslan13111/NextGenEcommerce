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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nextgenecommerce.data.models.AdminCommission
import com.example.nextgenecommerce.data.models.VaultSummary
import com.example.nextgenecommerce.presentation.viewmodel.AdminVaultViewModel
import com.example.nextgenecommerce.util.Resource
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevenueDetailScreen(
    navController: NavController,
    adminVaultViewModel: AdminVaultViewModel = hiltViewModel()
) {
    val vaultSummary by adminVaultViewModel.vaultSummary.collectAsState()
    val history by adminVaultViewModel.history.collectAsState()

    LaunchedEffect(Unit) {
        adminVaultViewModel.loadVaultSummary()
        adminVaultViewModel.loadHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Commission Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        adminVaultViewModel.loadVaultSummary()
                        adminVaultViewModel.loadHistory()
                    }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        val summaryData = when (val v = vaultSummary) {
            is Resource.Success -> v.data
            is Resource.Error   -> VaultSummary(0.0, 0.0, 0.0)
            else                -> null
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CommissionSummaryCard(summaryData = summaryData)
            }

            when (val h = history) {
                null, is Resource.Loading -> {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator() }
                    }
                }
                is Resource.Error -> {
                    item {
                        Text(
                            h.message ?: "Failed to load history",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is Resource.Success -> {
                    val entries = h.data
                        ?.sortedByDescending { it.createdAt }
                        ?: emptyList()

                    if (entries.isEmpty()) {
                        item {
                            Box(
                                Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AccountBalance,
                                        null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                    Text(
                                        "No commissions recorded yet",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            Text(
                                "Commission History  (${entries.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(entries, key = { it.id }) { commission ->
                            CommissionEntryRow(commission = commission)
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun CommissionSummaryCard(summaryData: VaultSummary?) {
    val pkrFormat = NumberFormat.getNumberInstance(Locale.US)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.AccountBalance,
                    null,
                    tint = Color(0xFF81C784),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    "Total Commission Earned",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            if (summaryData == null) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF81C784),
                    trackColor = Color(0xFF2E7D32)
                )
            } else {
                Text(
                    "PKR ${pkrFormat.format(summaryData.total.toLong())}",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
                Divider(color = Color(0xFF2E7D32))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "From Retailers (5%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA5D6A7)
                        )
                        Text(
                            "PKR ${pkrFormat.format(summaryData.retailerCommission.toLong())}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "From Deliveries (2%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA5D6A7)
                        )
                        Text(
                            "PKR ${pkrFormat.format(summaryData.deliveryCommission.toLong())}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommissionEntryRow(commission: AdminCommission) {
    val pkrFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val isRetailer = commission.commissionType == "RETAILER"
    val entryColor = if (isRetailer) Color(0xFF2E7D32) else Color(0xFF1565C0)
    val label      = if (isRetailer) "Retailer Sale (5%)" else "Delivery Fee (2%)"
    val icon       = if (isRetailer) Icons.Default.Store else Icons.Default.LocalShipping

    val dateStr = remember(commission.createdAt) {
        runCatching {
            val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                .parse(commission.createdAt)
            SimpleDateFormat("MMM d, yyyy  •  hh:mm a", Locale.getDefault()).format(parsed!!)
        }.getOrDefault(commission.createdAt.take(10))
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(entryColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = entryColor, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    "Order: …${commission.orderId.takeLast(8)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "+PKR ${pkrFormat.format(commission.amount.toLong())}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = entryColor
            )
        }
    }
}
