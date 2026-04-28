package com.example.nextgenecommerce.presentation.screens.notifications

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nextgenecommerce.data.models.NotificationEntity
import com.example.nextgenecommerce.data.models.NotificationType
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

@Composable
fun NotificationScreen(
    navController: NavController,
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val notifications by notificationViewModel.notifications.collectAsState()
    val unreadCount by notificationViewModel.unreadCount.collectAsState()

    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Custom top bar ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "< Back"
            Row(
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { navController.popBackStack() }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "Back",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 15.sp
                )
            }

            // Unread count + options menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (unreadCount > 0) {
                    Text(
                        text = unreadCount.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .width(240.dp)
                    ) {
                        if (unreadCount > 0) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Mark all as read",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    notificationViewModel.markAllAsRead()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.DoneAll, null, tint = Color(0xFF4CAF50))
                                }
                            )
                            Divider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Delete read notifications",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                notificationViewModel.deleteReadNotifications()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.DeleteSweep, null, tint = Color(0xFFFF9800))
                            }
                        )
                        if (notifications.isNotEmpty()) {
                            Divider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Clear all notifications",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    notificationViewModel.clearAllNotifications()
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                }
                            )
                        }
                    }
                }
            }
        }

        // ── "Notifications" title ────────────────────────────────────────
        Text(
            text = "Notifications",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (notifications.isEmpty()) {
            // Empty state
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "No notifications",
                    modifier = Modifier
                        .size(120.dp)
                        .scale(scale),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "No notifications yet",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "We'll notify you when something new arrives",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(notifications, key = { _, item -> item.id }) { index, notification ->
                    var visible by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        delay(index * 50L)
                        visible = true
                    }

                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + fadeIn(animationSpec = tween(300))
                    ) {
                        NotificationCard(
                            notification = notification,
                            onClick = {
                                notificationViewModel.markAsRead(notification.id)
                                when (notification.type) {
                                    NotificationType.ORDER_UPDATE, NotificationType.DELIVERY -> {
                                        notification.orderId?.let {
                                            navController.navigate(Screen.OrderDetail.createRoute(it))
                                        }
                                    }
                                    NotificationType.PRODUCT_RESTOCK, NotificationType.PRICE_DROP -> {
                                        notification.productId?.let {
                                            navController.navigate(Screen.ProductDetail.createRoute(it))
                                        }
                                    }
                                    else -> {}
                                }
                            },
                            onDelete = {
                                notificationViewModel.deleteNotification(notification.id)
                            }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val typeLabel = when (notification.type) {
        NotificationType.ORDER_UPDATE -> "Order updates"
        NotificationType.PROMOTION -> "Promotion"
        NotificationType.PRODUCT_RESTOCK -> "New arrival"
        NotificationType.PRICE_DROP -> "Price drop"
        NotificationType.DELIVERY -> "Delivery"
        NotificationType.PAYMENT -> "Payment"
        else -> "Notification"
    }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val dateStr = dateFormat.format(Date(notification.createdAt))

    val iconColor = when (notification.type) {
        NotificationType.ORDER_UPDATE -> Color(0xFF4CAF50)
        NotificationType.PROMOTION -> Color(0xFFFF9800)
        NotificationType.PRODUCT_RESTOCK -> Color(0xFF2196F3)
        NotificationType.PRICE_DROP -> Color(0xFF9C27B0)
        NotificationType.DELIVERY -> Color(0xFF00BCD4)
        NotificationType.PAYMENT -> Color(0xFFE91E63)
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false
            )
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Thumbnail — product image or colored icon box
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            if (!notification.productId.isNullOrBlank()) {
                // Show product image placeholder with type icon
                val icon = when (notification.type) {
                    NotificationType.ORDER_UPDATE -> Icons.Default.ShoppingBag
                    NotificationType.PROMOTION -> Icons.Default.LocalOffer
                    NotificationType.PRODUCT_RESTOCK -> Icons.Default.Inventory
                    NotificationType.PRICE_DROP -> Icons.Default.TrendingDown
                    NotificationType.DELIVERY -> Icons.Default.LocalShipping
                    NotificationType.PAYMENT -> Icons.Default.Payment
                    else -> Icons.Default.Notifications
                }
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(32.dp))
            } else {
                val icon = when (notification.type) {
                    NotificationType.ORDER_UPDATE -> Icons.Default.ShoppingBag
                    NotificationType.PROMOTION -> Icons.Default.LocalOffer
                    NotificationType.PRODUCT_RESTOCK -> Icons.Default.Inventory
                    NotificationType.PRICE_DROP -> Icons.Default.TrendingDown
                    NotificationType.DELIVERY -> Icons.Default.LocalShipping
                    NotificationType.PAYMENT -> Icons.Default.Payment
                    else -> Icons.Default.Notifications
                }
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(32.dp))
            }

            // "New" badge if product restock
            if (notification.type == NotificationType.PRODUCT_RESTOCK) {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text("New", fontSize = 8.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                }
            }
        }

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = notification.title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }

        // Unread dot or delete button
        if (!notification.isRead) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        } else {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
