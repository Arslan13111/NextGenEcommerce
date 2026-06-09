package com.example.nextgenecommerce.presentation.screens.retailer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.nextgenecommerce.data.models.PushNotification
import com.example.nextgenecommerce.data.models.RecipientRole
import com.example.nextgenecommerce.presentation.viewmodel.SendNotificationViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RetailerNotifyTab(
    modifier: Modifier = Modifier,
    senderId: String,
    viewModel: SendNotificationViewModel,
) {
    val sendState by viewModel.sendState.collectAsState()
    val recentNotifications by viewModel.recentNotifications.collectAsState()

    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadRecentNotifications("retailer")
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
                    "Notify Customers",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                )
                Text(
                    "Send a notification to all your customers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

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
                    // Fixed recipient: customers only for retailers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Column {
                            Text(
                                "Sending to: Customers",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "All registered customers will receive this notification.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Notification Title") },
                        placeholder = { Text("e.g. Flash Sale Today!") },
                        leadingIcon = { Icon(Icons.Default.Title, null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

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

                    AnimatedVisibility(
                        visible = sendState is SendNotificationViewModel.SendState.Success,
                        enter = fadeIn(), exit = fadeOut()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                            Text("Notification sent to customers!", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.sendNotification(
                                title = title.trim(),
                                message = message.trim(),
                                senderId = senderId,
                                senderRole = "retailer",
                                recipientRole = RecipientRole.CUSTOMER
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
                            Text("Send Notification", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        if (recentNotifications.isNotEmpty()) {
            item {
                Text(
                    "Sent History",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            items(recentNotifications) { notification ->
                RetailerSentCard(notification)
            }
        }
    }
}

@Composable
private fun RetailerSentCard(notification: PushNotification) {
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
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    notification.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
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
