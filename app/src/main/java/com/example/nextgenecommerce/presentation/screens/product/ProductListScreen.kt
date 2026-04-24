package com.example.nextgenecommerce.presentation.screens.product

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.nextgenecommerce.data.models.ProductCategory
import com.example.nextgenecommerce.presentation.components.ProductCard
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.ProductViewModel
import com.example.nextgenecommerce.presentation.viewmodel.WishlistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    navController: NavController,
    category: String,
    viewModel: ProductViewModel = hiltViewModel(),
    wishlistViewModel: WishlistViewModel = hiltViewModel()
) {
    val categoryProducts by viewModel.categoryProducts.collectAsStateWithLifecycle()
    val wishlistItems by wishlistViewModel.wishlistItems.collectAsStateWithLifecycle()

    var isLoading by remember { mutableStateOf(true) }
    var selectedSubcategory by remember { mutableStateOf<String?>(null) }
    var showSortSheet by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf("New") }

    val subcategories = remember(categoryProducts) {
        categoryProducts.mapNotNull { it.subCategory.ifBlank { null } }.distinct().take(8)
    }

    val displayedProducts = remember(categoryProducts, selectedSubcategory, sortOption) {
        var list = if (selectedSubcategory != null)
            categoryProducts.filter { it.subCategory == selectedSubcategory }
        else categoryProducts
        list = when (sortOption) {
            "Price high to low" -> list.sortedByDescending { it.price }
            "Price low to high" -> list.sortedBy { it.price }
            else -> list
        }
        list
    }

    LaunchedEffect(category) {
        if (categoryProducts.isEmpty()) isLoading = true
        viewModel.loadProductsByCategory(ProductCategory.valueOf(category))
    }

    LaunchedEffect(categoryProducts) {
        if (categoryProducts.isNotEmpty()) isLoading = false
    }

    if (showSortSheet) {
        SortBottomSheet(
            selected = sortOption,
            onSelect = { sortOption = it; showSortSheet = false },
            onDismiss = { showSortSheet = false }
        )
    }

    val categoryTitle = category.replace("_", " ")
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // ── Top bar row ──────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "< Back" text
                    Row(
                        modifier = Modifier
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { navController.popBackStack() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "Back",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.sp
                        )
                    }

                    // Centred category title
                    Text(
                        text = categoryTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    // Search icon
                    IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                        Icon(
                            Icons.Default.Search,
                            "Search",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // ── Filter / Sort / Grid row ──────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filter icon (≡ lines) in rounded box
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Sort icon (⚙ sliders) in rounded box — with optional badge
                    BadgedBox(
                        badge = {
                            if (sortOption != "New") {
                                Badge(containerColor = Color(0xFFFF6600)) {
                                    Text("1", fontSize = 9.sp, color = Color.White)
                                }
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { showSortSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Sort",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Grid view toggle icon
                    Icon(
                        Icons.Outlined.GridView,
                        contentDescription = "Grid view",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // ── Category chips ────────────────────────────────────────
                if (subcategories.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        subcategories.forEach { sub ->
                            val isSelected = selectedSubcategory == sub
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.onBackground
                                        else Color.Transparent
                                    )
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { selectedSubcategory = if (isSelected) null else sub }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = sub,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.background
                                    else MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
            }
        } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(displayedProducts, key = { it.id }) { product ->
                ProductCard(
                    product = product,
                    onProductClick = {
                        navController.navigate(Screen.ProductDetail.createRoute(product.id)) {
                            launchSingleTop = true
                        }
                    },
                    onFavoriteClick = {
                        wishlistViewModel.toggleWishlistItem(product)
                    },
                    isFavorite = wishlistItems.any { it.productId == product.id }
                )
            }
        }
        } // end else
    }
}

// ── Sort Bottom Sheet ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortBottomSheet(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf("New", "Price high to low", "Price low to high")
    var current by remember { mutableStateOf(selected) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sorting",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            options.forEachIndexed { i, option ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (current == option)
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            else Color.Transparent
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { current = option }
                        .padding(vertical = 15.dp, horizontal = 12.dp)
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = if (current == option) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                if (i < options.lastIndex) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onSelect(current) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text("Select", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}
