package com.example.nextgenecommerce.presentation.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.AuthViewModel
import com.example.nextgenecommerce.presentation.viewmodel.CartViewModel
import com.example.nextgenecommerce.presentation.viewmodel.OrderViewModel
import com.example.nextgenecommerce.util.CardTierUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel(),
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val userTier by orderViewModel.userTier.collectAsState()
    var showDiscountCard by remember { mutableStateOf(false) }
    var showPhotoViewer by remember { mutableStateOf(false) }

    // Full-screen photo viewer
    if (showPhotoViewer && currentUser?.profileImageUrl != null) {
        Dialog(
            onDismissRequest = { showPhotoViewer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showPhotoViewer = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = currentUser?.profileImageUrl,
                    contentDescription = "Profile photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(0.dp)),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { showPhotoViewer = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkAdminStatus()
    }

    // Discount card full-screen dialog
    if (showDiscountCard) {
        AlertDialog(
            onDismissRequest = { showDiscountCard = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = null,
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Full card
                    DiscountCardFull(
                        cardNumber = currentUser?.id?.take(16)?.uppercase() ?: "0000000000000000",
                        userName = currentUser?.name ?: "User",
                        tier = userTier
                    )

                    // Bonuses
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DiscountInfoBox(
                            label = "Card tier",
                            value = userTier.label,
                            modifier = Modifier.weight(1f)
                        )
                        DiscountInfoBox(
                            label = "Cashback",
                            value = "${userTier.cashbackPercent.toInt()}%",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    TextButton(
                        onClick = { showDiscountCard = false; navController.navigate(Screen.DiscountCard.route) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "More about Discount Card",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showDiscountCard = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Close")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text(
                "My profile",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(
                            enabled = currentUser?.profileImageUrl != null,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { showPhotoViewer = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (currentUser?.profileImageUrl != null) {
                        AsyncImage(
                            model = currentUser?.profileImageUrl,
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.AccountCircle,
                            null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    currentUser?.name ?: "Guest User",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // ── Menu rows ─────────────────────────────────────────────────────────
        ProfileMenuRow(icon = Icons.Default.AccountCircle, title = "My account") {
            navController.navigate(Screen.MyAccount.route)
        }
        ProfileMenuRow(icon = Icons.Default.Receipt, title = "My purchases") {
            navController.navigate(Screen.Orders.route)
        }
        ProfileMenuRow(icon = Icons.Default.LocalShipping, title = "Terms of delivery") {
            navController.navigate(Screen.DeliveryTerms.route)
        }
        ProfileMenuRow(icon = Icons.Default.Undo, title = "Products return") {
            navController.navigate(Screen.ProductReturn.route)
        }
        ProfileMenuRow(icon = Icons.Default.HeadsetMic, title = "Live support") {
            navController.navigate(Screen.HelpCenter.route)
        }
        ProfileMenuRow(icon = Icons.Default.Store, title = "Our stores") {
            navController.navigate(Screen.About.route)
        }
        ProfileMenuRow(icon = Icons.Default.Settings, title = "Settings") {
            navController.navigate(Screen.Settings.route)
        }

        if (isAdmin) {
            ProfileMenuRow(icon = Icons.Default.AdminPanelSettings, title = "Admin Panel") {
                navController.navigate(Screen.AdminDashboard.route)
            }
        }

        ProfileMenuRow(
            icon = Icons.Default.Logout,
            title = "Logout",
            titleColor = MaterialTheme.colorScheme.error,
            onClick = {
                cartViewModel.onUserLogout()
                viewModel.logout()
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // ── Discount card section ─────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showDiscountCard = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "My discount card",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Compact card preview — tappable
            DiscountCardCompact(
                cardNumber = currentUser?.id?.take(16)?.uppercase() ?: "0000000000000000",
                tier = userTier,
                onClick = { showDiscountCard = true }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ── Compact card (profile page preview) ──────────────────────────────────────
@Composable
private fun DiscountCardCompact(cardNumber: String, tier: CardTierUtil.CardTier, onClick: () -> Unit) {
    val gradient = when (tier) {
        CardTierUtil.CardTier.SILVER -> listOf(Color(0xFF1E88E5), Color(0xFF42A5F5), Color(0xFF64B5F6))
        CardTierUtil.CardTier.GOLD -> listOf(Color(0xFFE65100), Color(0xFFF57C00), Color(0xFFFFB74D))
        CardTierUtil.CardTier.PLATINUM -> listOf(Color(0xFF1B2631), Color(0xFF37474F), Color(0xFF546E7A))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors = gradient))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
    ) {
        // Background large "N" watermark
        Text(
            "N",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 120.sp
            ),
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 16.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "NEXTGEN CARD",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp
                    ),
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        tier.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = Color.White,
                        fontSize = 9.sp
                    )
                }
            }
            Text(
                formatCardNumber(cardNumber),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                ),
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

// ── Full card (dialog) ────────────────────────────────────────────────────────
@Composable
private fun DiscountCardFull(cardNumber: String, userName: String, tier: CardTierUtil.CardTier) {
    val gradient = when (tier) {
        CardTierUtil.CardTier.SILVER -> listOf(Color(0xFF1565C0), Color(0xFF1E88E5), Color(0xFF42A5F5))
        CardTierUtil.CardTier.GOLD -> listOf(Color(0xFFBF360C), Color(0xFFE64A19), Color(0xFFFF7043))
        CardTierUtil.CardTier.PLATINUM -> listOf(Color(0xFF1B2631), Color(0xFF37474F), Color(0xFF546E7A))
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(colors = gradient))
    ) {
        // Watermark letter
        Text(
            "N",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                fontSize = 160.sp
            ),
            color = Color.White.copy(alpha = 0.07f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 20.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(22.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "NEXTGEN CARD",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 3.sp
                        ),
                        color = Color.White
                    )
                    Text(
                        tier.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 9.sp
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
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }

            // Card number + barcode
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    userName,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.85f)
                )
                Text(
                    "Nr ${formatCardNumber(cardNumber)}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White
                )
                BarcodeVisual()
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
private fun BarcodeVisual() {
    val barPattern = remember {
        listOf(3, 1, 2, 1, 3, 2, 1, 1, 2, 3, 1, 2, 1, 1, 3, 2, 1, 2, 1, 3, 1, 2, 2, 1, 3, 1, 2, 1)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        barPattern.forEachIndexed { i, width ->
            Box(
                modifier = Modifier
                    .width(width.dp)
                    .fillMaxHeight()
                    .background(
                        if (i % 2 == 0) Color.White.copy(alpha = 0.9f)
                        else Color.Transparent
                    )
            )
        }
    }
}

@Composable
private fun DiscountInfoBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

private fun formatCardNumber(raw: String): String {
    val padded = raw.padEnd(16, '0').take(16)
    return padded.chunked(4).joinToString("  ")
}

// ── Shared menu row ───────────────────────────────────────────────────────────
@Composable
fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    titleColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (titleColor != Color.Unspecified) titleColor else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}
