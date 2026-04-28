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
import com.example.nextgenecommerce.data.models.SupabaseReview
import com.example.nextgenecommerce.presentation.components.ImageCarousel
import com.example.nextgenecommerce.presentation.viewmodel.AuthViewModel
import com.example.nextgenecommerce.presentation.viewmodel.CartViewModel
import com.example.nextgenecommerce.presentation.viewmodel.ProductViewModel
import com.example.nextgenecommerce.presentation.viewmodel.ReviewViewModel
import com.example.nextgenecommerce.presentation.screens.tryon.LiveARTryOnActivity
import com.example.nextgenecommerce.presentation.viewmodel.WishlistViewModel
import com.example.nextgenecommerce.util.ColorUtils
import com.example.nextgenecommerce.util.Resource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun Color.luminance() = 0.299f * red + 0.587f * green + 0.114f * blue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    navController: NavController,
    productId: String,
    productViewModel: ProductViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel(),
    wishlistViewModel: WishlistViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    reviewViewModel: ReviewViewModel = hiltViewModel()
) {
    val product by productViewModel.selectedProduct.collectAsStateWithLifecycle()
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val wishlistItems by wishlistViewModel.wishlistItems.collectAsStateWithLifecycle()
    val reviews by reviewViewModel.reviews.collectAsStateWithLifecycle()
    val reviewsLoading by reviewViewModel.reviewsLoading.collectAsStateWithLifecycle()
    val submitState by reviewViewModel.submitState.collectAsStateWithLifecycle()
    val hasReviewed by reviewViewModel.hasReviewed.collectAsStateWithLifecycle()

    var selectedSize by remember { mutableStateOf<String?>(null) }
    var selectedColor by remember { mutableStateOf<String?>(null) }
    var quantity by remember { mutableIntStateOf(1) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showLensLoginDialog by remember { mutableStateOf(false) }
    var detailsExpanded by remember { mutableStateOf(false) }
    var ratingExpanded by remember { mutableStateOf(false) }
    var currentCarouselPage by remember { mutableIntStateOf(0) }
    var carouselTargetPage by remember { mutableIntStateOf(0) }

    // Write-review form state
    var reviewRating by remember { mutableIntStateOf(0) }
    var reviewComment by remember { mutableStateOf("") }

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
        reviewViewModel.loadReviews(productId)
        reviewViewModel.checkHasReviewed(productId)
    }

    LaunchedEffect(submitState) {
        when (submitState) {
            is Resource.Success -> {
                reviewRating = 0
                reviewComment = ""
                snackbarHostState.showSnackbar("Review submitted!")
                reviewViewModel.resetSubmitState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(
                    (submitState as Resource.Error).message ?: "Failed to submit review"
                )
                reviewViewModel.resetSubmitState()
            }
            else -> {}
        }
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
    val isFavorite by remember(wishlistItems, prod.id) {
        derivedStateOf { wishlistItems.any { it.productId == prod.id } }
    }

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
                // Persistent container for bottom bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .navigationBarsPadding()
                ) {
                    // ── BLACK bottom bar: [cart icon] [Buy now] ──────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "PKR ${prod.price.toInt()}",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (prod.originalPrice > prod.price) Color(0xFFE53935)
                                else MaterialTheme.colorScheme.onBackground,
                                fontSize = 22.sp
                            )
                            if (prod.originalPrice > prod.price) {
                                Text(
                                    text = "PKR ${prod.originalPrice.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textDecoration = TextDecoration.LineThrough
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
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
                                    text = ColorUtils.getColorDisplayName(selectedColor!!),
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
                                val dotColor = ColorUtils.parseColor(colorName)
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

                    // ── Rating & Reviews ──────────────────────────────────
                    DetailRow(
                        title = "Rating & Reviews",
                        isExpanded = ratingExpanded,
                        onToggle = { ratingExpanded = !ratingExpanded }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                            // ── Score summary ──────────────────────────────
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = if (prod.rating > 0) String.format("%.1f", prod.rating) else "—",
                                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        val full  = prod.rating.toInt()
                                        val half  = (prod.rating - full) >= 0.5f
                                        val empty = 5 - full - if (half) 1 else 0
                                        repeat(full)  { Icon(Icons.Default.Star,        null, tint = Color(0xFFFFC107), modifier = Modifier.size(20.dp)) }
                                        if (half)       Icon(Icons.Default.StarHalf,    null, tint = Color(0xFFFFC107), modifier = Modifier.size(20.dp))
                                        repeat(empty) { Icon(Icons.Default.StarOutline, null, tint = Color(0xFFFFC107), modifier = Modifier.size(20.dp)) }
                                    }
                                    Text(
                                        text = if (prod.reviewCount > 0) "${prod.reviewCount} reviews" else "No reviews yet",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                            // ── Write a review ─────────────────────────────
                            if (currentUser == null) {
                                // Not logged in
                                OutlinedButton(
                                    onClick = { navController.navigate("login") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Login to write a review")
                                }
                            } else if (hasReviewed) {
                                // Already reviewed
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(12.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                                    Text("You've already reviewed this product", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            } else {
                                // Write review form
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Write a Review", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))

                                    // Tappable star selector
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        (1..5).forEach { star ->
                                            Icon(
                                                imageVector = if (star <= reviewRating) Icons.Default.Star else Icons.Default.StarOutline,
                                                contentDescription = "$star stars",
                                                tint = if (star <= reviewRating) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clickable(
                                                        indication = null,
                                                        interactionSource = remember { MutableInteractionSource() }
                                                    ) { reviewRating = star }
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = reviewComment,
                                        onValueChange = { reviewComment = it },
                                        placeholder = { Text("Share your thoughts about this product…") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 3,
                                        maxLines = 5,
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    val isSubmitting = submitState is Resource.Loading
                                    Button(
                                        onClick = {
                                            reviewViewModel.submitReview(
                                                productId = prod.id,
                                                userId    = currentUser!!.id,
                                                userName  = currentUser!!.name.ifBlank { currentUser!!.email.ifBlank { "User" } },
                                                rating    = reviewRating,
                                                comment   = reviewComment
                                            )
                                        },
                                        enabled = !isSubmitting && reviewRating > 0,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        if (isSubmitting) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                            Spacer(Modifier.width(8.dp))
                                        }
                                        Text(if (isSubmitting) "Submitting…" else "Submit Review", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            // ── Existing reviews list ──────────────────────
                            if (reviewsLoading) {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            } else if (reviews.isEmpty()) {
                                Text(
                                    "No reviews yet. Be the first!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    reviews.take(10).forEach { review ->
                                        ReviewCard(review)
                                    }
                                    if (reviews.size > 10) {
                                        Text(
                                            "+ ${reviews.size - 10} more reviews",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
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
private fun ReviewCard(review: SupabaseReview) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reviewer name + stars
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = review.userName.ifBlank { "Anonymous" },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(5) { i ->
                        Icon(
                            imageVector = if (i < review.rating) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            // Date
            Text(
                text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    .format(Date(review.createdAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (review.comment.isNotBlank()) {
            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
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
