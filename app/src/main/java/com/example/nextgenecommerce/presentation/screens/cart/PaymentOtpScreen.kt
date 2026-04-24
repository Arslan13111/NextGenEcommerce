package com.example.nextgenecommerce.presentation.screens.cart

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nextgenecommerce.presentation.components.PrimaryButton
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.CartViewModel
import com.example.nextgenecommerce.presentation.viewmodel.OrderViewModel
import com.example.nextgenecommerce.util.Resource

// ── UPDATE these with your actual EasyPaisa / JazzCash merchant numbers ──────
private const val EASYPAISA_NUMBER = "03180078338"
private const val JAZZCASH_NUMBER  = "03180078338"
private const val MERCHANT_NAME    = "Muhammad Arslan Munawar"
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentOtpScreen(
    navController: NavController,
    paymentMethod: String,
    orderId: String,
    amount: String,
    cartViewModel: CartViewModel = hiltViewModel(),
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isEasyPaisa = paymentMethod == "EASYPAISA"
    val brandColor  = if (isEasyPaisa) Color(0xFF1E8C2E) else Color(0xFFE4002B)
    val brandName   = if (isEasyPaisa) "EasyPaisa" else "JazzCash"
    val merchantNo  = if (isEasyPaisa) EASYPAISA_NUMBER else JAZZCASH_NUMBER

    var isSubmitting      by remember { mutableStateOf(false) }
    var copiedAmount      by remember { mutableStateOf(false) }
    var copiedNumber      by remember { mutableStateOf(false) }

    val createOrderState by orderViewModel.createOrderState.collectAsState()

    LaunchedEffect(createOrderState) {
        when (val state = createOrderState) {
            is Resource.Success -> {
                isSubmitting = false
                orderViewModel.resetCreateOrderState()
                cartViewModel.clearCart()
                navController.navigate("order_success/${state.data?.id ?: orderId}") {
                    popUpTo(Screen.Cart.route) { inclusive = true }
                }
            }
            is Resource.Error -> {
                isSubmitting = false
                orderViewModel.resetCreateOrderState()
            }
            is Resource.Loading -> isSubmitting = true
            else -> {}
        }
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "$brandName Payment",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Brand header card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = brandColor),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (isEasyPaisa) "EP" else "JC",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Rs. $amount",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    )
                    Text(
                        "Order #${orderId.take(8).uppercase()}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }
            }

            // Step-by-step instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "How to Pay",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    StepRow(step = "1", text = "Open your $brandName app")
                    StepRow(step = "2", text = "Go to Send Money")
                    StepRow(step = "3", text = "Send Rs. $amount to this number:")

                    // Merchant number with copy button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(brandColor.copy(alpha = 0.08f))
                            .border(1.dp, brandColor.copy(alpha = 0.3f), MaterialTheme.shapes.small)
                            .clickable {
                                copyToClipboard("Merchant Number", merchantNo)
                                copiedNumber = true
                                copiedAmount = false
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                MERCHANT_NAME,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                merchantNo,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = brandColor
                                )
                            )
                        }
                        Icon(
                            if (copiedNumber) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
                            null,
                            tint = if (copiedNumber) Color(0xFF4CAF50) else brandColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Amount with copy button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable {
                                copyToClipboard("Amount", amount)
                                copiedAmount = true
                                copiedNumber = false
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Amount",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Rs. $amount",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Icon(
                            if (copiedAmount) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
                            null,
                            tint = if (copiedAmount) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    StepRow(step = "4", text = "Once sent, tap \"I've Paid\" below")
                    StepRow(step = "5", text = "We'll confirm your order after verifying the payment")
                }
            }

            // Info note
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.small,
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Your order will be confirmed once we verify your payment in our $brandName app. This usually takes a few minutes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            PrimaryButton(
                text = if (isSubmitting) "Please wait..." else "I've Paid",
                loading = isSubmitting,
                enabled = !isSubmitting,
                onClick = {
                    orderViewModel.confirmMobilePayment(orderId)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StepRow(step: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                step,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
