package com.example.nextgenecommerce.presentation.screens.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nextgenecommerce.data.models.ProductCategory
import com.example.nextgenecommerce.data.models.ProductEntity
import com.example.nextgenecommerce.presentation.viewmodel.AuthViewModel
import com.example.nextgenecommerce.presentation.viewmodel.ProductViewModel
import com.example.nextgenecommerce.util.Resource
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    navController: NavController,
    productId: String? = null,
    productViewModel: ProductViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
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

    // MULTIPLE IMAGES - Each image selected one at a time
    var imageUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var newImageUrl by remember { mutableStateOf("") }
    var isUploadingImage by remember { mutableStateOf(false) }

    var lensId by remember { mutableStateOf("") }

    var isFeatured by remember { mutableStateOf(false) }
    var isNew by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isAdmin by authViewModel.isAdmin.collectAsState()
    val productOperationState by productViewModel.productOperationState.collectAsState()
    val imageUploadState by productViewModel.imageUploadState.collectAsState()

    // Gallery image picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            isUploadingImage = true
            productViewModel.uploadProductImage(it)
        }
    }

    // Handle image upload result
    LaunchedEffect(imageUploadState) {
        when (imageUploadState) {
            is Resource.Success -> {
                isUploadingImage = false
                val url = (imageUploadState as Resource.Success<String>).data
                if (url != null) {
                    imageUrls = imageUrls + url
                }
                productViewModel.resetImageUploadState()
            }
            is Resource.Error -> {
                isUploadingImage = false
                snackbarHostState.showSnackbar(
                    (imageUploadState as Resource.Error).message ?: "Failed to upload image"
                )
                productViewModel.resetImageUploadState()
            }
            is Resource.Loading -> {
                isUploadingImage = true
            }
            null -> {}
        }
    }

    // Handle product operation state
    LaunchedEffect(productOperationState) {
        when (productOperationState) {
            is Resource.Success -> {
                isSaving = false
                val message = if (editingProductId != null) "Product updated successfully!" else "Product added successfully!"
                snackbarHostState.showSnackbar(message)
                productViewModel.resetProductOperationState()
                navController.popBackStack()
            }
            is Resource.Error -> {
                isSaving = false
                snackbarHostState.showSnackbar(
                    (productOperationState as Resource.Error).message ?: "Operation failed"
                )
                productViewModel.resetProductOperationState()
            }
            is Resource.Loading -> {
                isSaving = true
            }
            null -> {
                isSaving = false
            }
        }
    }

    // Load product data if editing
    LaunchedEffect(productId) {
        if (productId != null) {
            productViewModel.getProductById(productId)
        }
    }

    val selectedProduct by productViewModel.selectedProduct.collectAsState()

    // Populate form when product is loaded for editing
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
            imageUrls = product.images
            lensId = product.lensId ?: ""
            isFeatured = product.isFeatured
            isNew = product.isNew
        }
    }

    val isEditMode = editingProductId != null

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
            // ========== MULTIPLE IMAGES SECTION ==========
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Product Images",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add images from gallery or by URL",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Upload from gallery button
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUploadingImage
                    ) {
                        if (isUploadingImage) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Uploading...")
                        } else {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pick from Gallery")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Image count
                    val totalImages = imageUrls.size
                    if (totalImages > 0) {
                        Text(
                            text = "$totalImages image${if (totalImages != 1) "s" else ""} added",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Horizontal scrollable list of image containers
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        // Display URL-based images with index
                        itemsIndexed(imageUrls) { index, url ->
                            val displayIndex = index
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .fillMaxHeight()
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxSize(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = "Image ${displayIndex + 1}",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(MaterialTheme.shapes.medium),
                                            contentScale = ContentScale.Crop
                                        )

                                        // Image number badge
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(4.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Text(
                                                text = "${displayIndex + 1}",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }

                                        // Remove button
                                        IconButton(
                                            onClick = {
                                                imageUrls = imageUrls.toMutableList().apply {
                                                    removeAt(index)
                                                }
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(28.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.error,
                                                    shape = CircleShape
                                                )
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onError
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Add image by URL section
                    Text(
                        text = "Add image by URL:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newImageUrl,
                            onValueChange = { newImageUrl = it },
                            label = { Text("Image URL") },
                            placeholder = { Text("https://example.com/image.jpg") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        FilledTonalButton(
                            onClick = {
                                if (newImageUrl.isNotBlank()) {
                                    imageUrls = imageUrls + newImageUrl.trim()
                                    newImageUrl = ""
                                }
                            },
                            enabled = newImageUrl.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add URL")
                        }
                    }
                }
            }
            // ========== END MULTIPLE IMAGES SECTION ==========

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
                                    val isSelected = selectedSizes.contains(size)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedSizes = if (isSelected) {
                                                selectedSizes - size
                                            } else {
                                                selectedSizes + size
                                            }
                                        },
                                        label = {
                                            Text(
                                                size,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            borderColor = MaterialTheme.colorScheme.outline,
                                            selectedBorderColor = MaterialTheme.colorScheme.primary,
                                            borderWidth = 1.dp,
                                            selectedBorderWidth = 2.dp
                                        )
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
                                            label = {
                                                Text(
                                                    color,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            },
                                            trailingIcon = {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.onPrimary
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = InputChipDefaults.inputChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                                selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // AR Lens Configuration
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "AR Lens Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Add a Snapchat Lens ID for live AR try-on",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = lensId,
                        onValueChange = { lensId = it },
                        label = { Text("Lens ID") },
                        placeholder = { Text("e.g., abc123-def456-...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                        }
                    )
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

                    if (imageUrls.isEmpty()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Please add at least one image URL")
                        }
                        return@Button
                    }

                    val product = ProductEntity(
                        id = editingProductId ?: UUID.randomUUID().toString(),
                        name = productName,
                        description = description,
                        price = price.toDoubleOrNull() ?: 0.0,
                        originalPrice = originalPrice.toDoubleOrNull() ?: price.toDoubleOrNull() ?: 0.0,
                        category = selectedCategory,
                        subCategory = subCategory,
                        images = imageUrls,
                        lensId = lensId.ifBlank { null },
                        sizes = selectedSizes.toList(),
                        colors = selectedColors.toList(),
                        stock = stock.toIntOrNull() ?: 0,
                        brand = brand,
                        isFeatured = isFeatured,
                        isNew = isNew,
                        inStock = (stock.toIntOrNull() ?: 0) > 0
                    )

                    // Save or update product to Supabase
                    if (isEditMode) {
                        productViewModel.updateProductInSupabase(product)
                    } else {
                        productViewModel.addProductToSupabase(product)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEditMode) "Update Product" else "Save Product", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
