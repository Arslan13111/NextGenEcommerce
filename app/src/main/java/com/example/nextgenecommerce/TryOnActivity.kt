package com.example.nextgenecommerce

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.nextgenecommerce.models.Product
import com.example.nextgenecommerce.models.TryOnState
import com.example.nextgenecommerce.viewmodel.TryOnViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Modern Try-On Activity using MVVM Architecture
 * Features:
 * - ViewModel with StateFlow for reactive state management
 * - Repository pattern for data layer separation
 * - ActivityResultContracts for image picking
 * - Runtime permission handling for Android 13+
 * - Lifecycle-aware state observation
 * - Clean separation of concerns
 */
@AndroidEntryPoint
class TryOnActivity : AppCompatActivity() {

    private val TAG = "TryOnActivity"

    // ViewModel
    private val viewModel: TryOnViewModel by viewModels()

    // Product data from intent
    private lateinit var product: Product
    private lateinit var selectedSize: String
    private lateinit var selectedColor: String

    // UI Components
    private lateinit var imgClothing: ImageView
    private lateinit var imgAvatar: ImageView
    private lateinit var imgResult: ImageView
    private lateinit var btnSelectClothing: Button
    private lateinit var btnSelectAvatar: Button
    private lateinit var btnUseCamera: Button
    private lateinit var btnTryOn: Button
    private lateinit var btnRetry: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var txtStatus: TextView

    // Image selection mode
    private var isSelectingClothing = false

