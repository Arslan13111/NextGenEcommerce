package com.example.nextgenecommerce.presentation.screens.product

import android.content.Intent
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
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.example.nextgenecommerce.data.models.CartItem
import com.example.nextgenecommerce.presentation.components.ImageCarousel
import com.example.nextgenecommerce.presentation.viewmodel.AuthViewModel
import com.example.nextgenecommerce.presentation.viewmodel.CartViewModel
import com.example.nextgenecommerce.presentation.viewmodel.ProductViewModel
import com.example.nextgenecommerce.presentation.screens.tryon.LiveARTryOnActivity
import com.example.nextgenecommerce.presentation.viewmodel.WishlistViewModel

// ── Color name → Color value ───────────────────────────────────────────────────
private fun colorNameToColor(name: String): Color = when (name.lowercase().trim()) {
    "red", "coral red"              -> Color(0xFFE53935)
    "blue", "navy", "navy blue"     -> Color(0xFF1565C0)
    "green"                         -> Color(0xFF2E7D32)
    "black"                         -> Color(0xFF111111)
    "white"                         -> Color(0xFFEEEEEE)
    "yellow", "lemon yellow"        -> Color(0xFFFDD835)
    "pink"                          -> Color(0xFFEC407A)
    "purple"                        -> Color(0xFF7B1FA2)
    "orange"                        -> Color(0xFFFB8C00)
    "gray", "grey"                  -> Color(0xFF757575)
    "brown"                         -> Color(0xFF5D4037)
    "beige"                         -> Color(0xFFD7CCC8)
    "lilac"                         -> Color(0xFFCE93D8)
    "sand"                          -> Color(0xFFC2B280)
    else                            -> Color(0xFFBDBDBD)
}

