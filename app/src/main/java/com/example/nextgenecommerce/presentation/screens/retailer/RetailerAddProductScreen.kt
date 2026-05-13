package com.example.nextgenecommerce.presentation.screens.retailer

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nextgenecommerce.data.models.ProductCategory
import com.example.nextgenecommerce.data.models.SupabaseProduct
import com.example.nextgenecommerce.presentation.screens.admin.ColorPickerDialog
import com.example.nextgenecommerce.presentation.screens.admin.colorNameToPreview
import com.example.nextgenecommerce.presentation.viewmodel.ProductViewModel
import com.example.nextgenecommerce.presentation.viewmodel.RetailerViewModel
import com.example.nextgenecommerce.util.Resource
import kotlinx.coroutines.launch
import java.util.UUID

private data class RetailerColorVariant(val color: String, val imageUrl: String)
private enum class RetailerColorPickerSource { PENDING, EDIT_FORM, DIRECT_VARIANT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RetailerAddProductScreen(
    navController: NavController,
    productId: String? = null,
    retailerViewModel: RetailerViewModel = hiltViewModel(),
    productViewModel: ProductViewModel = hiltViewModel()
) {
    val store by retailerViewModel.store.collectAsState()
    val products by retailerViewModel.products.collectAsState()
    val actionState by retailerViewModel.actionState.collectAsState()
    val imageUploadState by productViewModel.imageUploadState.collectAsState()

    // Resolve edit target from the products already loaded in RetailerViewModel
    val existingProduct = remember(productId, products) {
        if (productId == null) null
        else (products as? Resource.Success)?.data?.find { it.id == productId }
    }

    // ── Form state ──────────────────────────────────────────────────────────────
    var productName     by remember { mutableStateOf("") }
    var description     by remember { mutableStateOf("") }
    var price           by remember { mutableStateOf("") }
    var originalPrice   by remember { mutableStateOf("") }
    var brand           by remember { mutableStateOf("") }
    var stock           by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ProductCategory.CLOTHING) }
    var subCategory     by remember { mutableStateOf("") }
    var lensId          by remember { mutableStateOf("") }
    var isFeatured      by remember { mutableStateOf(false) }
    var isNew           by remember { mutableStateOf(true) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var isSaving        by remember { mutableStateOf(false) }

    // ── Size state ──────────────────────────────────────────────────────────────
    val availableSizes = listOf("XS", "S", "M", "L", "XL", "XXL")
    var selectedSizes   by remember { mutableStateOf(setOf<String>()) }

    // ── Color variants ──────────────────────────────────────────────────────────
    var colorVariants      by remember { mutableStateOf<List<RetailerColorVariant>>(emptyList()) }
    var showAddVariantForm  by remember { mutableStateOf(false) }
    var pendingColor       by remember { mutableStateOf("") }
    var pendingImageUrl    by remember { mutableStateOf("") }
    var isUploadingVariant by remember { mutableStateOf(false) }
    var editingVariantIndex by remember { mutableStateOf<Int?>(null) }
    var editingColor       by remember { mutableStateOf("") }
    var editingImageUrl    by remember { mutableStateOf("") }
    var isUploadingEditImage by remember { mutableStateOf(false) }
    var galleryTargetIsEdit by remember { mutableStateOf(false) }

    // ── Color picker state ──────────────────────────────────────────────────────
    var colorPickerSource      by remember { mutableStateOf<RetailerColorPickerSource?>(null) }
    var colorPickerVariantIndex by remember { mutableStateOf(-1) }

    // ── Misc ────────────────────────────────────────────────────────────────────
    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()
    var showDeleteDialog  by remember { mutableStateOf(false) }
    var formInitialized   by remember { mutableStateOf(false) }

    // Populate form when editing an existing product
    LaunchedEffect(existingProduct) {
        if (existingProduct != null && !formInitialized) {
            formInitialized  = true
            productName      = existingProduct.name
            description      = existingProduct.description
            price            = existingProduct.price.toString()
            originalPrice    = if (existingProduct.originalPrice > 0) existingProduct.originalPrice.toString() else ""
            brand            = existingProduct.brand
            stock            = existingProduct.stock.toString()
            selectedCategory = runCatching { ProductCategory.valueOf(existingProduct.category) }.getOrDefault(ProductCategory.CLOTHING)
            subCategory      = existingProduct.subCategory
            lensId           = existingProduct.lensId ?: ""
            isFeatured       = existingProduct.isFeatured
            isNew            = existingProduct.isNew
            selectedSizes    = existingProduct.sizes.toSet()
            colorVariants    = existingProduct.colors.mapIndexed { i, c ->
                RetailerColorVariant(c, existingProduct.images.getOrElse(i) { existingProduct.imageUrl ?: "" })
            }
        }
    }

    // Clear stale operation state on enter
    LaunchedEffect(Unit) { retailerViewModel.resetActionState() }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            if (galleryTargetIsEdit) isUploadingEditImage = true else isUploadingVariant = true
            productViewModel.uploadProductImage(it)
        }
    }

    // Route image upload result to correct target
    LaunchedEffect(imageUploadState) {
        when (val s = imageUploadState) {
            is Resource.Success -> {
                val url = s.data ?: return@LaunchedEffect
                if (galleryTargetIsEdit) editingImageUrl = url else pendingImageUrl = url
                isUploadingVariant = false
                isUploadingEditImage = false
                productViewModel.resetImageUploadState()
            }
            is Resource.Error -> {
                isUploadingVariant = false
                isUploadingEditImage = false
                snackbarHostState.showSnackbar(s.message ?: "Upload failed")
                productViewModel.resetImageUploadState()
            }
            is Resource.Loading -> {
                if (galleryTargetIsEdit) isUploadingEditImage = true else isUploadingVariant = true
            }
            null -> {}
        }
    }

    // Handle save / update result from RetailerViewModel
    LaunchedEffect(actionState) {
        when (val s = actionState) {
            is Resource.Success -> {
                isSaving = false
                snackbarHostState.showSnackbar(if (productId != null) "Product updated!" else "Product added!")
                retailerViewModel.resetActionState()
                navController.popBackStack()
            }
            is Resource.Error -> {
                isSaving = false
                snackbarHostState.showSnackbar(s.message ?: "Operation failed")
                retailerViewModel.resetActionState()
            }
            is Resource.Loading -> isSaving = true
            null -> isSaving = false
        }
    }

    // Color picker dialog
    colorPickerSource?.let { src ->
        ColorPickerDialog(
            currentColorName = when (src) {
                RetailerColorPickerSource.PENDING        -> pendingColor
                RetailerColorPickerSource.EDIT_FORM      -> editingColor
                RetailerColorPickerSource.DIRECT_VARIANT -> colorVariants.getOrNull(colorPickerVariantIndex)?.color ?: ""
            },
            onDismiss = { colorPickerSource = null },
            onColorSelected = { newColor ->
                when (src) {
                    RetailerColorPickerSource.PENDING        -> pendingColor = newColor
                    RetailerColorPickerSource.EDIT_FORM      -> editingColor = newColor
                    RetailerColorPickerSource.DIRECT_VARIANT -> {
                        colorVariants = colorVariants.toMutableList().also { list ->
                            if (colorPickerVariantIndex in list.indices)
                                list[colorPickerVariantIndex] = list[colorPickerVariantIndex].copy(color = newColor)
                        }
                    }
                }
                colorPickerSource = null
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Product") },
            text  = { Text("Delete \"$productName\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    productId?.let { retailerViewModel.deleteProduct(it) }
                    navController.popBackStack()
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    val isEditMode = productId != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Product" else "Add New Product") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, "Delete Product", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {

            // ── Color Variants ────────────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Color Variants", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Each color must have its own matching image", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (colorVariants.isNotEmpty()) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text("${colorVariants.size}", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }

                        // Existing variants
                        colorVariants.forEachIndexed { index, variant ->
                            Column(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), modifier = Modifier.size(24.dp)) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Text("${index + 1}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Box(
                                        modifier = Modifier.size(28.dp).clip(CircleShape)
                                            .background(colorNameToPreview(variant.color))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                                            .clickable { colorPickerVariantIndex = index; colorPickerSource = RetailerColorPickerSource.DIRECT_VARIANT }
                                    )
                                    Text(variant.color, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
                                    if (variant.imageUrl.isNotBlank()) {
                                        Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                            AsyncImage(model = variant.imageUrl, contentDescription = variant.color, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        }
                                    } else {
                                        Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.BrokenImage, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                                        }
                                    }
                                    IconButton(onClick = {
                                        if (editingVariantIndex == index) editingVariantIndex = null
                                        else { editingVariantIndex = index; editingColor = variant.color; editingImageUrl = variant.imageUrl }
                                    }, modifier = Modifier.size(32.dp)) {
                                        Icon(if (editingVariantIndex == index) Icons.Default.ExpandLess else Icons.Default.Edit, "Edit",
                                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = {
                                        if (editingVariantIndex == index) editingVariantIndex = null
                                        colorVariants = colorVariants.toMutableList().apply { removeAt(index) }
                                    }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }

                                // Inline edit form
                                AnimatedVisibility(visible = editingVariantIndex == index, enter = expandVertically(), exit = shrinkVertically()) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text("Edit Variant", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Box(
                                                modifier = Modifier.size(40.dp).clip(CircleShape)
                                                    .background(colorNameToPreview(editingColor))
                                                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                                                    .clickable { colorPickerSource = RetailerColorPickerSource.EDIT_FORM }
                                            )
                                            OutlinedTextField(value = editingColor, onValueChange = { editingColor = it }, label = { Text("Color name") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(10.dp))
                                        }
                                        Text("Image", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            if (editingImageUrl.isNotBlank()) {
                                                Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                                    AsyncImage(model = editingImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                                    IconButton(onClick = { galleryTargetIsEdit = true; galleryLauncher.launch("image/*") },
                                                        modifier = Modifier.size(22.dp).align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.55f), CircleShape)) {
                                                        Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                            } else {
                                                OutlinedButton(onClick = { galleryTargetIsEdit = true; galleryLauncher.launch("image/*") }, enabled = !isUploadingEditImage, shape = RoundedCornerShape(10.dp)) {
                                                    if (isUploadingEditImage) { CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(6.dp)); Text("Uploading...", fontSize = 13.sp) }
                                                    else { Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Pick Image", fontSize = 13.sp) }
                                                }
                                            }
                                            if (isUploadingEditImage) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(onClick = { editingVariantIndex = null }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text("Cancel") }
                                            Button(onClick = {
                                                if (editingColor.isNotBlank() && editingImageUrl.isNotBlank()) {
                                                    colorVariants = colorVariants.toMutableList().also { it[index] = RetailerColorVariant(editingColor.trim(), editingImageUrl) }
                                                    editingVariantIndex = null
                                                } else scope.launch { snackbarHostState.showSnackbar(if (editingColor.isBlank()) "Enter a color name" else "Image required") }
                                            }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), enabled = !isUploadingEditImage) {
                                                Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Save", fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Add variant form
                        AnimatedVisibility(visible = showAddVariantForm, enter = expandVertically(), exit = shrinkVertically()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("New Color Variant", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier.size(42.dp).clip(CircleShape)
                                            .background(colorNameToPreview(pendingColor))
                                            .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                                            .clickable { colorPickerSource = RetailerColorPickerSource.PENDING }
                                    )
                                    OutlinedTextField(value = pendingColor, onValueChange = { pendingColor = it }, label = { Text("Color name (e.g., Black, Navy Blue)") }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(10.dp))
                                }
                                Text("Image for this color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (pendingImageUrl.isNotBlank()) {
                                        Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                            AsyncImage(model = pendingImageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                            IconButton(onClick = { galleryTargetIsEdit = false; galleryLauncher.launch("image/*") },
                                                modifier = Modifier.size(22.dp).align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.55f), CircleShape)) {
                                                Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    } else {
                                        OutlinedButton(onClick = { galleryTargetIsEdit = false; galleryLauncher.launch("image/*") }, enabled = !isUploadingVariant, shape = RoundedCornerShape(10.dp)) {
                                            if (isUploadingVariant) { CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(6.dp)); Text("Uploading...", fontSize = 13.sp) }
                                            else { Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Pick Image", fontSize = 13.sp) }
                                        }
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { showAddVariantForm = false; pendingColor = ""; pendingImageUrl = "" }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Text("Cancel") }
                                    Button(onClick = {
                                        if (pendingColor.isNotBlank() && pendingImageUrl.isNotBlank()) {
                                            colorVariants = colorVariants + RetailerColorVariant(pendingColor.trim(), pendingImageUrl)
                                            pendingColor = ""; pendingImageUrl = ""; showAddVariantForm = false
                                        } else scope.launch { snackbarHostState.showSnackbar(if (pendingColor.isBlank()) "Enter a color name" else "Pick an image for this color") }
                                    }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), enabled = !isUploadingVariant) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Add Variant", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        if (!showAddVariantForm) {
                            OutlinedButton(onClick = { showAddVariantForm = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Add Color Variant")
                            }
                        }
                    }
                }
            }

            // ── Basic Information ─────────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Basic Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = productName, onValueChange = { productName = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5)
                        
                        // Default brand to store name if empty and we have store info
                        LaunchedEffect(store) {
                            if (productId == null && brand.isBlank() && store != null) {
                                brand = store!!.storeName
                            }
                        }

                        OutlinedTextField(
                            value = brand, 
                            onValueChange = { brand = it }, 
                            label = { Text("Brand / Store Name") }, 
                            modifier = Modifier.fillMaxWidth(), 
                            singleLine = true,
                            supportingText = { Text("This will be shown to customers as the product brand.") }
                        )
                    }
                }
            }

            // ── Pricing & Stock ───────────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Pricing & Stock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price") }, modifier = Modifier.weight(1f), singleLine = true, leadingIcon = { Text("PKR") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                            OutlinedTextField(value = originalPrice, onValueChange = { originalPrice = it }, label = { Text("Original Price") }, modifier = Modifier.weight(1f), singleLine = true, leadingIcon = { Text("PKR") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        }
                        OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stock Quantity") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
            }

            // ── Category ──────────────────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        ExposedDropdownMenuBox(expanded = showCategoryMenu, onExpandedChange = { showCategoryMenu = it }) {
                            OutlinedTextField(
                                value = selectedCategory.name, onValueChange = {}, readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryMenu) },
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                                ProductCategory.entries.forEach { cat ->
                                    DropdownMenuItem(text = { Text(cat.name) }, onClick = { selectedCategory = cat; showCategoryMenu = false })
                                }
                            }
                        }
                        OutlinedTextField(value = subCategory, onValueChange = { subCategory = it }, label = { Text("Sub Category") }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("e.g., T-Shirts, Dresses, Jackets") })
                    }
                }
            }

            // ── Available Sizes ───────────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Available Sizes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            availableSizes.chunked(3).forEach { row ->
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { size ->
                                        val isSel = selectedSizes.contains(size)
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { selectedSizes = if (isSel) selectedSizes - size else selectedSizes + size },
                                            label = { Text(size, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal, color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── AR Lens Configuration ─────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("AR Lens Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Add a Snapchat Lens ID for live AR try-on", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(value = lensId, onValueChange = { lensId = it }, label = { Text("Lens ID") }, placeholder = { Text("e.g., abc123-def456-...") }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.CameraAlt, null) })
                    }
                }
            }

            // ── Additional Options ────────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Additional Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isFeatured, onCheckedChange = { isFeatured = it })
                            Text("Featured Product")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isNew, onCheckedChange = { isNew = it })
                            Text("New Arrival")
                        }
                    }
                }
            }

            // ── Save Button ───────────────────────────────────────────────────
            item {
                Button(
                    onClick = {
                        val cleanPrice    = price.trim().replace(",", "")
                        val cleanOriginal = originalPrice.trim().replace(",", "")
                        when {
                            productName.isBlank()                              -> scope.launch { snackbarHostState.showSnackbar("Enter product name") }
                            cleanPrice.isBlank() || cleanPrice.toDoubleOrNull() == null -> scope.launch { snackbarHostState.showSnackbar("Enter a valid price") }
                            selectedSizes.isEmpty()                            -> scope.launch { snackbarHostState.showSnackbar("Select at least one size") }
                            colorVariants.isEmpty()                            -> scope.launch { snackbarHostState.showSnackbar("Add at least one color variant with an image") }
                            colorVariants.any { it.imageUrl.isBlank() }        -> scope.launch { snackbarHostState.showSnackbar("All color variants must have an image") }
                            else -> {
                                val parsedPrice    = cleanPrice.toDouble()
                                val parsedOriginal = cleanOriginal.toDoubleOrNull() ?: parsedPrice
                                val retailerId     = store?.id ?: existingProduct?.retailerId ?: ""
                                val product = SupabaseProduct(
                                    id           = productId ?: UUID.randomUUID().toString(),
                                    name         = productName.trim(),
                                    description  = description.trim(),
                                    price        = parsedPrice,
                                    originalPrice = parsedOriginal,
                                    category     = selectedCategory.name,
                                    subCategory  = subCategory.trim(),
                                    images       = colorVariants.map { it.imageUrl },
                                    imageUrl     = colorVariants.firstOrNull()?.imageUrl,
                                    colors       = colorVariants.map { it.color },
                                    colorImages  = colorVariants.associate { it.color to it.imageUrl },
                                    sizes        = selectedSizes.toList(),
                                    stock        = stock.toIntOrNull() ?: 0,
                                    inStock      = (stock.toIntOrNull() ?: 0) > 0,
                                    brand        = brand.trim(),
                                    lensId       = lensId.trim().ifBlank { null },
                                    isFeatured   = isFeatured,
                                    isNew        = isNew,
                                    retailerId   = retailerId,
                                    rating       = existingProduct?.rating ?: 0.0,
                                    reviewCount  = existingProduct?.reviewCount ?: 0,
                                    tags         = existingProduct?.tags ?: emptyList(),
                                    createdAt    = existingProduct?.createdAt ?: System.currentTimeMillis(),
                                    updatedAt    = System.currentTimeMillis()
                                )
                                isSaving = true
                                if (isEditMode) retailerViewModel.updateProduct(product)
                                else retailerViewModel.addProduct(product)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Saving...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isEditMode) "Update Product" else "Save Product", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
