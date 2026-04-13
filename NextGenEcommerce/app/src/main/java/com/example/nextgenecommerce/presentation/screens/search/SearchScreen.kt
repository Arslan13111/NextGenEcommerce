package com.example.nextgenecommerce.presentation.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.nextgenecommerce.data.models.ProductCategory
import com.example.nextgenecommerce.presentation.components.ProductCard
import com.example.nextgenecommerce.presentation.navigation.Screen
import com.example.nextgenecommerce.presentation.viewmodel.ProductViewModel
import com.example.nextgenecommerce.presentation.viewmodel.WishlistViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    productViewModel: ProductViewModel = hiltViewModel(),
    wishlistViewModel: WishlistViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }

    // Filter states
    var selectedCategories by remember { mutableStateOf<Set<ProductCategory>>(emptySet()) }
    var priceRangeStart by remember { mutableStateOf(0f) }
    var priceRangeEnd by remember { mutableStateOf(10000f) }
    var minRating by remember { mutableStateOf(0f) }
    var sortOption by remember { mutableStateOf(SortOption.RELEVANCE) }

    val allProducts by productViewModel.allProducts.collectAsState()
    val wishlistItems by wishlistViewModel.wishlistItems.collectAsState()

    // Apply filters and search
    val searchResults = remember(
        searchQuery,
        allProducts,
        selectedCategories,
        priceRangeStart,
        priceRangeEnd,
        minRating,
        sortOption
    ) {
        if (searchQuery.isBlank() && selectedCategories.isEmpty() && minRating == 0f) {
            emptyList()
        } else {
            var filtered = allProducts.filter { product ->
                val matchesSearch = if (searchQuery.isBlank()) {
                    true
                } else {
                    product.name.contains(searchQuery, ignoreCase = true) ||
                    product.description.contains(searchQuery, ignoreCase = true) ||
                    product.category.name.contains(searchQuery, ignoreCase = true)
                }

                val matchesCategory = if (selectedCategories.isEmpty()) {
                    true
                } else {
                    selectedCategories.contains(product.category)
                }

                val matchesPrice = product.price in priceRangeStart.toDouble()..priceRangeEnd.toDouble()

                val matchesRating = product.rating >= minRating

                matchesSearch && matchesCategory && matchesPrice && matchesRating
            }

            // Apply sorting
            filtered = when (sortOption) {
                SortOption.RELEVANCE -> filtered
                SortOption.PRICE_LOW_TO_HIGH -> filtered.sortedBy { it.price }
                SortOption.PRICE_HIGH_TO_LOW -> filtered.sortedByDescending { it.price }
                SortOption.RATING -> filtered.sortedByDescending { it.rating }
                SortOption.NEWEST -> filtered.sortedByDescending { it.createdAt }
            }

            filtered
        }
    }

    val hasActiveFilters = selectedCategories.isNotEmpty() || minRating > 0f ||
            priceRangeStart > 0f || priceRangeEnd < 10000f

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Search products...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                        ),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, "Clear search")
                                }
                            }
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            // Filter and Sort Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filter button
                FilterChip(
                    selected = hasActiveFilters,
                    onClick = { showFilterDialog = true },
                    label = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filter",
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Filters")
                            if (hasActiveFilters) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Text("")
                                }
                            }
                        }
                    }
                )

                // Sort dropdown
                var sortExpanded by remember { mutableStateOf(false) }
                Box {
                    FilterChip(
                        selected = false,
                        onClick = { sortExpanded = true },
                        label = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Sort,
                                    contentDescription = "Sort",
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(sortOption.displayName)
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        SortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName) },
                                onClick = {
                                    sortOption = option
                                    sortExpanded = false
                                },
                                leadingIcon = {
                                    if (sortOption == option) {
                                        Icon(Icons.Default.Check, null)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            when {
                searchQuery.isBlank() && !hasActiveFilters -> {
                    // Initial state - show search prompt
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            modifier = Modifier.size(120.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Search for products",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start typing or use filters to find your desired products",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                searchResults.isEmpty() -> {
                    // No results found
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = "No results",
                            modifier = Modifier.size(120.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "No products found",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try adjusting your search or filters",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (hasActiveFilters) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(
                                onClick = {
                                    selectedCategories = emptySet()
                                    priceRangeStart = 0f
                                    priceRangeEnd = 10000f
                                    minRating = 0f
                                }
                            ) {
                                Text("Clear filters")
                            }
                        }
                    }
                }
                else -> {
                    // Display search results
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Active filters chips
                        if (hasActiveFilters) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(selectedCategories.toList()) { category ->
                                    InputChip(
                                        selected = true,
                                        onClick = {
                                            selectedCategories = selectedCategories - category
                                        },
                                        label = { Text(category.name) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove filter",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Text(
                            text = "${searchResults.size} product${if (searchResults.size != 1) "s" else ""} found",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(searchResults) { product ->
                                ProductCard(
                                    product = product,
                                    onProductClick = {
                                        navController.navigate(Screen.ProductDetail.createRoute(product.id))
                                    },
                                    onFavoriteClick = {
                                        wishlistViewModel.toggleWishlistItem(product)
                                    },
                                    isFavorite = wishlistItems.any { it.productId == product.id }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Filter Dialog
    if (showFilterDialog) {
        FilterDialog(
            selectedCategories = selectedCategories,
            priceRangeStart = priceRangeStart,
            priceRangeEnd = priceRangeEnd,
            minRating = minRating,
            onCategoriesChange = { selectedCategories = it },
            onPriceRangeChange = { start, end ->
                priceRangeStart = start
                priceRangeEnd = end
            },
            onMinRatingChange = { minRating = it },
            onDismiss = { showFilterDialog = false },
            onClearAll = {
                selectedCategories = emptySet()
                priceRangeStart = 0f
                priceRangeEnd = 10000f
                minRating = 0f
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    selectedCategories: Set<ProductCategory>,
    priceRangeStart: Float,
    priceRangeEnd: Float,
    minRating: Float,
    onCategoriesChange: (Set<ProductCategory>) -> Unit,
    onPriceRangeChange: (Float, Float) -> Unit,
    onMinRatingChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    onClearAll: () -> Unit
) {
    var localPriceStart by remember { mutableStateOf(priceRangeStart) }
    var localPriceEnd by remember { mutableStateOf(priceRangeEnd) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filters")
                TextButton(onClick = onClearAll) {
                    Text("Clear all")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                // Categories
                Text(
                    "Categories",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                ProductCategory.values().forEach { category ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedCategories.contains(category),
                            onCheckedChange = { checked ->
                                onCategoriesChange(
                                    if (checked) {
                                        selectedCategories + category
                                    } else {
                                        selectedCategories - category
                                    }
                                )
                            }
                        )
                        Text(
                            text = category.name.replace("_", " "),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Price Range
                Text(
                    "Price Range",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "$ ${localPriceStart.toInt()} - $ ${localPriceEnd.toInt()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                RangeSlider(
                    value = localPriceStart..localPriceEnd,
                    onValueChange = { range ->
                        localPriceStart = range.start
                        localPriceEnd = range.endInclusive
                    },
                    valueRange = 0f..10000f,
                    steps = 99
                )

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                // Rating
                Text(
                    "Minimum Rating",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (0..5).forEach { rating ->
                        FilterChip(
                            selected = minRating == rating.toFloat(),
                            onClick = {
                                onMinRatingChange(
                                    if (minRating == rating.toFloat()) 0f else rating.toFloat()
                                )
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("$rating")
                                    if (rating > 0) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPriceRangeChange(localPriceStart, localPriceEnd)
                    onDismiss()
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

enum class SortOption(val displayName: String) {
    RELEVANCE("Relevance"),
    PRICE_LOW_TO_HIGH("Price: Low to High"),
    PRICE_HIGH_TO_LOW("Price: High to Low"),
    RATING("Highest Rated"),
    NEWEST("Newest")
}
