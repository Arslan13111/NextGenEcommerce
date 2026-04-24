package com.example.nextgenecommerce.presentation.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nextgenecommerce.data.models.ProductEntity
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.CartViewModel
import com.example.nextgenecommerce.presentation.viewmodel.NotificationViewModel
import com.example.nextgenecommerce.presentation.viewmodel.ProductViewModel
import com.example.nextgenecommerce.presentation.viewmodel.WishlistViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    productViewModel: ProductViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel(),
    wishlistViewModel: WishlistViewModel = hiltViewModel(),
    notificationViewModel: NotificationViewModel = hiltViewModel()
) {
    val allProducts by productViewModel.allProducts.collectAsStateWithLifecycle()
    val wishlistItems by wishlistViewModel.wishlistItems.collectAsStateWithLifecycle()
    val unreadNotificationCount by notificationViewModel.unreadCount.collectAsStateWithLifecycle()

    // Only show spinner on first load — never flash back to spinner during background sync
    var hasLoadedOnce by remember { mutableStateOf(false) }
    if (allProducts.isNotEmpty()) hasLoadedOnce = true

    val heroBanners = remember(allProducts) { allProducts.take(5) }
    val trendingProducts = remember(allProducts) { allProducts.take(4) }
    val newArrivals = remember(allProducts) { allProducts.take(6) }
    val youMightLike = remember(allProducts) { allProducts.drop(2).take(6) }
    val promotions = remember(allProducts) { allProducts.filter { it.originalPrice > it.price }.take(5) }
    val pagerState = rememberPagerState { heroBanners.size.coerceAtLeast(1) }

    // Auto-slide the hero carousel every 4 seconds
    LaunchedEffect(pagerState, heroBanners.size) {
        if (heroBanners.size > 1) {
            while (true) {
                kotlinx.coroutines.delay(4000L)
                val next = (pagerState.currentPage + 1) % heroBanners.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.navigate(Screen.AllProducts.route) { launchSingleTop = true } }) {
                        Icon(
                            Icons.Default.GridView,
                            contentDescription = "Catalog",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "U",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontStyle = FontStyle.Italic,
                                fontSize = 28.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (unreadNotificationCount > 0) {
                                Badge(containerColor = MaterialTheme.colorScheme.error) {
                                    Text(
                                        "$unreadNotificationCount",
                                        color = Color.White,
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    IconButton(onClick = { navController.navigate(Screen.AllProducts.route) { launchSingleTop = true } }) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        if (!hasLoadedOnce) {
            // Only show spinner on very first load — never again after data is received
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {

            // ── Hero Carousel ──────────────────────────────────────────
            item {
                if (heroBanners.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(420.dp)
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondBoundsPageCount = 1,
                            key = { it }
                        ) { page ->
                            HeroBannerCard(
                                product = heroBanners[page],
                                onClick = {
                                    navController.navigate(
                                        Screen.ProductDetail.createRoute(heroBanners[page].id)
                                    ) { launchSingleTop = true }
                                }
                            )
                        }
                    }

                    // Dots + "Trends" label centred below carousel
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            repeat(heroBanners.size) { idx ->
                                Box(
                                    modifier = Modifier
                                        .size(
                                            width = if (pagerState.currentPage == idx) 18.dp else 6.dp,
                                            height = 6.dp
                                        )
                                        .clip(CircleShape)
                                        .background(
                                            if (pagerState.currentPage == idx)
                                                MaterialTheme.colorScheme.onBackground
                                            else
                                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                                        )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Trends",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // ── Trending Products Grid ─────────────────────────────────
            item {
                if (trendingProducts.isNotEmpty()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            trendingProducts.chunked(2).forEach { rowPair ->
                                rowPair.forEach { product ->
                                    HomeProductCard(
                                        product = product,
                                        isFavorite = wishlistItems.any { it.productId == product.id },
                                        onClick = {
                                            navController.navigate(
                                                Screen.ProductDetail.createRoute(product.id)
                                            ) { launchSingleTop = true }
                                        },
                                        onFavoriteClick = {
                                            wishlistViewModel.toggleWishlistItem(product)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // ── New arrivals ───────────────────────────────────────────
            item {
                SectionHeader(
                    title = "New arrivals",
                    onShowAll = { navController.navigate(Screen.AllProducts.route) { launchSingleTop = true } }
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(newArrivals, key = { it.id }) { product ->
                        HorizontalProductCard(
                            product = product,
                            isFavorite = wishlistItems.any { it.productId == product.id },
                            onClick = {
                                navController.navigate(Screen.ProductDetail.createRoute(product.id)) { launchSingleTop = true }
                            },
                            onFavoriteClick = { wishlistViewModel.toggleWishlistItem(product) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── You might like ─────────────────────────────────────────
            item {
                SectionHeader(
                    title = "You might like",
                    onShowAll = { navController.navigate(Screen.AllProducts.route) { launchSingleTop = true } }
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(youMightLike, key = { it.id }) { product ->
                        HorizontalProductCard(
                            product = product,
                            isFavorite = wishlistItems.any { it.productId == product.id },
                            onClick = {
                                navController.navigate(Screen.ProductDetail.createRoute(product.id)) { launchSingleTop = true }
                            },
                            onFavoriteClick = { wishlistViewModel.toggleWishlistItem(product) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Promotions ─────────────────────────────────────────────
            if (promotions.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Promotions",
                        onShowAll = { navController.navigate(Screen.AllProducts.route) { launchSingleTop = true } }
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(promotions, key = { it.id }) { product ->
                            PromotionCard(
                                product = product,
                                onClick = {
                                    navController.navigate(
                                        Screen.ProductDetail.createRoute(product.id)
                                    ) { launchSingleTop = true }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // ── Brands ─────────────────────────────────────────────────
            item {
                val brands = allProducts.mapNotNull { it.brand.ifBlank { null } }.distinct().take(6)
                if (brands.isNotEmpty()) {
                    SectionHeader(
                        title = "Brands",
                        onShowAll = { navController.navigate(Screen.AllProducts.route) { launchSingleTop = true } }
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(brands) { brand ->
                            val brandProduct = allProducts.firstOrNull { it.brand == brand }
                            BrandCard(
                                brandName = brand,
                                imageUrl = brandProduct?.images?.firstOrNull(),
                                onClick = {}
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// ── Hero Banner Card ──────────────────────────────────────────────────────────
@Composable
private fun HeroBannerCard(product: ProductEntity, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = product.images.firstOrNull(),
            contentDescription = product.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark gradient at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                    )
                )
        )

        // Top-right arrow icon
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.88f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CallMade,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color.Black
            )
        }

        // Bottom-left text
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            if (product.originalPrice > product.price) {
                val pct = ((1 - product.price / product.originalPrice) * 100).toInt()
                Text(
                    text = "-$pct%",
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFFFFF176),  // yellow
                    lineHeight = 40.sp
                )
                Text(
                    text = product.subCategory.ifBlank { "Summer collection" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = buildString {
                    append(product.name)
                    if (product.brand.isNotBlank()) {
                        append("\nwith ${product.brand}")
                    }
                },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 3,
                lineHeight = 28.sp
            )
        }
    }
}

// ── Home Grid Product Card (for Trends section) ───────────────────────────────
@Composable
private fun HomeProductCard(
    product: ProductEntity,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = product.images.firstOrNull(),
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // New badge
            if (product.isNew || product.originalPrice > product.price) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (product.isNew) "New"
                        else "-${((1 - product.price / product.originalPrice) * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                        fontSize = 10.sp
                    )
                }
            }
            // Heart button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.88f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onFavoriteClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Color.Red else Color(0xFF888888),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = product.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp,
            lineHeight = 17.sp
        )
        if (product.brand.isNotBlank()) {
            Text(
                text = product.brand,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (product.originalPrice > product.price) {
                Text(
                    text = "₴ ${product.originalPrice.toInt()}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = TextDecoration.LineThrough
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            Text(
                text = "₴ ${product.price.toInt()}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 13.sp
            )
        }
    }
}

// ── Horizontal Scroll Product Card (New arrivals / You might like) ────────────
@Composable
private fun HorizontalProductCard(
    product: ProductEntity,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(155.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(195.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = product.images.firstOrNull(),
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (product.isNew || product.originalPrice > product.price) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (product.isNew) "New"
                        else "-${((1 - product.price / product.originalPrice) * 100).toInt()}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.88f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onFavoriteClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Color.Red else Color(0xFF888888),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = product.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 17.sp
        )
        if (product.brand.isNotBlank()) {
            Text(
                text = product.brand,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (product.originalPrice > product.price) {
                Text(
                    text = "₴ ${product.originalPrice.toInt()}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDecoration = TextDecoration.LineThrough
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            Text(
                text = "₴ ${product.price.toInt()}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

// ── Promotion Card ────────────────────────────────────────────────────────────
@Composable
private fun PromotionCard(product: ProductEntity, onClick: () -> Unit) {
    val pct = if (product.originalPrice > 0 && product.originalPrice > product.price)
        ((1 - product.price / product.originalPrice) * 100).toInt() else 0
    Box(
        modifier = Modifier
            .width(185.dp)
            .height(125.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE65C00))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = product.images.firstOrNull(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.35f
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            if (pct > 0) {
                Text(
                    text = "-$pct%",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                    fontSize = 30.sp
                )
            }
            Text(
                text = product.subCategory.ifBlank { "Summer collection" },
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

// ── Brand Card ────────────────────────────────────────────────────────────────
@Composable
private fun BrandCard(brandName: String, imageUrl: String?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 130.dp, height = 85.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomStart
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.6f
            )
        }
        Box(
            modifier = Modifier
                .padding(8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = brandName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(title: String, onShowAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        TextButton(onClick = onShowAll, contentPadding = PaddingValues(0.dp)) {
            Text(
                "Show all",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}
