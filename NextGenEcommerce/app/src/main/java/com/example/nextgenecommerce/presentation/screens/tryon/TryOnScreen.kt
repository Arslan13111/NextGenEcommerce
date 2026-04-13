package com.example.nextgenecommerce.presentation.screens.tryon

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nextgenecommerce.models.TryOnState
import com.example.nextgenecommerce.presentation.components.PrimaryButton
import com.example.nextgenecommerce.presentation.components.SecondaryButton
import com.example.nextgenecommerce.presentation.viewmodel.AuthViewModel
import com.example.nextgenecommerce.presentation.viewmodel.ProductViewModel
import com.example.nextgenecommerce.viewmodel.TryOnViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun TryOnScreen(
    navController: NavController,
    productId: String,
    imageIndex: Int = 0,
    productViewModel: ProductViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    tryOnViewModel: TryOnViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val product by productViewModel.selectedProduct.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val tryOnState by tryOnViewModel.tryOnState.collectAsState()
    val avatarBitmap by tryOnViewModel.avatarBitmap.collectAsState()
    val avatarSource by tryOnViewModel.avatarSource.collectAsState()
    val resultBitmap by tryOnViewModel.resultBitmap.collectAsState()

    var showCameraView by remember { mutableStateOf(false) }
    var showAvatarSourceDialog by remember { mutableStateOf(false) }
    var selectedAvatarSex by remember { mutableStateOf("female") }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Load product and set the product image URL
    LaunchedEffect(productId) {
        productViewModel.getProductById(productId)
    }

    // When product loads, set the product image URL in the try-on ViewModel
    LaunchedEffect(product) {
        product?.let { prod ->
            // Use the image at the selected carousel index, falling back to first image
            val imageUrl = prod.images.getOrElse(imageIndex) { prod.images.firstOrNull() }
            if (!imageUrl.isNullOrEmpty()) {
                tryOnViewModel.setProductImageUrl(imageUrl)
            }
        }
    }

    // Image Picker Launcher (for avatar photo)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            tryOnViewModel.setAvatarFromUri(context, it)
        }
    }

    // Avatar source selection dialog
    if (showAvatarSourceDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarSourceDialog = false },
            title = { Text("Choose Your Photo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Option 1: Use Profile Photo (if available)
                    if (!currentUser?.profileImageUrl.isNullOrEmpty()) {
                        TextButton(
                            onClick = {
                                tryOnViewModel.setAvatarFromProfileUrl(currentUser!!.profileImageUrl!!)
                                showAvatarSourceDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null)
                                Text("Use Profile Photo")
                            }
                        }
                    }

                    // Option 2: Upload from Gallery
                    TextButton(
                        onClick = {
                            imagePickerLauncher.launch("image/*")
                            showAvatarSourceDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Text("Upload from Gallery")
                        }
                    }

                    // Option 3: Take Photo
                    TextButton(
                        onClick = {
                            showAvatarSourceDialog = false
                            if (cameraPermissionState.status.isGranted) {
                                showCameraView = true
                            } else {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Text("Take a Photo")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAvatarSourceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Virtual Try-On",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        tryOnViewModel.clearAll()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (showCameraView) {
            LiveCameraTryOnView(
                productImageUrl = product?.images?.getOrElse(imageIndex) { product?.images?.firstOrNull() } ?: "",
                onClose = { showCameraView = false }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Product Info Card
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
                                model = prod.images.getOrElse(imageIndex) { prod.images.firstOrNull() },
                                contentDescription = prod.name,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
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

                // Main content based on state
                when (val state = tryOnState) {
                    is TryOnState.Idle -> {
                        if (avatarBitmap != null) {
                            // Avatar selected - show preview and try-on button
                            AvatarPreviewSection(
                                avatarBitmap = avatarBitmap!!,
                                avatarSource = avatarSource,
                                selectedSex = selectedAvatarSex,
                                onSexChanged = { selectedAvatarSex = it },
                                onChangeAvatar = { showAvatarSourceDialog = true },
                                onTryOn = {
                                    tryOnViewModel.processTryOn(
                                        context = context,
                                        avatarSex = selectedAvatarSex
                                    )
                                }
                            )
                        } else {
                            // No avatar - show selection screen
                            AvatarSelectionScreen(
                                hasProfileImage = !currentUser?.profileImageUrl.isNullOrEmpty(),
                                onSelectSource = { showAvatarSourceDialog = true }
                            )
                        }
                    }

                    is TryOnState.Loading -> {
                        LoadingSection(message = state.message)
                    }

                    is TryOnState.Success -> {
                        ResultSection(
                            resultBitmap = state.resultBitmap,
                            onTryAgain = {
                                tryOnViewModel.clearAvatar()
                            },
                            onSaveShare = {
                                // TODO: Implement save and share
                            }
                        )
                    }

                    is TryOnState.Error -> {
                        ErrorSection(
                            message = state.message,
                            retryable = state.retryable,
                            onRetry = { tryOnViewModel.retry(context) },
                            onChangePhoto = {
                                tryOnViewModel.clearAvatar()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarSelectionScreen(
    hasProfileImage: Boolean,
    onSelectSource: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Try On This Product",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = if (hasProfileImage)
                "Use your profile photo or upload a new one to see how this product looks on you"
            else
                "Upload a photo or take one to see how this product looks on you",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        PrimaryButton(
            text = "Choose Your Photo",
            onClick = onSelectSource,
            modifier = Modifier.fillMaxWidth()
        )

        // Info card
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
                    text = "Our AI-powered try-on uses your photo and the product image from our catalog to generate a realistic preview.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun AvatarPreviewSection(
    avatarBitmap: Bitmap,
    avatarSource: String?,
    selectedSex: String,
    onSexChanged: (String) -> Unit,
    onChangeAvatar: () -> Unit,
    onTryOn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar preview
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 350.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = avatarBitmap.asImageBitmap(),
                    contentDescription = "Your photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        // Avatar source indicator
        if (avatarSource != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (avatarSource) {
                        "Profile Photo" -> Icons.Default.AccountCircle
                        "Camera Capture" -> Icons.Default.CameraAlt
                        else -> Icons.Default.PhotoLibrary
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = avatarSource,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Gender selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select Gender",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = selectedSex == "female",
                        onClick = { onSexChanged("female") },
                        label = { Text("Female") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedSex == "male",
                        onClick = { onSexChanged("male") },
                        label = { Text("Male") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Action buttons
        PrimaryButton(
            text = "Try On This Product",
            onClick = onTryOn,
            modifier = Modifier.fillMaxWidth()
        )

        SecondaryButton(
            text = "Choose Different Photo",
            onClick = onChangeAvatar,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LoadingSection(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        CircularProgressIndicator(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Please don't close this screen",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ResultSection(
    resultBitmap: Bitmap,
    onTryAgain: () -> Unit,
    onSaveShare: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Result label
        Text(
            text = "Try-On Result",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // Result image
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp, max = 500.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Image(
                bitmap = resultBitmap.asImageBitmap(),
                contentDescription = "Try-on result",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SecondaryButton(
                text = "Try Again",
                onClick = onTryAgain,
                modifier = Modifier.weight(1f)
            )
            PrimaryButton(
                text = "Save & Share",
                onClick = onSaveShare,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ErrorSection(
    message: String,
    retryable: Boolean,
    onRetry: () -> Unit,
    onChangePhoto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Text(
            text = "Try-On Failed",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.error
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (retryable) {
            PrimaryButton(
                text = "Retry",
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SecondaryButton(
            text = "Choose Different Photo",
            onClick = onChangePhoto,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