private fun Color.luminance() = 0.299f * red + 0.587f * green + 0.114f * blue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    navController: NavController,
    productId: String,
    productViewModel: ProductViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel(),
    wishlistViewModel: WishlistViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val product by productViewModel.selectedProduct.collectAsStateWithLifecycle()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val wishlistItems by wishlistViewModel.wishlistItems.collectAsStateWithLifecycle()

    var selectedSize by remember { mutableStateOf<String?>(null) }
    var selectedColor by remember { mutableStateOf<String?>(null) }
    var quantity by remember { mutableIntStateOf(1) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showLensLoginDialog by remember { mutableStateOf(false) }
    var detailsExpanded by remember { mutableStateOf(false) }
    var currentCarouselPage by remember { mutableIntStateOf(0) }
    var carouselTargetPage by remember { mutableIntStateOf(0) }

    // Color selected → scroll carousel to the exact matching image
    LaunchedEffect(selectedColor) {
        val prod = product ?: return@LaunchedEffect
        val color = selectedColor ?: return@LaunchedEffect
        val targetIdx = resolveImageIndexForColor(color, prod.colorImages, prod.colors, prod.images)
        if (targetIdx >= 0) carouselTargetPage = targetIdx
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(productId) {
        productViewModel.getProductById(productId)
    }

    // ── Loading state (prevents "appears twice" flash) ─────────────────────────
    if (product == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    // ── Dialogs ────────────────────────────────────────────────────────────────
    if (showLoginDialog) {
        AlertDialog(
            onDismissRequest = { showLoginDialog = false },
            title = { Text("Login Required") },
            text = { Text("Please login to use the Try On feature.") },
            confirmButton = {
                TextButton(onClick = { showLoginDialog = false; navController.navigate("login") }) {
                    Text("Login")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoginDialog = false }) { Text("Cancel") }
            }
        )
    }
    if (showLensLoginDialog) {
        AlertDialog(
            onDismissRequest = { showLensLoginDialog = false },
            title = { Text("Login Required") },
            text = { Text("Please login to use the Live Try On with AR feature.") },
            confirmButton = {
                TextButton(onClick = { showLensLoginDialog = false; navController.navigate("login") }) {
                    Text("Login")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLensLoginDialog = false }) { Text("Cancel") }
            }
        )
    }

    val prod = product!!
    val isFavorite = wishlistItems.any { it.productId == prod.id }

        fun addToCartAndValidate(onSuccess: () -> Unit) {
            if (selectedSize == null || selectedColor == null) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = when {
                            selectedSize == null && selectedColor == null -> "Please select size and color"
                            selectedSize == null -> "Please select a size"
                            else -> "Please select a color"
                        },
                        duration = SnackbarDuration.Short
                    )
                }
            } else {
                val colorIdx = prod.colors.indexOf(selectedColor ?: "").coerceAtLeast(0)
                val colorImage = if (colorIdx < prod.images.size) prod.images[colorIdx]
                                 else prod.images.firstOrNull() ?: ""
                val cartItem = CartItem(
                    productId = prod.id,
                    productName = prod.name,
                    productImage = colorImage,
                    price = prod.price,
                    originalPrice = prod.originalPrice,
                    quantity = quantity,
                    selectedSize = selectedSize!!,
                    selectedColor = selectedColor!!,
                    storeName = prod.brand.ifEmpty { "Default Store" }
                )
                cartViewModel.addToCart(cartItem)
                onSuccess()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                // "< Back" on left, heart + share on right — NO title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "< Back" row
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
                            Icons.Default.ArrowBack,
                            "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Back",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Heart
                    IconButton(onClick = { wishlistViewModel.toggleWishlistItem(prod) }) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            "Wishlist",
                            tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onBackground
                        )
                    }
                    // Share
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Outlined.Share,
                            "Share",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            },
            bottomBar = {
                // ── BLACK bottom bar: [cart icon] [Buy now] ──────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Round cart icon button
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222222))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                addToCartAndValidate {
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "${prod.name} added to cart",
                                            actionLabel = "View Cart",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            navController.navigate("cart")
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // "Buy now" white button
                    Button(
                        onClick = {
                            addToCartAndValidate {
                                navController.navigate("checkout")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text(
                            "Buy now",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Product image carousel ─────────────────────────────────
                Box(modifier = Modifier.fillMaxWidth()) {
                    ImageCarousel(
                        images = prod.images,
                        contentDescription = prod.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        scrollToPage = carouselTargetPage,
                        onPageChanged = { page ->
                            currentCarouselPage = page
                            // Swipe → reverse-lookup which color owns this image
                            val imageUrl = prod.images.getOrNull(page)
                            if (imageUrl != null && prod.colorImages.isNotEmpty()) {
                                // Explicit map: find the color that points to this URL
                                prod.colorImages.entries.find { it.value == imageUrl }
                                    ?.key?.let { selectedColor = it }
                            } else {
                                // Legacy positional fallback
                                prod.colors.getOrNull(page)?.let { selectedColor = it }
                            }
                        }
                    )

                    // "New" badge on image
                    if (prod.isNew || prod.originalPrice > prod.price) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (prod.isNew) "New"
                                else "-${((1 - prod.price / prod.originalPrice) * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                // ── Product info ───────────────────────────────────────────
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {

                    // Name (left) + prices (right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Product name + brand (left)
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = prod.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                lineHeight = 26.sp
                            )
                            if (prod.brand.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = prod.brand,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        // Prices (right)
                        Column(horizontalAlignment = Alignment.End) {
                            if (prod.originalPrice > prod.price) {
                                Text(
                                    text = "₴ ${prod.originalPrice.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textDecoration = TextDecoration.LineThrough
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                            Text(
                                text = "₴ ${prod.price.toInt()}",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (prod.originalPrice > prod.price) Color(0xFFE53935)
                                else MaterialTheme.colorScheme.onBackground,
                                fontSize = 22.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // ── Color dots ─────────────────────────────────────────
                    if (prod.colors.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Color",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                            if (selectedColor != null) {
                                Text(
                                    text = selectedColor!!,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            prod.colors.forEach { colorName ->
                                val isSelected = selectedColor == colorName
                                val dotColor = colorNameToColor(colorName)
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 32.dp else 26.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                        .then(
                                            if (isSelected)
                                                Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                            else
                                                Modifier.border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                                        )
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { selectedColor = colorName },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            null,
                                            tint = if (dotColor.luminance() > 0.55f) Color.Black else Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                    }

                    // ── AR Try-On (if available) ───────────────────────────
                    OutlinedButton(
                        onClick = {
                            if (currentUser == null) showLoginDialog = true
                            else navController.navigate("tryon/${prod.id}/$currentCarouselPage")
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Try On with AR", fontWeight = FontWeight.SemiBold)
                    }

                    if (!prod.lensId.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (currentUser == null) showLensLoginDialog = true
                                else {
                                    val intent = Intent(context, LiveARTryOnActivity::class.java).apply {
                                        putExtra(LiveARTryOnActivity.EXTRA_LENS_ID, prod.lensId)
                                        putExtra(LiveARTryOnActivity.EXTRA_PRODUCT_NAME, prod.name)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Icon(Icons.Default.Videocam, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Live Try On with AR", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Available sizes ───────────────────────────────────
                    if (prod.sizes.isNotEmpty()) {
                        Text(
                            "Available sizes",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            prod.sizes.forEach { size ->
                                val isSelected = selectedSize == size
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.onBackground
                                            else Color.Transparent
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.onBackground
                                            else MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { selectedSize = size }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        size,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.background
                                        else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // ── Details (expandable) ──────────────────────────────
                    DetailRow(
                        title = "Details",
                        isExpanded = detailsExpanded,
                        onToggle = { detailsExpanded = !detailsExpanded }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (prod.description.isNotBlank()) {
                                Text(
                                    prod.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 21.sp
                                )
                            }
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // ── Availability in stores ────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {}
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Availability in stores",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // ── Dimensional grid ──────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {}
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Dimensional grid",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // ── Payment and delivery ──────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {}
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Payment and delivery",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // ── Refund ────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {}
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Refund",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
}

/**
 * Resolves the carousel image index for a selected color.
 *
 * Priority:
 * 1. Explicit colorImages map (new products) — looks up the image URL then finds its index.
 * 2. Positional fallback (legacy products where images.size == colors.size).
 * 3. Returns -1 (no scroll) if neither strategy yields a valid index.
 */
private fun resolveImageIndexForColor(
    color: String,
    colorImages: Map<String, String>,
    colors: List<String>,
    images: List<String>
): Int {
    // Strategy 1: explicit map
    if (colorImages.isNotEmpty()) {
        val imageUrl = colorImages[color]
        if (imageUrl != null) {
            val idx = images.indexOf(imageUrl)
            if (idx >= 0) return idx
        }
        return -1
    }
    // Strategy 2: positional — only when list sizes match to avoid wrong scrolling
    if (colors.size == images.size) {
        val idx = colors.indexOf(color)
        if (idx >= 0) return idx
    }
    return -1
}

@Composable
private fun DetailRow(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onToggle() }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isExpanded) {
            Box(modifier = Modifier.padding(bottom = 14.dp)) {
                content()
            }
        }
    }
}
