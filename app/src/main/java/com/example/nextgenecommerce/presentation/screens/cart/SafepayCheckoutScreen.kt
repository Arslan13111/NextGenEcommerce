package com.example.nextgenecommerce.presentation.screens.cart

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nextgenecommerce.presentation.components.PrimaryButton
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.CartViewModel
import com.example.nextgenecommerce.presentation.viewmodel.SafepayViewModel
import com.example.nextgenecommerce.util.Resource

// Sandbox checkout UI: https://sandbox.payments.getsafepay.com
// Production checkout UI: https://payments.getsafepay.com
// (API calls go to sandbox.api.getsafepay.com — that's different from the checkout page)
private const val SAFEPAY_ENV = "sandbox"
private const val SAFEPAY_CHECKOUT_BASE = "https://sandbox.payments.getsafepay.com"

private const val SUCCESS_SCHEME = "nextgenecommerce://payment/success"
private const val CANCEL_SCHEME  = "nextgenecommerce://payment/cancel"

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

    // Kick off session creation once
    LaunchedEffect(Unit) {
        safepayViewModel.createSession(
            amount = amount.toDoubleOrNull() ?: 0.0,
            orderId = orderId
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Safepay",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = sessionState) {
                null, is Resource.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            "Preparing secure checkout…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is Resource.Success -> {
                    val tracker = state.data ?: ""
                    val checkoutUrl = buildCheckoutUrl(tracker, orderId)

                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val url = request?.url?.toString() ?: return false
                                        return when {
                                            url.startsWith(SUCCESS_SCHEME) -> {
                                                safepayViewModel.resetState()
                                                cartViewModel.clearCart()
                                                navController.navigate("order_success/$orderId") {
                                                    popUpTo(Screen.Cart.route) { inclusive = true }
                                                }
                                                true
                                            }
                                            url.startsWith(CANCEL_SCHEME) -> {
                                                navController.popBackStack()
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

                is Resource.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "Could not start Safepay checkout",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            state.message ?: "Unknown error",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PrimaryButton(
                            text = "Try Again",
                            onClick = {
                                safepayViewModel.createSession(
                                    amount = amount.toDoubleOrNull() ?: 0.0,
                                    orderId = orderId
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun buildCheckoutUrl(tracker: String, orderId: String): String {
    val successUrl = Uri.encode(SUCCESS_SCHEME)
    val cancelUrl  = Uri.encode(CANCEL_SCHEME)
    return "$SAFEPAY_CHECKOUT_BASE/checkout" +
        "?env=$SAFEPAY_ENV" +
        "&beacon=$tracker" +
        "&source=mobile" +
        "&order_id=$orderId" +
        "&redirect_url=$successUrl" +
        "&cancel_url=$cancelUrl"
}
