package com.example.nextgenecommerce.presentation.screens.tryon

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.nextgenecommerce.models.TryOnState
import com.example.nextgenecommerce.presentation.components.IconTextButton
import com.example.nextgenecommerce.presentation.components.PrimaryButton
import com.example.nextgenecommerce.presentation.components.SecondaryButton
import com.example.nextgenecommerce.presentation.viewmodel.ProductViewModel
import com.example.nextgenecommerce.viewmodel.TryOnViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TryOnScreen(
    navController: NavController,
    productId: String,
    imageIndex: Int = 0,
    productViewModel: ProductViewModel = hiltViewModel(),
    tryOnViewModel: TryOnViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val product by productViewModel.selectedProduct.collectAsState()
    val tryOnState by tryOnViewModel.tryOnState.collectAsState()
    val avatarBitmap by tryOnViewModel.avatarBitmap.collectAsState()
    val avatarSource by tryOnViewModel.avatarSource.collectAsState()

    var showCameraView by remember { mutableStateOf(false) }
    var showAvatarSourceDialog by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showCameraView = true
        } else {
            Toast.makeText(
                context,
                "Camera permission is required to take your photo.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(productId) {
        productViewModel.getProductById(productId)
    }

    LaunchedEffect(product) {
        product?.let { prod ->
            val imageUrl = prod.images.getOrElse(imageIndex) { prod.images.firstOrNull() }
            if (!imageUrl.isNullOrEmpty()) {
                tryOnViewModel.setProductImageUrl(imageUrl)
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { tryOnViewModel.setAvatarFromUri(context, it) }
    }

    if (showAvatarSourceDialog) {
        AlertDialog(
            onDismissRequest = { showAvatarSourceDialog = false },
            title = { Text("Choose Your Photo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

                    TextButton(
                        onClick = {
                            showAvatarSourceDialog = false
                            val hasCameraPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasCameraPermission) {
                                showCameraView = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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
        },
        bottomBar = {
            if (tryOnState is TryOnState.Success) {
                val successState = tryOnState as TryOnState.Success
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp,
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconTextButton(
                            text = "Try Again",
                            icon = { Icon(Icons.Default.Refresh, null) },
                            onClick = { tryOnViewModel.clearAvatar() },
                            modifier = Modifier.weight(1f),
                            isPrimary = false
                        )
                        IconTextButton(
                            text = "Save",
                            icon = { Icon(Icons.Default.SaveAlt, null) },
                            onClick = {
                                val saved = saveBitmapToGallery(context, successState.resultBitmap)
                                Toast.makeText(
                                    context,
                                    if (saved) "Image saved to Gallery" else "Failed to save image",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f),
                            isPrimary = true
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (showCameraView) {
            LiveCameraTryOnView(
                productImageUrl = product?.images?.getOrElse(imageIndex) { product?.images?.firstOrNull() } ?: "",
                onPhotoCaptured = { bitmap ->
                    tryOnViewModel.setAvatarFromBitmap(bitmap)
                    showCameraView = false
                },
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

                when (val state = tryOnState) {
                    is TryOnState.Idle -> {
                        if (avatarBitmap != null) {
                            AvatarPreviewSection(
                                avatarBitmap = avatarBitmap!!,
                                avatarSource = avatarSource,
                                onChangeAvatar = { showAvatarSourceDialog = true },
                                onTryOn = { tryOnViewModel.processTryOn(context) }
                            )
                        } else {
                            AvatarSelectionScreen(
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
                            onTryAgain = { tryOnViewModel.clearAvatar() },
                            onSaveToGallery = {
                                val saved = saveBitmapToGallery(context, state.resultBitmap)
                                Toast.makeText(
                                    context,
                                    if (saved) "Image saved to Gallery" else "Failed to save image",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }

                    is TryOnState.Error -> {
                        ErrorSection(
                            message = state.message,
                            retryable = state.retryable,
                            onRetry = { tryOnViewModel.retry(context) },
                            onChangePhoto = { tryOnViewModel.clearAvatar() }
                        )
                    }
                }
            }
        }
    }
}

private fun saveBitmapToGallery(context: android.content.Context, bitmap: Bitmap): Boolean {
    return try {
        val filename = "TryOn_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NextGenEcommerce")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(it, contentValues, null, null)
            }
            true
        } ?: false
    } catch (e: Exception) {
        false
    }
}

@Composable
private fun AvatarSelectionScreen(
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
            text = "Upload a photo or take one to see how this product looks on you",
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
    onChangeAvatar: () -> Unit,
    onTryOn: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

        if (avatarSource != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (avatarSource) {
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

        PrimaryButton(
            text = "Try On This Image",
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
    onSaveToGallery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Try-On Result",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "AI has generated your virtual preview",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 400.dp, max = 600.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Image(
                bitmap = resultBitmap.asImageBitmap(),
                contentDescription = "Try-on result",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(80.dp)) // Padding for fixed bottom bar
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
