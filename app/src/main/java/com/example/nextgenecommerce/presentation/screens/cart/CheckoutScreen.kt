package com.example.nextgenecommerce.presentation.screens.cart

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nextgenecommerce.data.models.Address
import com.example.nextgenecommerce.data.models.CartItem
import com.example.nextgenecommerce.data.models.OrderItem
import com.example.nextgenecommerce.data.models.PaymentMethod
import com.example.nextgenecommerce.data.remote.CreateOrderRequest

import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.AddressViewModel
import com.example.nextgenecommerce.presentation.viewmodel.CartViewModel
import com.example.nextgenecommerce.presentation.viewmodel.OrderViewModel
import com.example.nextgenecommerce.util.CardTierUtil
import com.example.nextgenecommerce.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    navController: NavController,
    cartViewModel: CartViewModel = hiltViewModel(),
    addressViewModel: AddressViewModel = hiltViewModel(),
    orderViewModel: OrderViewModel = hiltViewModel()
) {
    val cartItems        by cartViewModel.cartItems.collectAsState()
    val cartTotal        by cartViewModel.cartTotal.collectAsState()
    val addresses        by addressViewModel.addresses.collectAsState()
    val createOrderState by orderViewModel.createOrderState.collectAsState()
    val userTier         by orderViewModel.userTier.collectAsState()

    var selectedAddress   by remember { mutableStateOf<Address?>(null) }
    var showAddressSheet  by remember { mutableStateOf(false) }
    var comment           by remember { mutableStateOf("") }
    var orderError        by remember { mutableStateOf("") }
    var applyCardDiscount by remember { mutableStateOf(true) }
    var selectedDelivery  by remember { mutableStateOf(DeliveryType.STANDARD) }

    val subtotal = cartTotal
    val tax = subtotal * 0.08
    val shipping = selectedDelivery.charge.toDouble()
    val discountAmount = if (applyCardDiscount) CardTierUtil.computeDiscount(subtotal, userTier) else 0.0
    val grandTotal = (subtotal + tax + shipping - discountAmount).coerceAtLeast(0.0)
    val isProcessing = createOrderState is Resource.Loading

    LaunchedEffect(addresses) {
        if (selectedAddress == null) {
            selectedAddress = addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
        }
    }

    LaunchedEffect(createOrderState) {
        when (val state = createOrderState) {
            is Resource.Success -> {
                orderViewModel.resetCreateOrderState()
                val orderId = state.data?.id ?: ""
                cartViewModel.clearCart()
                navController.navigate("order_success/$orderId") {
                    popUpTo(Screen.Cart.route) { inclusive = true }
                }
            }
            is Resource.Error -> {
                orderError = state.message ?: "Failed to place order"
                orderViewModel.resetCreateOrderState()
            }
            else -> {}
        }
    }

    if (showAddressSheet) {
        AddressSelectionSheet(
            addresses = addresses,
            selectedAddress = selectedAddress,
            onAddressSelected = { selectedAddress = it; showAddressSheet = false },
            onAddNew = { showAddressSheet = false; navController.navigate(Screen.Addresses.route) },
            onDismiss = { showAddressSheet = false }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { navController.popBackStack() }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack, "Back",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Back", color = MaterialTheme.colorScheme.onBackground, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "Checkout",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(end = 48.dp)
                )
            }
        },
        bottomBar = {
            // ── Black bottom bar ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFF111111))
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Total breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        selectedDelivery.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        if (selectedDelivery.charge == 0) "Free" else "Rs ${selectedDelivery.charge}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (selectedDelivery.charge == 0) Color(0xFF80DEEA) else Color.White.copy(alpha = 0.85f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (discountAmount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${userTier.label} card discount",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF80DEEA)
                        )
                        Text(
                            "- Rs ${discountAmount.toInt()}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF80DEEA)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Total",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color.White
                        )
                        Text(
                            "${cartItems.size} items",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                    Text(
                        "Rs ${grandTotal.toInt()}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        fontSize = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (orderError.isNotEmpty()) {
                    Text(
                        orderError,
                        color = Color(0xFFEF5350),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Confirm order button
                Button(
                    onClick = {
                        orderError = ""
                        val address = selectedAddress ?: return@Button
                        orderViewModel.createOrder(
                            CreateOrderRequest(
                                userId = "",
                                items = cartItems.map {
                                    OrderItem(
                                        productId = it.productId,
                                        productName = it.productName,
                                        productImage = it.productImage,
                                        price = it.price,
                                        quantity = it.quantity,
                                        selectedSize = it.selectedSize,
                                        selectedColor = it.selectedColor
                                    )
                                },
                                shippingAddress = address,
                                paymentMethod = PaymentMethod.CASH_ON_DELIVERY.name,
                                discountAmount = discountAmount,
                                cardTier = userTier.name
                            )
                        )
                    },
                    enabled = !isProcessing && selectedAddress != null,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.3f)
                    )
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text("Confirm order", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Payment method ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Payment method",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Payments, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Cash on delivery", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // ── Discount card ──────────────────────────────────────────────────
            DiscountCardSection(
                tier = userTier,
                discountAmount = discountAmount,
                subtotal = subtotal,
                applyDiscount = applyCardDiscount,
                onToggle = { applyCardDiscount = it }
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // ── Delivery type ──────────────────────────────────────────────────
            DeliveryTypeSection(
                selected = selectedDelivery,
                onSelect = { selectedDelivery = it }
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // ── Delivery address ───────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Delivery address",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(
                        onClick = { showAddressSheet = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            if (selectedAddress == null) "Add" else "Change",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }

                if (selectedAddress != null) {
                    val addr = selectedAddress!!
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF111111)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LocationOn, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${addr.fullName}  ·  ${addr.label}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onBackground)
                            Text(addr.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${addr.addressLine1}, ${addr.city}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF111111).copy(alpha = 0.06f))
                            .border(1.5.dp, Color(0xFF111111).copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { showAddressSheet = true }
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF111111)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AddLocation, null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Add delivery address", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onBackground)
                                Text("Tap to select or add a new address", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // ── Your order ─────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Your order",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "${cartItems.size.toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            Icons.Default.Edit, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                cartItems.forEach { item ->
                    CheckoutItemRow(item = item)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // ── Comment ────────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Text(
                    "Comment",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    placeholder = {
                        Text(
                            "Use the doorbell!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    minLines = 2,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ── Delivery type ─────────────────────────────────────────────────────────────

enum class DeliveryType(
    val label: String,
    val description: String,
    val charge: Int,
    val chargeLabel: String
) {
    STANDARD("Standard Delivery", "3–5 business days", 150, "Rs 150"),
    EXPRESS("Express Delivery",  "1–2 business days", 350, "Rs 350"),
    CLICK_COLLECT("Click & Collect", "Ready in ~2 hours", 0, "Free")
}

@Composable
private fun DeliveryTypeSection(
    selected: DeliveryType,
    onSelect: (DeliveryType) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Delivery type",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        DeliveryType.values().forEach { type ->
            val isSelected = selected == type
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF111111) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                animationSpec = tween(200), label = "deliveryBg"
            )
            val icon = when (type) {
                DeliveryType.STANDARD     -> Icons.Default.LocalShipping
                DeliveryType.EXPRESS      -> Icons.Default.FlightTakeoff
                DeliveryType.CLICK_COLLECT -> Icons.Default.Store
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(bgColor)
                    .clickable { onSelect(type) }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) Color.White.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surface
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon, null,
                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        type.label,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        type.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    type.chargeLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) {
                        if (type.charge == 0) Color(0xFF80DEEA) else Color.White
                    } else {
                        if (type.charge == 0) Color(0xFF43A047) else MaterialTheme.colorScheme.onBackground
                    }
                )
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle, null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ── Discount card section ─────────────────────────────────────────────────────
@Composable
private fun DiscountCardSection(
    tier: CardTierUtil.CardTier,
    discountAmount: Double,
    subtotal: Double,
    applyDiscount: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val tierGradient = when (tier) {
        CardTierUtil.CardTier.SILVER -> listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
        CardTierUtil.CardTier.GOLD -> listOf(Color(0xFFE65100), Color(0xFFFFB74D))
        CardTierUtil.CardTier.PLATINUM -> listOf(Color(0xFF1B2631), Color(0xFF546E7A))
    }
    val switchTrack by animateColorAsState(
        targetValue = if (applyDiscount) Color(0xFF111111) else MaterialTheme.colorScheme.outline,
        animationSpec = tween(200), label = "switchTrack"
    )

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Discount Card",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mini card chip
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .width(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(tierGradient)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        tier.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp),
                        color = Color.White,
                        fontSize = 8.sp
                    )
                    Text(
                        "${tier.cashbackPercent.toInt()}% off",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${tier.label} card — ${tier.cashbackPercent.toInt()}% cashback",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    if (applyDiscount && discountAmount > 0)
                        "Saving Rs ${discountAmount.toInt()} on this order"
                    else
                        "Toggle to apply your card discount",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (applyDiscount && discountAmount > 0) Color(0xFF43A047)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = applyDiscount,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF111111),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = MaterialTheme.colorScheme.outline
                )
            )
        }
    }
}

