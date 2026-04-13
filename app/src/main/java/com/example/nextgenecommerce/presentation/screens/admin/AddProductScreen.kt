package com.example.nextgenecommerce.presentation.screens.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nextgenecommerce.data.models.ProductCategory
import com.example.nextgenecommerce.data.models.ProductEntity
import com.example.nextgenecommerce.presentation.viewmodel.ProductViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavController,
    productId: String? = null,
    productViewModel: ProductViewModel = hiltViewModel()
) {
    var editingProductId by remember { mutableStateOf<String?>(null) }
    var productName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var originalPrice by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ProductCategory.CLOTHING) }
    var subCategory by remember { mutableStateOf("") }

    // Size selection
    val availableSizes = listOf("XS", "S", "M", "L", "XL", "XXL")
    var selectedSizes by remember { mutableStateOf(setOf<String>()) }

    // Color selection
    var customColor by remember { mutableStateOf("") }
    var selectedColors by remember { mutableStateOf(setOf<String>()) }

    // Image handling
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUrl by remember { mutableStateOf("") }

    var isFeatured by remember { mutableStateOf(false) }
    var isNew by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Load product data if editing
    LaunchedEffect(productId) {
        if (productId != null) {
            productViewModel.getProductById(productId)
        }
    }

    val selectedProduct by productViewModel.selectedProduct.collectAsState()

    // Populate form when product is loaded
    LaunchedEffect(selectedProduct) {
        selectedProduct?.let { product ->
            editingProductId = product.id
            productName = product.name
            description = product.description
            price = product.price.toString()
            originalPrice = product.originalPrice.toString()
            brand = product.brand
            stock = product.stock.toString()
            selectedCategory = product.category
            subCategory = product.subCategory
            selectedSizes = product.sizes.toSet()
            selectedColors = product.colors.toSet()
            imageUrl = product.images.firstOrNull() ?: ""
            isFeatured = product.isFeatured
            isNew = product.isNew
        }
    }

    val isEditMode = editingProductId != null

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Product" else "Add New Product") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Image Upload Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Product Image",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .border(
                                2.dp,
                                MaterialTheme.colorScheme.outline,
                                MaterialTheme.shapes.medium
                            )
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Product Image",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (imageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Product Image",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Tap to select image",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Or enter image URL:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Image URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Basic Information
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Basic Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = productName,
                        onValueChange = { productName = it },
                        label = { Text("Product Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )

                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Brand") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Pricing & Stock
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Pricing & Stock",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text("Price") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            leadingIcon = { Text("$") }
                        )

                        OutlinedTextField(
                            value = originalPrice,
                            onValueChange = { originalPrice = it },
                            label = { Text("Original Price") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            leadingIcon = { Text("$") }
                        )
                    }

                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it },
                        label = { Text("Stock Quantity") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Category
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    ExposedDropdownMenuBox(
                        expanded = showCategoryMenu,
                        onExpandedChange = { showCategoryMenu = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )

                        ExposedDropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            ProductCategory.values().forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedCategory = category
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = subCategory,
                        onValueChange = { subCategory = it },
                        label = { Text("Sub Category") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("e.g., T-Shirts, Dresses, Jackets") }
                    )
                }
            }

            // Sizes
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Available Sizes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableSizes.chunked(3).forEach { row ->
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { size ->
                                    FilterChip(
                                        selected = selectedSizes.contains(size),
                                        onClick = {
                                            selectedSizes = if (selectedSizes.contains(size)) {
                                                selectedSizes - size
                                            } else {
                                                selectedSizes + size
                                            }
                                        },
                                        label = { Text(size) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Colors
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Available Colors",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customColor,
                            onValueChange = { customColor = it },
                            label = { Text("Add Color") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (customColor.isNotBlank()) {
                                    selectedColors = selectedColors + customColor.trim()
                                    customColor = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Add, "Add")
                        }
                    }

                    if (selectedColors.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedColors.chunked(3).forEach { row ->
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    row.forEach { color ->
                                        InputChip(
                                            selected = true,
                                            onClick = {
                                                selectedColors = selectedColors - color
                                            },
                                            label = { Text(color) },
                                            trailingIcon = {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Additional Options
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Additional Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isFeatured,
                            onCheckedChange = { isFeatured = it }
                        )
                        Text("Featured Product")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isNew,
                            onCheckedChange = { isNew = it }
                        )
                        Text("New Arrival")
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    // Validation
                    if (productName.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please enter product name")
                        }
                        return@Button
                    }

                    if (price.isBlank() || price.toDoubleOrNull() == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please enter valid price")
                        }
                        return@Button
                    }

                    if (selectedSizes.isEmpty()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please select at least one size")
                        }
                        return@Button
                    }

                    if (selectedColors.isEmpty()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please add at least one color")
                        }
                        return@Button
                    }

                    // Create or update product
                    val imageToUse = if (selectedImageUri != null) {
                        selectedImageUri.toString()
                    } else if (imageUrl.isNotEmpty()) {
                        imageUrl
                    } else {
                        "https://via.placeholder.com/400"
                    }

                    val product = ProductEntity(
                        id = editingProductId ?: UUID.randomUUID().toString(),
                        name = productName,
                        description = description,
                        price = price.toDoubleOrNull() ?: 0.0,
                        originalPrice = originalPrice.toDoubleOrNull() ?: price.toDoubleOrNull() ?: 0.0,
                        category = selectedCategory,
                        subCategory = subCategory,
                        images = listOf(imageToUse),
                        sizes = selectedSizes.toList(),
                        colors = selectedColors.toList(),
                        stock = stock.toIntOrNull() ?: 0,
                        brand = brand,
                        isFeatured = isFeatured,
                        isNew = isNew,
                        inStock = (stock.toIntOrNull() ?: 0) > 0
                    )

                    // Save or update product
                    if (isEditMode) {
                        productViewModel.updateProduct(product)
                    } else {
                        productViewModel.addProduct(product)
                    }

                    scope.launch {
                        val message = if (isEditMode) "Product updated successfully!" else "Product added successfully!"
                        snackbarHostState.showSnackbar(message)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isEditMode) "Update Product" else "Save Product", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