    /**
     * Activity Result Launcher for image picking
     */
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleImageSelected(it) }
    }

    /**
     * Activity Result Launcher for camera
     */
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { handleCameraImage(it) }
    }

    /**
     * Permission Launcher for storage/media access
     */
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(
                this,
                "Storage permission is required to select images",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Camera Permission Launcher
     */
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(
                this,
                "Camera permission is required to take photos",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_try_on)

        // Get product data from intent
        @Suppress("DEPRECATION")
        product = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra("product", Product::class.java)
        } else {
            intent.getSerializableExtra("product") as? Product
        })!!
        selectedSize = intent.getStringExtra("selectedSize") ?: "M"
        selectedColor = intent.getStringExtra("selectedColor") ?: "Black"

        // Setup action bar
        supportActionBar?.title = "Try On - ${product.name}"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize UI
        setupViews()

        // Observe ViewModel state
        observeViewModel()

        // Load product image automatically
        loadProductImage()
    }

    /**
     * Setup UI components and click listeners
     */
    private fun setupViews() {
        // Find views
        imgClothing = findViewById(R.id.imgClothing)
        imgAvatar = findViewById(R.id.imgAvatar)
        imgResult = findViewById(R.id.imgResult)
        btnSelectClothing = findViewById(R.id.btnSelectClothing)
        btnSelectAvatar = findViewById(R.id.btnSelectAvatar)
        btnUseCamera = findViewById(R.id.btnUseCamera)
        btnTryOn = findViewById(R.id.btnTryOn)
        btnRetry = findViewById(R.id.btnRetry)
        progressBar = findViewById(R.id.progressBar)
        txtStatus = findViewById(R.id.txtStatus)

        // Hide select clothing button as it now comes from URL
        btnSelectClothing.isVisible = false

        // Setup click listeners
        btnSelectAvatar.setOnClickListener {
            isSelectingClothing = false
            checkPermissionAndPickImage()
        }

        btnUseCamera.setOnClickListener {
            isSelectingClothing = false // Camera is for avatar only
            checkCameraPermissionAndOpen()
        }

        btnTryOn.setOnClickListener {
            processTryOn()
        }

        btnRetry.setOnClickListener {
            viewModel.retry(this)
        }

        // Initially hide retry button
        btnRetry.isVisible = false
    }

    /**
     * Observe ViewModel state changes
     */
    private fun observeViewModel() {
        // Observe try-on state
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.tryOnState.collect { state ->
                    handleTryOnState(state)
                }
            }
        }

        // Observe product image URL
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.productImageUrl.collect { url ->
                    if (!url.isNullOrEmpty()) {
                        Glide.with(this@TryOnActivity)
                            .load(url)
                            .into(imgClothing)
                        updateTryOnButtonState()
                    }
                }
            }
        }

        // Observe avatar bitmap
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.avatarBitmap.collect { bitmap ->
                    bitmap?.let {
                        imgAvatar.setImageBitmap(it)
                        // Show Try On button when avatar is available
                        btnTryOn.isVisible = true
                        btnTryOn.isEnabled = viewModel.isReadyToProcess()
                        android.util.Log.d(TAG, "Avatar bitmap updated, showing Try On button")
                    }
                }
            }
        }
    }

    /**
     * Handle different try-on states
     */
    private fun handleTryOnState(state: TryOnState) {
        android.util.Log.d(TAG, "State changed: ${state::class.simpleName}")

        when (state) {
            is TryOnState.Idle -> {
                progressBar.isVisible = false
                txtStatus.isVisible = false
                btnTryOn.isEnabled = viewModel.isReadyToProcess()
                // Only show Try On button if avatar is uploaded
                btnTryOn.isVisible = viewModel.avatarBitmap.value != null
                btnRetry.isVisible = false
                imgResult.isVisible = false
            }

            is TryOnState.Loading -> {
                progressBar.isVisible = true
                txtStatus.isVisible = true
                txtStatus.text = state.message
                btnTryOn.isEnabled = false
                btnRetry.isVisible = false
                imgResult.isVisible = false
            }

            is TryOnState.Success -> {
                progressBar.isVisible = false
                txtStatus.isVisible = false
                btnTryOn.isEnabled = true
                btnTryOn.isVisible = true // Keep button visible for retry
                btnRetry.isVisible = false
                imgResult.setImageBitmap(state.resultBitmap)
                imgResult.isVisible = true

                Toast.makeText(
                    this,
                    "Try-on complete! Here's how it looks on you!",
                    Toast.LENGTH_LONG
                ).show()

                android.util.Log.d(TAG, "Try-on successful!")
            }

            is TryOnState.Error -> {
                progressBar.isVisible = false
                txtStatus.isVisible = true
                txtStatus.text = "Error: ${state.message}"
                btnTryOn.isEnabled = viewModel.isReadyToProcess()
                btnTryOn.isVisible = viewModel.avatarBitmap.value != null // Show if avatar available
                btnRetry.isVisible = state.retryable
                imgResult.isVisible = false

                Toast.makeText(
                    this,
                    state.message,
                    Toast.LENGTH_LONG
                ).show()

                android.util.Log.e(TAG, "Try-on error: ${state.message}", state.exception)
            }
        }
    }

    /**
     * Load product image from URL
     */
    private fun loadProductImage() {
        android.util.Log.d(TAG, "Loading product image from URL: ${product.imageUrl}")
        viewModel.setProductImageUrl(product.imageUrl)
    }

    /**
     * Check permission and open image picker
     */
    private fun checkPermissionAndPickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                Toast.makeText(
                    this,
                    "Storage permission is needed to select images",
                    Toast.LENGTH_LONG
                ).show()
                permissionLauncher.launch(permission)
            }
            else -> {
                permissionLauncher.launch(permission)
            }
        }
    }

    /**
     * Check camera permission and open camera
     */
    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(
                    this,
                    "Camera permission is needed to take photos",
                    Toast.LENGTH_LONG
                ).show()
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Open image picker
     */
    private fun openImagePicker() {
        android.util.Log.d(TAG, "Opening image picker (selecting clothing: $isSelectingClothing)")
        imagePickerLauncher.launch("image/*")
    }

    /**
     * Open camera
     */
    private fun openCamera() {
        android.util.Log.d(TAG, "Opening camera")
        cameraLauncher.launch(null)
    }

    /**
     * Handle selected image from picker
     */
    private fun handleImageSelected(uri: Uri) {
        android.util.Log.d(TAG, "Image selected: $uri (clothing: $isSelectingClothing)")

        if (isSelectingClothing) {
            // This shouldn't be reachable since btnSelectClothing is hidden,
            // but we'll keep it safe.
            Toast.makeText(this, "Manual clothing selection not supported", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.setAvatarFromUri(this, uri)
            // Show Try On button when avatar is uploaded
            btnTryOn.isVisible = true
            Toast.makeText(
                this,
                "Photo uploaded! Click 'Try On' to see how it looks!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Handle captured image from camera
     */
    private fun handleCameraImage(bitmap: Bitmap) {
        android.util.Log.d(TAG, "Camera image captured: ${bitmap.width}x${bitmap.height}")

        viewModel.setAvatarFromBitmap(bitmap)
        // Show Try On button when photo is captured
        btnTryOn.isVisible = true
        Toast.makeText(
            this,
            "Photo captured! Click 'Try On' to see how it looks!",
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Update Try On button state based on image selection
     * Since product image is auto-loaded, only check if avatar is present
     */
    private fun updateTryOnButtonState() {
        val isReady = viewModel.isReadyToProcess()
        btnTryOn.isEnabled = isReady
        // Show button if avatar is available
        if (viewModel.avatarBitmap.value != null) {
            btnTryOn.isVisible = true
        }
        android.util.Log.d(TAG, "Try On button state - enabled: $isReady, visible: ${btnTryOn.isVisible}")
    }

    /**
     * Process virtual try-on
     */
    private fun processTryOn() {
        android.util.Log.d(TAG, "Try On button clicked")

        if (!viewModel.isReadyToProcess()) {
            Toast.makeText(
                this,
                "Please select your photo first",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Reset state and start processing
        viewModel.resetState()
        viewModel.processTryOn(context = this)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        // ViewModel will automatically clean up bitmaps in onCleared()
        android.util.Log.d(TAG, "Activity destroyed")
    }
}
