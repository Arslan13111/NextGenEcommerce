package com.example.nextgenecommerce.presentation.screens.admin

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// Every color variant is explicitly paired with one image URL
private data class ColorVariant(val color: String, val imageUrl: String)

private enum class ColorPickerSource { PENDING, EDIT_FORM, DIRECT_VARIANT }

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
    var lensId by remember { mutableStateOf("") }
    var isFeatured by remember { mutableStateOf(false) }
    var isNew by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Color picker dialog state
    var colorPickerSource by remember { mutableStateOf<ColorPickerSource?>(null) }
    var colorPickerVariantIndex by remember { mutableStateOf(-1) }

    // Size selection
    val availableSizes = listOf("XS", "S", "M", "L", "XL", "XXL")
    var selectedSizes by remember { mutableStateOf(setOf<String>()) }

    // Color variants — each color is explicitly paired with its image
    var colorVariants by remember { mutableStateOf<List<ColorVariant>>(emptyList()) }

    // Pending variant being built (add new)
    var showAddVariantForm by remember { mutableStateOf(false) }
    var pendingColor by remember { mutableStateOf("") }
    var pendingImageUrl by remember { mutableStateOf("") }
    var isUploadingVariantImage by remember { mutableStateOf(false) }

    // Editing existing variant in-place
    var editingVariantIndex by remember { mutableStateOf<Int?>(null) }
    var editingColor by remember { mutableStateOf("") }
    var editingImageUrl by remember { mutableStateOf("") }
    var isUploadingEditImage by remember { mutableStateOf(false) }
    // true = upload feeds editingImageUrl, false = upload feeds pendingImageUrl
    var galleryTargetIsEdit by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val productOperationState by productViewModel.productOperationState.collectAsState()
    val imageUploadState by productViewModel.imageUploadState.collectAsState()

    // Clear any stale operation state left over from previous screens (e.g. admin dashboard
    // color-dot changes) so we don't show a phantom error the moment this screen appears.
    LaunchedEffect(Unit) { productViewModel.resetProductOperationState() }

    // Gallery launcher — routes result to either pending (new) or editing (existing)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            if (galleryTargetIsEdit) isUploadingEditImage = true
            else isUploadingVariantImage = true
            productViewModel.uploadProductImage(it)
        }
    }

    // Handle image upload → route to the correct target
    LaunchedEffect(imageUploadState) {
        when (imageUploadState) {
            is Resource.Success -> {
                val url = (imageUploadState as Resource.Success<String>).data
                if (url != null) {
                    if (galleryTargetIsEdit) editingImageUrl = url
                    else pendingImageUrl = url
                }
                isUploadingVariantImage = false
                isUploadingEditImage = false
                productViewModel.resetImageUploadState()
            }
            is Resource.Error -> {
                isUploadingVariantImage = false
                isUploadingEditImage = false
                snackbarHostState.showSnackbar(
                    (imageUploadState as Resource.Error).message ?: "Failed to upload image"
                )
                productViewModel.resetImageUploadState()
            }
            is Resource.Loading -> {
                if (galleryTargetIsEdit) isUploadingEditImage = true
                else isUploadingVariantImage = true
            }
            null -> {}
        }
    }

    // Handle save/update result
    LaunchedEffect(productOperationState) {
        when (productOperationState) {
            is Resource.Success -> {
                isSaving = false
                val msg = if (editingProductId != null) "Product updated!" else "Product added!"
                snackbarHostState.showSnackbar(msg)
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
            is Resource.Loading -> isSaving = true
            null -> isSaving = false
        }
    }

    // Load existing product for edit mode
    LaunchedEffect(productId) {
        if (productId != null) productViewModel.getProductById(productId)
    }
    val selectedProduct by productViewModel.selectedProduct.collectAsState()
    LaunchedEffect(selectedProduct) {
        selectedProduct?.let { p ->
            editingProductId = p.id
            productName = p.name
            description = p.description
            price = p.price.toString()
            originalPrice = p.originalPrice.toString()
            brand = p.brand
            stock = p.stock.toString()
            selectedCategory = p.category
            subCategory = p.subCategory
            selectedSizes = p.sizes.toSet()
            lensId = p.lensId ?: ""
            isFeatured = p.isFeatured
            isNew = p.isNew
            // Re-pair colors ↔ images by position (existing data contract)
            colorVariants = p.colors.mapIndexed { i, color ->
                ColorVariant(color, p.images.getOrElse(i) { "" })
            }
        }
    }

    val isEditMode = editingProductId != null

    colorPickerSource?.let { src ->
        ColorPickerDialog(
            currentColorName = when (src) {
                ColorPickerSource.PENDING       -> pendingColor
                ColorPickerSource.EDIT_FORM     -> editingColor
                ColorPickerSource.DIRECT_VARIANT -> colorVariants.getOrNull(colorPickerVariantIndex)?.color ?: ""
            },
            onDismiss = { colorPickerSource = null },
            onColorSelected = { newColor ->
                when (src) {
                    ColorPickerSource.PENDING       -> pendingColor = newColor
                    ColorPickerSource.EDIT_FORM     -> editingColor = newColor
                    ColorPickerSource.DIRECT_VARIANT -> {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {

            // ── Color Variants (color + image pairs) ──────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Color Variants",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Each color must have its own matching image",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (colorVariants.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "${colorVariants.size}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        // Existing variants list
                        colorVariants.forEachIndexed { index, variant ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                // Variant row header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Position badge
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Text(
                                                "${index + 1}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    // Color swatch dot — tap to open color picker
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(colorNameToPreview(variant.color))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                                            .clickable {
                                                colorPickerVariantIndex = index
                                                colorPickerSource = ColorPickerSource.DIRECT_VARIANT
                                            }
                                    )
                                    // Color name
                                    Text(
                                        variant.color,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    // Image thumbnail
                                    if (variant.imageUrl.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            AsyncImage(
                                                model = variant.imageUrl,
                                                contentDescription = variant.color,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.BrokenImage, null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(22.dp))
                                        }
                                    }
                                    // Edit button
                                    IconButton(
                                        onClick = {
                                            if (editingVariantIndex == index) {
                                                editingVariantIndex = null
                                            } else {
                                                editingVariantIndex = index
                                                editingColor = variant.color
                                                editingImageUrl = variant.imageUrl
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            if (editingVariantIndex == index) Icons.Default.ExpandLess else Icons.Default.Edit,
                                            "Edit",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    // Delete
                                    IconButton(
                                        onClick = {
                                            if (editingVariantIndex == index) editingVariantIndex = null
                                            colorVariants = colorVariants.toMutableList().apply { removeAt(index) }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, "Remove",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp))
                                    }
                                }

                                // Inline edit form (expandable per variant)
                                AnimatedVisibility(
                                    visible = editingVariantIndex == index,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                                            )
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            "Edit Variant",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        // Color name with live swatch preview — tap swatch to open picker
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(colorNameToPreview(editingColor))
                                                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                                                    .clickable { colorPickerSource = ColorPickerSource.EDIT_FORM }
                                            )
                                            OutlinedTextField(
                                                value = editingColor,
                                                onValueChange = { editingColor = it },
                                                label = { Text("Color name") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                        }

                                        // Image replacement
                                        Text(
                                            "Image",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            if (editingImageUrl.isNotBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(72.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                ) {
                                                    AsyncImage(
                                                        model = editingImageUrl,
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            galleryTargetIsEdit = true
                                                            galleryLauncher.launch("image/*")
                                                        },
                                                        modifier = Modifier
                                                            .size(22.dp)
                                                            .align(Alignment.TopEnd)
                                                            .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                                    ) {
                                                        Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                            } else {
                                                OutlinedButton(
                                                    onClick = {
                                                        galleryTargetIsEdit = true
                                                        galleryLauncher.launch("image/*")
                                                    },
                                                    enabled = !isUploadingEditImage,
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    if (isUploadingEditImage) {
                                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Uploading...", fontSize = 13.sp)
                                                    } else {
                                                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Pick Image", fontSize = 13.sp)
                                                    }
                                                }
                                            }
                                            if (isUploadingEditImage) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                            }
                                        }

                                        // Save / Cancel
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { editingVariantIndex = null },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp)
                                            ) { Text("Cancel") }
                                            Button(
                                                onClick = {
                                                    if (editingColor.isNotBlank() && editingImageUrl.isNotBlank()) {
                                                        colorVariants = colorVariants.toMutableList().also { list ->
                                                            list[index] = ColorVariant(editingColor.trim(), editingImageUrl)
                                                        }
                                                        editingVariantIndex = null
                                                    } else {
                                                        scope.launch {
                                                            snackbarHostState.showSnackbar(
                                                                if (editingColor.isBlank()) "Enter a color name" else "Image required"
                                                            )
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp),
                                                enabled = !isUploadingEditImage
                                            ) {
                                                Icon(Icons.Default.Save, null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Save", fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Add variant form (expandable)
                        AnimatedVisibility(
                            visible = showAddVariantForm,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "New Color Variant",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Color name + swatch preview — tap swatch to open picker
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(colorNameToPreview(pendingColor))
                                            .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                                            .clickable { colorPickerSource = ColorPickerSource.PENDING }
                                    )
                                    OutlinedTextField(
                                        value = pendingColor,
                                        onValueChange = { pendingColor = it },
                                        label = { Text("Color name (e.g., Black, Navy Blue)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                // Image picker / thumbnail
                                Text(
                                    "Image for this color",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (pendingImageUrl.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            AsyncImage(
                                                model = pendingImageUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            // Replace button
                                            IconButton(
                                                onClick = {
                                                    galleryTargetIsEdit = false
                                                    galleryLauncher.launch("image/*")
                                                },
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .align(Alignment.TopEnd)
                                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                            ) {
                                                Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                galleryTargetIsEdit = false
                                                galleryLauncher.launch("image/*")
                                            },
                                            enabled = !isUploadingVariantImage,
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            if (isUploadingVariantImage) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Uploading...", fontSize = 13.sp)
                                            } else {
                                                Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Pick Image", fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }

                                // Cancel / Add buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            showAddVariantForm = false
                                            pendingColor = ""
                                            pendingImageUrl = ""
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Cancel")
                                    }
                                    Button(
                                        onClick = {
                                            if (pendingColor.isNotBlank() && pendingImageUrl.isNotBlank()) {
                                                colorVariants = colorVariants + ColorVariant(pendingColor.trim(), pendingImageUrl)
                                                pendingColor = ""
                                                pendingImageUrl = ""
                                                showAddVariantForm = false
                                            } else {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        if (pendingColor.isBlank()) "Enter a color name"
                                                        else "Pick an image for this color"
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        enabled = !isUploadingVariantImage
                                    ) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add Variant", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        // Show "Add Color Variant" button only when form is hidden
                        if (!showAddVariantForm) {
                            OutlinedButton(
                                onClick = { showAddVariantForm = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Color Variant")
                            }
                        }
                    }
                }
            }

            // ── Basic Information ─────────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Basic Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

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
            }

            // ── Pricing & Stock ───────────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Pricing & Stock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = price,
                                onValueChange = { price = it },
                                label = { Text("Price") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                leadingIcon = { Text("PKR") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                            OutlinedTextField(
                                value = originalPrice,
                                onValueChange = { originalPrice = it },
                                label = { Text("Original Price") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                leadingIcon = { Text("PKR") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                        OutlinedTextField(
                            value = stock,
                            onValueChange = { stock = it },
                            label = { Text("Stock Quantity") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            // ── Category ──────────────────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

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
                                modifier = Modifier.fillMaxWidth().menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = showCategoryMenu,
                                onDismissRequest = { showCategoryMenu = false }
                            ) {
                                ProductCategory.values().forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category.name) },
                                        onClick = { selectedCategory = category; showCategoryMenu = false }
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
            }

            // ── Available Sizes ───────────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Available Sizes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableSizes.chunked(3).forEach { row ->
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEach { size ->
                                        val isSel = selectedSizes.contains(size)
                                        FilterChip(
                                            selected = isSel,
                                            onClick = {
                                                selectedSizes = if (isSel) selectedSizes - size else selectedSizes + size
                                            },
                                            label = {
                                                Text(
                                                    size,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary
                                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            },
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
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("AR Lens Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Add a Snapchat Lens ID for live AR try-on",
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
                            leadingIcon = { Icon(Icons.Default.CameraAlt, null) }
                        )
                    }
                }
            }

            // ── Additional Options ────────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
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
                        val cleanPrice = price.trim().replace(",", "")
                        val cleanOriginal = originalPrice.trim().replace(",", "")
                        when {
                            productName.isBlank() -> scope.launch { snackbarHostState.showSnackbar("Enter product name") }
                            cleanPrice.isBlank() || cleanPrice.toDoubleOrNull() == null -> scope.launch { snackbarHostState.showSnackbar("Enter a valid price (numbers only)") }
                            selectedSizes.isEmpty() -> scope.launch { snackbarHostState.showSnackbar("Select at least one size") }
                            colorVariants.isEmpty() -> scope.launch { snackbarHostState.showSnackbar("Add at least one color variant with an image") }
                            colorVariants.any { it.imageUrl.isBlank() } -> scope.launch { snackbarHostState.showSnackbar("All color variants must have an image") }
                            else -> {
                                val parsedPrice = cleanPrice.toDouble()
                                val parsedOriginal = cleanOriginal.toDoubleOrNull() ?: parsedPrice
                                val product = ProductEntity(
                                    id = editingProductId ?: UUID.randomUUID().toString(),
                                    name = productName,
                                    description = description,
                                    price = parsedPrice,
                                    originalPrice = parsedOriginal,
                                    category = selectedCategory,
                                    subCategory = subCategory,
                                    images = colorVariants.map { it.imageUrl },
                                    colors = colorVariants.map { it.color },
                                    // Explicit map so ProductDetailScreen never has to guess
                                    colorImages = colorVariants.associate { it.color to it.imageUrl },
                                    lensId = lensId.ifBlank { null },
                                    sizes = selectedSizes.toList(),
                                    stock = stock.toIntOrNull() ?: 0,
                                    brand = brand,
                                    isFeatured = isFeatured,
                                    isNew = isNew,
                                    inStock = (stock.toIntOrNull() ?: 0) > 0
                                )
                                if (isEditMode) productViewModel.updateProductInSupabase(product)
                                else productViewModel.addProductToSupabase(product)
                            }
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
                        Icon(Icons.Default.Save, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isEditMode) "Update Product" else "Save Product",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
