package com.example.nextgenecommerce.presentation.screens.tryon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import android.view.Surface
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun LiveCameraTryOnView(
    productImageUrl: String,
    onPhotoCaptured: (Bitmap) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(isFrontCamera, productImageUrl) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
                    .build()
                val cameraSelector = if (isFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    capture
                )
                imageCapture = capture
            } catch (exc: Exception) {
                Log.e("TryOnCamera", "Camera binding failed", exc)
                Toast.makeText(context, "Unable to start camera.", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close camera",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Take Your Photo",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.55f), MaterialTheme.shapes.small)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.size(48.dp))
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.72f)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Make sure your full body is in frame to try this product. Do not take only a face photo.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isFrontCamera = !isFrontCamera },
                        enabled = !isCapturing,
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.FlipCameraAndroid,
                            contentDescription = "Flip camera",
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val capture = imageCapture ?: return@IconButton
                            isCapturing = true
                            captureAvatarPhoto(
                                context = context,
                                imageCapture = capture,
                                cameraExecutor = cameraExecutor,
                                onSuccess = { bitmap ->
                                    isCapturing = false
                                    onPhotoCaptured(bitmap)
                                },
                                onError = { message, error ->
                                    isCapturing = false
                                    Log.e("TryOnCamera", message, error)
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        enabled = !isCapturing && imageCapture != null,
                        modifier = Modifier
                            .size(76.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(34.dp),
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Take photo",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(56.dp))
                }
            }
        }
    }
}

private fun captureAvatarPhoto(
    context: Context,
    imageCapture: ImageCapture,
    cameraExecutor: ExecutorService,
    onSuccess: (Bitmap) -> Unit,
    onError: (String, Throwable?) -> Unit
) {
    val photoFile = File(context.cacheDir, "try_on_avatar_${System.currentTimeMillis()}.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        cameraExecutor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val bitmap = decodeCapturedBitmap(photoFile)
                photoFile.delete()

                if (bitmap == null) {
                    ContextCompat.getMainExecutor(context).execute {
                        onError("Unable to read captured photo.", null)
                    }
                    return
                }

                ContextCompat.getMainExecutor(context).execute {
                    onSuccess(bitmap)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                photoFile.delete()
                ContextCompat.getMainExecutor(context).execute {
                    onError("Failed to capture photo. Please try again.", exception)
                }
            }
        }
    )
}

private fun decodeCapturedBitmap(file: File): Bitmap? {
    val decoded = BitmapFactory.decodeFile(file.absolutePath) ?: return null
    val exif = runCatching { ExifInterface(file.absolutePath) }.getOrNull()
    val rotation = when (exif?.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }

    if (rotation == 0f) return decoded

    val matrix = Matrix().apply { postRotate(rotation) }
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
        if (it != decoded) decoded.recycle()
    }
}
