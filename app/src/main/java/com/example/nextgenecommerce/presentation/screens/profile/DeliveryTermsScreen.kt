package com.example.nextgenecommerce.presentation.screens.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryTermsScreen(navController: NavController) {

    val deliveryOptions = listOf(
        DeliveryOption(
            icon = Icons.Default.LocalShipping,
            title = "Standard Delivery",
            duration = "3 – 5 business days",
            price = "PKR 150",
            description = "Your order is packed and dispatched within 24 hours. Tracking updates are sent via SMS and email. Available across all major cities in Pakistan."
        ),
        DeliveryOption(
            icon = Icons.Default.FlashOn,
            title = "Express Delivery",
            duration = "1 – 2 business days",
            price = "PKR 350",
            description = "Priority handling and same-day dispatch for orders placed before 12 PM. Available in Karachi, Lahore, and Islamabad only."
        ),
        DeliveryOption(
            icon = Icons.Default.Store,
            title = "Click & Collect",
            duration = "Ready in 2 hours",
            price = "Free",
            description = "Pick up your order from any of our partner stores at zero extra cost. You'll receive a notification when your order is ready."
        ),
        DeliveryOption(
            icon = Icons.Default.Public,
            title = "International Shipping",
            duration = "7 – 14 business days",
            price = "Calculated at checkout",
            description = "We ship worldwide. Duties and customs fees are the buyer's responsibility and vary by destination country."
        )
    )

    val faqs = listOf(
        "How do I track my shipment?" to "Once your order is dispatched you'll receive a tracking number via SMS and email. Use it on the courier's website or inside the app under My Purchases.",
        "Can I change my delivery address after placing an order?" to "Address changes are accepted within 1 hour of order placement. Contact Live Support immediately if you need to update it.",
        "What if I miss the delivery?" to "The courier will attempt delivery twice. After two failed attempts the package is held at the nearest depot for 5 days before being returned to us.",
        "Do you deliver on weekends?" to "Standard and Express deliveries operate Monday – Saturday. Sunday deliveries are available in Karachi for an additional PKR 100 surcharge.",
        "When is delivery free?" to "Enjoy free Standard Delivery on all orders above PKR 3,000."
    )

    var expandedFaq by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Terms of Delivery",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Hero banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF111111))
                        .padding(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            Icons.Default.LocalShipping,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Free delivery on orders\nover PKR 3,000",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            "Fast, reliable shipping across Pakistan",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            // Delivery options title
            item {
                Text(
                    "Delivery Options",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Delivery option cards
            items(deliveryOptions.size) { idx ->
                DeliveryOptionCard(option = deliveryOptions[idx])
            }

            // FAQ title
            item {
                Text(
                    "Common Questions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            // FAQ items
            items(faqs.size) { idx ->
                val (q, a) = faqs[idx]
                val expanded = expandedFaq == idx
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clickable { expandedFaq = if (expanded) null else idx },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                q,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    a,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class DeliveryOption(
    val icon: ImageVector,
    val title: String,
    val duration: String,
    val price: String,
    val description: String
)

@Composable
private fun DeliveryOptionCard(option: DeliveryOption) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center
            ) {
                Icon(option.icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        option.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        option.price,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (option.price == "Free") Color(0xFF43A047) else MaterialTheme.colorScheme.onBackground
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        option.duration,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
