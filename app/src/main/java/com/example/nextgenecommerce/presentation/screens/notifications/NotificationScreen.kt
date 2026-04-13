package com.example.nextgenecommerce.presentation.screens.notifications

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nextgenecommerce.data.models.NotificationEntity
import com.example.nextgenecommerce.data.models.NotificationType
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val notifications by notificationViewModel.notifications.collectAsState()
    val unreadCount by notificationViewModel.unreadCount.collectAsState()

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (unreadCount > 0) {
                            Text(
                                text = "$unreadCount unread",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                "More options",
                                tint = MaterialTheme.colorScheme.onSurface
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
                                        Icon(
                                            Icons.Default.DoneAll,
                                            null,
                                            tint = Color(0xFF4CAF50)
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                                Divider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            }

                            // Animated Delete Read Notifications
                            AnimatedDeleteReadMenuItem(
                                onClick = {
                                    notificationViewModel.deleteReadNotifications()
                                    showMenu = false
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
                                        Icon(
                                            Icons.Default.Delete,
                                            null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (notifications.isEmpty()) {
            // Empty state with animated icon
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
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
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
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
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
            // Notification list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
            ) {
                itemsIndexed(notifications, key = { _, item -> item.id }) { index, notification ->
                    var visible by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        delay(index * 50L) // Stagger animation for each item
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
                        ) + fadeIn(
                            animationSpec = tween(300)
                        )
                    ) {
                        Column {
                            NotificationItem(
                                notification = notification,
                                onClick = {
                                    notificationViewModel.markAsRead(notification.id)
                                    // Navigate based on notification type
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
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    notification: NotificationEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val icon = when (notification.type) {
        NotificationType.ORDER_UPDATE -> Icons.Default.ShoppingBag
        NotificationType.PROMOTION -> Icons.Default.LocalOffer
        NotificationType.PRODUCT_RESTOCK -> Icons.Default.Inventory
        NotificationType.PRICE_DROP -> Icons.Default.TrendingDown
        NotificationType.DELIVERY -> Icons.Default.LocalShipping
        NotificationType.PAYMENT -> Icons.Default.Payment
        else -> Icons.Default.Notifications
    }

    val iconColor = when (notification.type) {
        NotificationType.ORDER_UPDATE -> Color(0xFF4CAF50)
        NotificationType.PROMOTION -> Color(0xFFFF9800)
        NotificationType.PRODUCT_RESTOCK -> Color(0xFF2196F3)
        NotificationType.PRICE_DROP -> Color(0xFF9C27B0)
        NotificationType.DELIVERY -> Color(0xFF00BCD4)
        NotificationType.PAYMENT -> Color(0xFFE91E63)
        else -> MaterialTheme.colorScheme.primary
    }

    // Animated pulse for unread notifications
    val infiniteTransition = rememberInfiniteTransition(label = "iconPulse")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = if (!notification.isRead) 0.95f else 1f,
        targetValue = if (!notification.isRead) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    // Animated pulse for unread badge
    val badgeScale by infiniteTransition.animateFloat(
        initialValue = if (!notification.isRead) 1f else 1f,
        targetValue = if (!notification.isRead) 1.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgeScale"
    )

    val badgeAlpha by infiniteTransition.animateFloat(
        initialValue = if (!notification.isRead) 1f else 1f,
        targetValue = if (!notification.isRead) 0.6f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgeAlpha"
    )

    val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(notification.createdAt))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (!notification.isRead) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                } else {
                    Color.Transparent
                }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Animated Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(if (!notification.isRead) iconScale else 1f)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = if (!notification.isRead) 0.15f else 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .scale(badgeScale)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = badgeAlpha))
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete notification",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedDeleteReadMenuItem(
    onClick: () -> Unit
) {
    // Rotating animation for the delete icon
    val infiniteTransition = rememberInfiniteTransition(label = "deleteRotation")

    val rotation by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val colorAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "colorAnimation"
    )

    val animatedColor = androidx.compose.ui.graphics.lerp(
        Color(0xFFFF9800), // Orange
        Color(0xFFFF5722), // Deep Orange
        colorAnimation
    )

    DropdownMenuItem(
        text = {
            Text(
                "Delete read notifications",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        onClick = onClick,
        leadingIcon = {
            Icon(
                Icons.Default.DeleteSweep,
                contentDescription = null,
                tint = animatedColor,
                modifier = Modifier
                    .graphicsLayer(
                        rotationZ = rotation,
                        scaleX = scale,
                        scaleY = scale
                    )
            )
        },
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}
