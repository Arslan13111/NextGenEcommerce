package com.example.nextgenecommerce.presentation.screens.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nextgenecommerce.data.models.PushNotification
import com.example.nextgenecommerce.data.models.RecipientRole
import com.example.nextgenecommerce.presentation.viewmodel.SendNotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNotifyTab(
    modifier: Modifier = Modifier,
    senderId: String,
    viewModel: SendNotificationViewModel,
) {
    val sendState by viewModel.sendState.collectAsState()
    val recentNotifications by viewModel.recentNotifications.collectAsState()

    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(RecipientRole.ALL) }
    var showRoleDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadRecentNotifications("admin")
    }

    LaunchedEffect(sendState) {
        if (sendState is SendNotificationViewModel.SendState.Success) {
            title = ""
            message = ""
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Send Notification",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    "Broadcast a message to users by role.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Compose card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Recipient selector
                    Text(
                        "Recipients",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        RecipientRole.entries.forEach { role ->
                            val selected = selectedRole == role
                            FilterChip(
                                selected = selected,
                                onClick = { selectedRole = role },
                                label = { Text(role.label, fontSize = 12.sp) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Notification Title") },
                        placeholder = { Text("e.g. New Sale!") },
                        leadingIcon = { Icon(Icons.Default.Title, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Message
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message") },
                        placeholder = { Text("Write your message here...") },
                        leadingIcon = { Icon(Icons.Default.Message, null) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Error
                    AnimatedVisibility(
                        visible = sendState is SendNotificationViewModel.SendState.Error,
                        enter = fadeIn(), exit = fadeOut()
                    ) {
                        Text(
                            (sendState as? SendNotificationViewModel.SendState.Error)?.message ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Success
                    AnimatedVisibility(
                        visible = sendState is SendNotificationViewModel.SendState.Success,
                        enter = fadeIn(), exit = fadeOut()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                            Text("Notification sent!", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Send button
                    Button(
                        onClick = {
                            viewModel.sendNotification(
                                title = title.trim(),
                                message = message.trim(),
                                senderId = senderId,
                                senderRole = "admin",
                                recipientRole = selectedRole
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = sendState !is SendNotificationViewModel.SendState.Loading,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        if (sendState is SendNotificationViewModel.SendState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Send to ${selectedRole.label}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Recent notifications history
        if (recentNotifications.isNotEmpty()) {
            item {
                Text(
                    "Sent History",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            items(recentNotifications) { notification ->
                SentNotificationCard(notification)
            }
        }
    }
}

@Composable
private fun SentNotificationCard(notification: PushNotification) {
    val roleColor = when (notification.recipientRole) {
        "customer" -> Color(0xFF1565C0)
        "retailer" -> Color(0xFF2E7D32)
        "delivery_partner" -> Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(roleColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = roleColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        notification.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = roleColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            RecipientRole.entries.find { it.value == notification.recipientRole }?.label
                                ?: notification.recipientRole,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = roleColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Text(
                    notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                if (notification.createdAt.isNotBlank()) {
                    Text(
                        formatTimestamp(notification.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(iso: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        val date = parser.parse(iso.take(19)) ?: return iso
        SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(date)
    } catch (_: Exception) {
        iso
    }
}
