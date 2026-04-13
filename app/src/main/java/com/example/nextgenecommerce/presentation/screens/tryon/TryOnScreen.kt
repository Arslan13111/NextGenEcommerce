package com.example.nextgenecommerce.presentation.screens.tryon

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.nextgenecommerce.api.TryOnDiffusionClient
import com.example.nextgenecommerce.models.TryOnResult
import com.example.nextgenecommerce.presentation.components.IconTextButton
import com.example.nextgenecommerce.presentation.components.PrimaryButton
import com.example.nextgenecommerce.presentation.components.SecondaryButton
import com.example.nextgenecommerce.presentation.viewmodel.ProductViewModel
import com.example.nextgenecommerce.repository.TryOnRepository
import com.example.nextgenecommerce.util.ImageUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun TryOnScreen(
    navController: NavController,
    productId: String,
    productViewModel: ProductViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val product by productViewModel.selectedProduct.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showCameraView by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var processedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Create repository instance
    val tryOnRepository = remember {
        TryOnRepository(
            apiService = TryOnDiffusionClient.apiService,
            apiKey = TryOnDiffusionClient.RAPID_API_KEY,
            apiHost = TryOnDiffusionClient.RAPID_API_HOST
        )
    }

    LaunchedEffect(productId) {
        productViewModel.getProductById(productId)
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // User will click "Try On" button to process
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Try On",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
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
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Product Info
            product?.let { prod ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = prod.images.firstOrNull(),
                            contentDescription = prod.name,
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.shapes.medium
                                ),
                            contentScale = ContentScale.Crop
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = prod.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = prod.brand,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$${prod.price}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            // Main Content
            if (showCameraView) {
                // Live Camera View
                LiveCameraTryOnView(
                    productImageUrl = product?.images?.firstOrNull() ?: "",
                    onClose = { showCameraView = false }
                )
            } else if (selectedImageUri != null || processedImageBitmap != null) {
                // Display selected/processed image
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isProcessing) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(48.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Processing your try-on...",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                if (processedImageBitmap != null) {
                                    // Show processed result
                                    Image(
                                        bitmap = processedImageBitmap!!.asImageBitmap(),
                                        contentDescription = "Try-on result",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    // Show uploaded photo
                                    Image(
                                        painter = rememberAsyncImagePainter(selectedImageUri),
                                        contentDescription = "Your uploaded photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }

                    if (!isProcessing) {
                        // Show "Try On" button if image is uploaded but not processed yet
                        if (selectedImageUri != null && processedImageBitmap == null) {
                            PrimaryButton(
                                text = "✨ Try On This Product",
                                onClick = {
                                    coroutineScope.launch {
                                        isProcessing = true
                                        errorMessage = null

                                        try {
                                            // Load user's uploaded image
                                            val avatarBitmap = ImageUtils.loadBitmapFromUri(context, selectedImageUri!!)

                                            // Load product image from URL (first image)
                                            val productImageUrl = product?.images?.firstOrNull()

                                            if (avatarBitmap != null && productImageUrl != null) {
                                                // Load product image from assets or URL
                                                val clothingBitmap = product?.localImageName?.let {
                                                    ImageUtils.loadBitmapFromAssets(context, it, "products")
                                                }

                                                if (clothingBitmap != null) {
                                                    // Call repository to process try-on
                                                    val result = tryOnRepository.processTryOn(
                                                        context = context,
                                                        clothingBitmap = clothingBitmap,
                                                        avatarBitmap = avatarBitmap
                                                    )

                                                    when (result) {
                                                        is TryOnResult.Success -> {
                                                            processedImageBitmap = result.imageBitmap
                                                        }
                                                        is TryOnResult.Failure -> {
                                                            errorMessage = result.error.getUserMessage()
                                                        }
                                                    }
                                                } else {
                                                    errorMessage = "Failed to load product image"
                                                }
                                            } else {
                                                errorMessage = "Failed to load your photo or product image"
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Error: ${e.message}"
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            SecondaryButton(
                                text = "Choose Different Photo",
                                onClick = {
                                    selectedImageUri = null
                                    errorMessage = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (processedImageBitmap != null) {
                            // Show action buttons after processing is complete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SecondaryButton(
                                    text = "Try Again",
                                    onClick = {
                                        selectedImageUri = null
                                        processedImageBitmap = null
                                        errorMessage = null
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                PrimaryButton(
                                    text = "Save & Share",
                                    onClick = {
                                        // TODO: Implement save and share functionality
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            } else {
                // Selection Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Choose how to try on",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Upload a photo or use your camera to see how this product looks on you",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Upload Photo Button
                    IconTextButton(
                        text = "Upload Photo",
                        icon = {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                "Upload Photo",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            imagePickerLauncher.launch("image/*")
                        },
                        isPrimary = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Camera Button
                    IconTextButton(
                        text = "Use Live Camera",
                        icon = {
                            Icon(
                                Icons.Default.CameraAlt,
                                "Camera",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            if (cameraPermissionState.status.isGranted) {
                                showCameraView = true
                            } else {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        },
                        isPrimary = false
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Our AI-powered try-on technology helps you visualize how products will look before purchasing.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