// ── Checkout order item row ───────────────────────────────────────────────────
@Composable
private fun CheckoutItemRow(item: CartItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = item.productImage,
                contentDescription = item.productName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // New badge
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .align(Alignment.TopStart)
            ) {
                Text("New", fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.productName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                fontSize = 13.sp
            )
            if (item.storeName.isNotBlank() && item.storeName != "Default Store") {
                Text(
                    item.storeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Color", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(colorNameToCheckout(item.selectedColor))
                    )
                }
                // Size
                Text("Size  ${item.selectedSize}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Qty
                Text("Q-ty  ${item.quantity}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "₴ ${item.price.toInt()}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp
            )
        }
    }
}

private fun colorNameToCheckout(name: String): Color = when (name.lowercase().trim()) {
    "red", "coral red"           -> Color(0xFFEF5350)
    "blue", "navy", "navy blue"  -> Color(0xFF1565C0)
    "green"                      -> Color(0xFF43A047)
    "black"                      -> Color(0xFF111111)
    "white"                      -> Color(0xFFEEEEEE)
    "yellow", "lemon yellow"     -> Color(0xFFFDD835)
    "pink"                       -> Color(0xFFEC407A)
    "purple"                     -> Color(0xFF8E24AA)
    "orange"                     -> Color(0xFFFB8C00)
    "gray", "grey"               -> Color(0xFF757575)
    else                         -> Color(0xFFBDBDBD)
}

// ── Address selection sheet ───────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressSelectionSheet(
    addresses: List<Address>,
    selectedAddress: Address?,
    onAddressSelected: (Address) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Delivery Address", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(4.dp))

            if (addresses.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Text("No saved addresses", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                addresses.forEach { address ->
                    val isSelected = selectedAddress?.id == address.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) Color(0xFF111111)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .clickable { onAddressSelected(address) }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(if (isSelected) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                null,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${address.fullName} · ${address.label}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground)
                                if (address.isDefault) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(if (isSelected) Color.White.copy(alpha = 0.2f) else Color(0xFF111111)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                        Text("Default", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (isSelected) Color.White else Color.White, fontSize = 9.sp)
                                    }
                                }
                            }
                            Text(address.phone, style = MaterialTheme.typography.bodySmall, color = if (isSelected) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${address.addressLine1}, ${address.city}", style = MaterialTheme.typography.bodySmall, color = if (isSelected) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = onAddNew,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Address", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
