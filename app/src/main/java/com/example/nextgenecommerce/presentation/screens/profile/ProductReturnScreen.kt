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
fun ProductReturnScreen(navController: NavController) {

    val steps = listOf(
        Triple(Icons.Default.ShoppingBag, "Request a Return", "Go to My Purchases, select the order and tap \"Return Item\". Fill in the reason and submit. You'll get a confirmation within 24 hours."),
        Triple(Icons.Default.LocalShipping, "Ship the Item Back", "Pack the product securely in its original packaging. Attach the prepaid return label we'll send you and drop it at any partner courier point."),
        Triple(Icons.Default.Verified, "Item Inspection", "Once we receive the item our team inspects it within 2 business days to confirm it meets return eligibility conditions."),
        Triple(Icons.Default.AccountBalanceWallet, "Refund Processed", "Approved refunds are returned to your original payment method within 5 – 7 business days. Store credit is instant.")
    )

    val nonReturnable = listOf(
        "Intimate apparel and swimwear (hygiene reasons)",
        "Customised or personalised items",
        "Items marked as Final Sale at checkout",
        "Products with removed tags or original packaging",
        "Items showing signs of wear, washing or damage"
    )

    val faqs = listOf(
        "How long do I have to return an item?" to "You have 30 days from the delivery date to initiate a return for most items.",
        "Do I pay for return shipping?" to "No — we provide a prepaid return label for all eligible returns within Pakistan.",
        "Can I exchange instead of returning?" to "Yes. Select \"Exchange\" instead of \"Return\" in My Purchases and choose your new size or colour.",
        "What if my item arrived damaged?" to "Contact Live Support immediately with photos. We'll arrange a free replacement or full refund — no need to ship anything back.",
        "When will I see the refund in my account?" to "Card refunds take 5 – 7 business days after approval. Store credit is credited instantly."
    )

    var expandedFaq by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Products Return",
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
            // Policy banner
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
                        Icon(Icons.Default.Undo, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "30-Day Hassle-Free\nReturns",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            "Free return shipping on all eligible items",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            }

            // Return window chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PolicyChip(icon = Icons.Default.Schedule, label = "30 days", sublabel = "return window")
                    PolicyChip(icon = Icons.Default.LocalShipping, label = "Free", sublabel = "return shipping")
                    PolicyChip(icon = Icons.Default.Bolt, label = "Instant", sublabel = "store credit")
                }
            }

            // How it works
            item {
                Text(
                    "How to Return",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp)
                )
            }

            items(steps.size) { idx ->
                val (icon, title, desc) = steps[idx]
                ReturnStepRow(
                    stepNumber = idx + 1,
                    icon = icon,
                    title = title,
                    description = desc,
                    isLast = idx == steps.lastIndex
                )
            }

            // Non-returnable items
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFFF3E0))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 10.dp)
                    ) {
                        Icon(Icons.Default.Info, null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
                        Text(
                            "Non-Returnable Items",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFE65100)
                        )
                    }
                    nonReturnable.forEach { item ->
                        Row(
                            modifier = Modifier.padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("•", color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                            Text(
                                item,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6D4C41)
                            )
                        }
                    }
                }
            }

            // FAQ
            item {
                Text(
                    "Common Questions",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
                )
            }

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

@Composable
private fun RowScope.PolicyChip(icon: ImageVector, label: String, sublabel: String) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = Color(0xFF111111))
        Text(label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), fontSize = 13.sp)
        Text(sublabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReturnStepRow(
    stepNumber: Int,
    icon: ImageVector,
    title: String,
    description: String,
    isLast: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Step indicator column
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center
            ) {
                Text("$stepNumber", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(60.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
            }
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}
