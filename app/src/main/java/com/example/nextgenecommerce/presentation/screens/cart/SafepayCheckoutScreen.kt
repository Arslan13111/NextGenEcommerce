package com.example.nextgenecommerce.presentation.screens.cart

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.CartViewModel
import com.example.nextgenecommerce.presentation.viewmodel.SafepayViewModel
import com.example.nextgenecommerce.util.Resource

// ─────────────────────────────────────────────────────────────────────────────
// Safepay Mobile WebView Integration
// Source-verified from: github.com/getsafepay/safepay-react-native-sdk
//
// ✅ Correct checkout URL (from official SDK):
//    Sandbox:    https://sandbox.api.getsafepay.com/checkout/pay?beacon=...&order_id=...&source=mobile&env=sandbox
//    Production: https://getsafepay.com/checkout/pay?beacon=...&order_id=...&source=mobile&env=production
//
// Completion detection:
//   action=complete   → payment successful
//   action=cancelled  → user cancelled
// ─────────────────────────────────────────────────────────────────────────────
private const val SUCCESS_SCHEME = "nextgenecommerce://payment/success"
private const val CANCEL_SCHEME  = "nextgenecommerce://payment/cancel"

// Safepay brand colors
private val SafepayBlue  = Color(0xFF1A4DB5)
private val SafepayLight = Color(0xFF2563EB)
private val SafepayGreen = Color(0xFF00C853)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SafepayCheckoutScreen(
    navController: NavController,
    orderId: String,
    amount: String,
    cartViewModel: CartViewModel = hiltViewModel(),
    safepayViewModel: SafepayViewModel = hiltViewModel()
) {
    val sessionState by safepayViewModel.sessionState.collectAsState()
    val verificationState by safepayViewModel.verificationState.collectAsState()
    var webViewProgress by remember { mutableIntStateOf(0) }
    var webViewLoading  by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        safepayViewModel.createSession(
            amount = amount.toDoubleOrNull() ?: 0.0,
            orderId = orderId
        )
    }

    LaunchedEffect(verificationState) {
        if (verificationState is Resource.Success) {
            safepayViewModel.resetState()
            cartViewModel.clearCart()
            navController.navigate("order_success/$orderId") {
                popUpTo(Screen.Cart.route) { inclusive = true }
            }
        }
    }

    if (verificationState is Resource.Loading) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Verifying payment") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Waiting for Safepay confirmation...")
                }
            }
        )
    }

    if (verificationState is Resource.Error) {
        val message = (verificationState as Resource.Error).message
        AlertDialog(
            onDismissRequest = { safepayViewModel.resetState() },
            title = { Text("Payment not verified") },
            text = { Text(message ?: "Safepay has not confirmed this payment yet.") },
            confirmButton = {
                TextButton(onClick = { safepayViewModel.verifyPayment(orderId) }) {
                    Text("Retry")
                }
            },
            dismissButton = {
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            // ── Premium branded top bar ───────────────────────────────────────
            Column {
                Surface(
                    color     = SafepayBlue,
                    tonalElevation = 0.dp,
                    modifier  = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back button
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                        }
                        Spacer(Modifier.width(4.dp))

                        // Lock + Safepay branding
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Lock, null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Safepay",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = Color.White
                            )
                        }

                        // Amount badge
                        if (amount.isNotBlank()) {
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    "PKR ${amount.toDoubleOrNull()?.toInt() ?: amount}",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        } else {
                            // Placeholder spacer to keep title centered
                            Spacer(Modifier.width(48.dp))
                        }
                    }
                }

                // ── WebView loading progress bar ──────────────────────────────
                AnimatedVisibility(
                    visible = webViewLoading && sessionState is Resource.Success,
                    enter   = fadeIn(),
                    exit    = fadeOut()
                ) {
                    LinearProgressIndicator(
                        progress   = webViewProgress / 100f,
                        modifier   = Modifier.fillMaxWidth().height(3.dp),
                        color      = SafepayGreen,
                        trackColor = SafepayBlue.copy(alpha = 0.3f)
                    )
                }

                // ── Secure connection banner ──────────────────────────────────
                AnimatedVisibility(visible = sessionState is Resource.Success) {
                    Surface(
                        color    = Color(0xFF0F3A8A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Security, null, tint = SafepayGreen, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "256-bit SSL encrypted · Powered by Safepay",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = sessionState) {

                // ── Session loading state ─────────────────────────────────────
                null, is Resource.Loading -> {
                    SafepayLoadingContent(amount)
                }

                // ── WebView ───────────────────────────────────────────────────
                is Resource.Success -> {
                    val checkoutUrl = state.data ?: ""

                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        webViewLoading = false
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val url = request?.url?.toString() ?: return false
                                        return when {
                                            // Deep-link fallback
                                            url.startsWith(SUCCESS_SCHEME) -> {
                                                safepayViewModel.verifyPayment(orderId)
                                                true
                                            }
                                            url.startsWith(CANCEL_SCHEME) -> {
                                                navController.popBackStack()
                                                true
                                            }
                                            // Safepay standard callbacks
                                            url.contains("action=cancelled") -> {
                                                navController.popBackStack()
                                                true
                                            }
                                            url.contains("action=complete") -> {
                                                safepayViewModel.verifyPayment(orderId)
                                                true
                                            }
                                            else -> false
                                        }
                                    }
                                }
                                loadUrl(checkoutUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // ── Error state ───────────────────────────────────────────────
                is Resource.Error -> {
                    SafepayErrorContent(
                        message = state.message,
                        onRetry = {
                            safepayViewModel.createSession(
                                amount  = amount.toDoubleOrNull() ?: 0.0,
                                orderId = orderId
                            )
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

// ── Premium loading screen ────────────────────────────────────────────────────

@Composable
private fun SafepayLoadingContent(amount: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        SafepayBlue.copy(alpha = 0.06f),
                        Color.Transparent
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            // Animated lock icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(SafepayBlue, SafepayLight))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lock, null,
                    tint     = Color.White.copy(alpha = pulse),
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                "Preparing Secure Payment",
                style      = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onBackground
            )

            if (amount.isNotBlank()) {
                Surface(
                    color = SafepayBlue.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CreditCard, null, tint = SafepayBlue, modifier = Modifier.size(18.dp))
                        Text(
                            "PKR ${amount.toDoubleOrNull()?.toInt() ?: amount}",
                            style      = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color      = SafepayBlue
                        )
                    }
                }
            }

            LinearProgressIndicator(
                modifier   = Modifier.fillMaxWidth(0.5f).clip(RoundedCornerShape(4.dp)),
                color      = SafepayBlue,
                trackColor = SafepayBlue.copy(alpha = 0.15f)
            )

            Text(
                "Connecting to Safepay…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Trust badges
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                TrustBadge(Icons.Default.Security,   "SSL Secured")
                TrustBadge(Icons.Default.VerifiedUser, "PCI Compliant")
                TrustBadge(Icons.Default.Shield,     "Encrypted")
            }
        }
    }
}

@Composable
private fun TrustBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, null, tint = SafepayGreen, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Error screen ──────────────────────────────────────────────────────────────

@Composable
private fun SafepayErrorContent(
    message: String?,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ErrorOutline, null,
                    tint     = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                "Payment Setup Failed",
                style     = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    message ?: "Could not connect to Safepay. Please check your internet connection and try again.",
                    modifier  = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style     = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick  = onRetry,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = SafepayBlue)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Try Again", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick  = onCancel,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(14.dp)
            ) {
                Text("Cancel Payment")
            }
        }
    }
}

// ── URL builder ───────────────────────────────────────────────────────────────
