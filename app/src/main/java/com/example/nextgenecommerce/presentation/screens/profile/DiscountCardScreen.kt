package com.example.nextgenecommerce.presentation.screens.profile

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nextgenecommerce.presentation.viewmodel.AuthViewModel
import com.example.nextgenecommerce.presentation.viewmodel.OrderViewModel
import com.example.nextgenecommerce.util.CardTierUtil
import com.example.nextgenecommerce.util.CardTierUtil.CardTier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscountCardScreen(
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val totalSpent by orderViewModel.totalSpent.collectAsState()
    val currentTier by orderViewModel.userTier.collectAsState()

    val cardNumber = currentUser?.id?.take(16)?.uppercase() ?: "0000000000000000"
    val userName = currentUser?.name ?: "User"
    val nextTier = CardTierUtil.nextTier(currentTier)
    val progress = CardTierUtil.progressToNext(totalSpent, currentTier)
    val amountToNext = CardTierUtil.amountToNext(totalSpent, currentTier)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Discount Card",
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
            // ── Your card ─────────────────────────────────────────────────────
            item {
                Text(
                    "Your Card",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }

            item { FullDiscountCard(cardNumber = cardNumber, userName = userName, tier = currentTier) }

            // ── Stats row ─────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CardStatBox(
                        label = "Total spent",
                        value = CardTierUtil.formatAmount(totalSpent),
                        modifier = Modifier.weight(1f)
                    )
                    CardStatBox(
                        label = "Cashback",
                        value = "${currentTier.cashbackPercent.toInt()}%",
                        modifier = Modifier.weight(1f)
                    )
                    CardStatBox(
                        label = "Tier",
                        value = currentTier.label,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Progress to next tier ───────────────────────────────────────���─
            if (nextTier != null && amountToNext != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Progress to ${nextTier.label}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                "${CardTierUtil.formatAmount(amountToNext)} more",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = when (nextTier) {
                                CardTier.GOLD -> Color(0xFFF9A825)
                                CardTier.PLATINUM -> Color(0xFF546E7A)
                                else -> Color(0xFF1E88E5)
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                currentTier.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                nextTier.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "Spend ${nextTier.displayMinSpend} total to unlock ${nextTier.cashbackPercent.toInt()}% cashback",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF37474F))
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.WorkspacePremium, null, tint = Color(0xFFB0BEC5), modifier = Modifier.size(28.dp))
                        Column {
                            Text(
                                "You've reached the highest tier!",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                "Enjoy 12% cashback on every purchase",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // ── All tiers ─────────────────────────────────────────────────────
            item {
                Text(
                    "All Tiers",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)
                )
            }

            items(CardTier.entries.size) { idx ->
                val tier = CardTier.entries[idx]
                TierCard(tier = tier, isCurrent = tier == currentTier, isUnlocked = totalSpent >= tier.minSpend)
            }

            // ── How bonuses work ──────────────────────────────────────────────
            item {
                Text(
                    "How Bonuses Work",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    BonusInfoRow(
                        icon = Icons.Default.ShoppingBag,
                        title = "Automatic cashback at checkout",
                        subtitle = "Your card discount is applied automatically when you place an order. No coupon codes needed."
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    BonusInfoRow(
                        icon = Icons.Default.TrendingUp,
                        title = "Tier upgrades automatically",
                        subtitle = "Your tier upgrades as your total spending grows. Spend PKR 25,000 for Gold, PKR 75,000 for Platinum."
                    )
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    BonusInfoRow(
                        icon = Icons.Default.AccessTime,
                        title = "Cumulative spending",
                        subtitle = "All completed orders count towards your tier. Cancelled and returned orders are excluded."
                    )
                }
            }

            item {
                Text(
                    "Terms & Conditions apply. NextGen reserves the right to modify the loyalty programme at any time.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun FullDiscountCard(cardNumber: String, userName: String, tier: CardTier) {
    val gradient = when (tier) {
        CardTier.SILVER -> listOf(Color(0xFF1565C0), Color(0xFF1E88E5), Color(0xFF42A5F5))
        CardTier.GOLD -> listOf(Color(0xFFE65100), Color(0xFFF57C00), Color(0xFFFFB74D))
        CardTier.PLATINUM -> listOf(Color(0xFF1B2631), Color(0xFF37474F), Color(0xFF546E7A))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(colors = gradient))
    ) {
        Text(
            "N",
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Black, fontSize = 160.sp),
            color = Color.White.copy(alpha = 0.07f),
            modifier = Modifier.align(Alignment.CenterEnd).offset(x = 20.dp)
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "NEXTGEN CARD",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp),
                        color = Color.White
                    )
                    Text(
                        tier.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        "${tier.cashbackPercent.toInt()}% cashback",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    userName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
                Text(
                    "Nr ${formatCardNum(cardNumber)}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = 1.sp),
                    color = Color.White
                )
                CardBarcodeVisual()
                Text(
                    "Your NextGen discount card",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Composable
private fun TierCard(tier: CardTier, isCurrent: Boolean, isUnlocked: Boolean) {
    val gradient = when (tier) {
        CardTier.SILVER -> listOf(Color(0xFF9E9E9E), Color(0xFFBDBDBD), Color(0xFFE0E0E0))
        CardTier.GOLD -> listOf(Color(0xFFF9A825), Color(0xFFFFCA28), Color(0xFFFFE082))
        CardTier.PLATINUM -> listOf(Color(0xFF37474F), Color(0xFF546E7A), Color(0xFF78909C))
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                            .background(if (isUnlocked) Brush.linearGradient(gradient) else Brush.linearGradient(listOf(Color(0xFFBDBDBD), Color(0xFFE0E0E0)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isUnlocked) Icons.Default.CreditCard else Icons.Default.Lock,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(tier.label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Min. spend: ${tier.displayMinSpend}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(if (isUnlocked) Brush.linearGradient(gradient) else Brush.linearGradient(listOf(Color(0xFFBDBDBD), Color(0xFFBDBDBD))))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("${tier.cashbackPercent.toInt()}%", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }
                    if (isCurrent) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE8F5E9)).padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Current", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF2E7D32))
                        }
                    }
                }
            }
            val perks = when (tier) {
                CardTier.SILVER -> listOf("5% cashback on all purchases", "Free standard delivery on orders above PKR 3,000", "Early access to sales")
                CardTier.GOLD -> listOf("8% cashback on all purchases", "Free delivery on all orders", "Priority customer support", "Birthday bonus: 2× savings")
                CardTier.PLATINUM -> listOf("12% cashback on all purchases", "Free express delivery on all orders", "Dedicated account manager", "Exclusive platinum-only deals")
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                perks.forEach { perk ->
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            null,
                            tint = if (isUnlocked) Color(0xFF43A047) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp).padding(top = 1.dp)
                        )
                        Text(perk, style = MaterialTheme.typography.bodySmall,
                            color = if (isUnlocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CardStatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun BonusInfoRow(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun CardBarcodeVisual() {
    val barPattern = remember {
        listOf(3, 1, 2, 1, 3, 2, 1, 1, 2, 3, 1, 2, 1, 1, 3, 2, 1, 2, 1, 3, 1, 2, 2, 1, 3, 1, 2, 1)
    }
    Row(
        modifier = Modifier.fillMaxWidth().height(28.dp).padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        barPattern.forEachIndexed { i, width ->
            Box(
                modifier = Modifier.width(width.dp).fillMaxHeight()
                    .background(if (i % 2 == 0) Color.White.copy(alpha = 0.9f) else Color.Transparent)
            )
        }
    }
}

private fun formatCardNum(raw: String): String =
    raw.padEnd(16, '0').take(16).chunked(4).joinToString("  ")
