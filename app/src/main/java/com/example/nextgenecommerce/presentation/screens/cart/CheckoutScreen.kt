package com.example.nextgenecommerce.presentation.screens.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nextgenecommerce.presentation.components.PaymentMethodCard
import com.example.nextgenecommerce.presentation.components.PrimaryButton
import com.example.nextgenecommerce.presentation.viewmodel.CartViewModel

enum class PaymentMethod {
    CREDIT_CARD,
    PAYPAL,
    GOOGLE_PAY,
    CASH_ON_DELIVERY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    navController: NavController,
    viewModel: CartViewModel = hiltViewModel()
) {
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CREDIT_CARD) }
    var cardNumber by remember { mutableStateOf("") }
    var cardHolderName by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val cartTotal by viewModel.cartTotal.collectAsState()
    val grandTotal = viewModel.getGrandTotal()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Payment",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
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
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Payment Methods Section
                Text(
                    "Select Payment Method",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Credit Card
                PaymentMethodCard(
                    title = "Credit Card",
                    subtitle = "Pay with your credit/debit card",
                    icon = Icons.Default.CreditCard,
                    isSelected = selectedPaymentMethod == PaymentMethod.CREDIT_CARD,
                    onClick = { selectedPaymentMethod = PaymentMethod.CREDIT_CARD }
                )

                // PayPal
                PaymentMethodCard(
                    title = "PayPal",
                    subtitle = "Pay using your PayPal account",
                    icon = Icons.Default.AccountBalanceWallet,
                    isSelected = selectedPaymentMethod == PaymentMethod.PAYPAL,
                    onClick = { selectedPaymentMethod = PaymentMethod.PAYPAL }
                )

                // Google Pay
                PaymentMethodCard(
                    title = "Google Pay",
                    subtitle = "Pay with Google Pay",
                    icon = Icons.Default.Wallet,
                    isSelected = selectedPaymentMethod == PaymentMethod.GOOGLE_PAY,
                    onClick = { selectedPaymentMethod = PaymentMethod.GOOGLE_PAY }
                )

                // Cash on Delivery
                PaymentMethodCard(
                    title = "Cash on Delivery",
                    subtitle = "Pay when you receive the order",
                    icon = Icons.Default.LocalShipping,
                    isSelected = selectedPaymentMethod == PaymentMethod.CASH_ON_DELIVERY,
                    onClick = { selectedPaymentMethod = PaymentMethod.CASH_ON_DELIVERY }
                )

                // Credit Card Form (only shown if credit card is selected)
                if (selectedPaymentMethod == PaymentMethod.CREDIT_CARD) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = MaterialTheme.shapes.medium,
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Card Information",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Card Number
                            OutlinedTextField(
                                value = cardNumber,
                                onValueChange = { if (it.length <= 19) cardNumber = it },
                                label = { Text("Card Number") },
                                placeholder = { Text("1234 5678 9012 3456") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )

                            // Card Holder Name
                            OutlinedTextField(
                                value = cardHolderName,
                                onValueChange = { cardHolderName = it },
                                label = { Text("Card Holder Name") },
                                placeholder = { Text("John Doe") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Expiry Date
                                OutlinedTextField(
                                    value = expiryDate,
                                    onValueChange = { if (it.length <= 5) expiryDate = it },
                                    label = { Text("Expiry") },
                                    placeholder = { Text("MM/YY") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )

                                // CVV
                                OutlinedTextField(
                                    value = cvv,
                                    onValueChange = { if (it.length <= 3) cvv = it },
                                    label = { Text("CVV") },
                                    placeholder = { Text("123") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }

                // Order Summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Order Summary",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                            Text("$${"%.2f".format(cartTotal)}", style = MaterialTheme.typography.bodyMedium)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                            Text(
                                "$${"%.2f".format(grandTotal)}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // Bottom Button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    PrimaryButton(
                        text = if (selectedPaymentMethod == PaymentMethod.CASH_ON_DELIVERY)
                            "Place Order"
                        else
                            "Pay $${"%.2f".format(grandTotal)}",
                        onClick = {
                            isProcessing = true
                            // TODO: Process payment
                            // For now just navigate to success screen
                            navController.navigate("order_success") {
                                popUpTo("home") { inclusive = false }
                            }
                        },
                        loading = isProcessing
                    )
                }
            }
        }
    }
}
