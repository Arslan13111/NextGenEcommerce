package com.example.nextgenecommerce.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.models.SelectedImages
import com.example.nextgenecommerce.models.TryOnResult
import com.example.nextgenecommerce.models.TryOnState
import com.example.nextgenecommerce.repository.TryOnRepository
import com.example.nextgenecommerce.util.ImageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * ViewModel for Try-On Feature
 * Manages state and business logic for virtual try-on functionality
 */
class TryOnViewModel(
    private val repository: TryOnRepository
) : ViewModel() {

    private val TAG = "TryOnViewModel"

    // Mutable state flows (private)
    private val _tryOnState = MutableStateFlow<TryOnState>(TryOnState.Idle)
    private val _selectedImages = MutableStateFlow(SelectedImages())
    private val _clothingBitmap = MutableStateFlow<Bitmap?>(null)
    private val _avatarBitmap = MutableStateFlow<Bitmap?>(null)

    // Public read-only state flows
    val tryOnState: StateFlow<TryOnState> = _tryOnState.asStateFlow()
    val selectedImages: StateFlow<SelectedImages> = _selectedImages.asStateFlow()
    val clothingBitmap: StateFlow<Bitmap?> = _clothingBitmap.asStateFlow()
    val avatarBitmap: StateFlow<Bitmap?> = _avatarBitmap.asStateFlow()

    /**
     * Set clothing image from URI
     */
    fun setClothingImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "Loading clothing image from URI: $uri")

                val bitmap = ImageUtils.loadBitmapFromUri(context, uri)

                if (bitmap != null) {
                    _clothingBitmap.value = bitmap
                    _selectedImages.value = _selectedImages.value.copy(
                        clothingUri = uri,
                        clothingBitmap = bitmap
                    )
                    android.util.Log.d(TAG, "Clothing image loaded: ${bitmap.width}x${bitmap.height}")
                } else {
                    android.util.Log.e(TAG, "Failed to load clothing image from URI")
                    _tryOnState.value = TryOnState.Error(
                        message = "Failed to load clothing image",
                        retryable = false
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading clothing image: ${e.message}", e)
                _tryOnState.value = TryOnState.Error(
                    message = "Error loading clothing image: ${e.message}",
                    exception = e,
                    retryable = false
                )
            }
        }
    }

    /**
     * Set clothing image from Bitmap
     */
    fun setClothingImage(bitmap: Bitmap) {
        _clothingBitmap.value = bitmap
        _selectedImages.value = _selectedImages.value.copy(clothingBitmap = bitmap)
        android.util.Log.d(TAG, "Clothing bitmap set: ${bitmap.width}x${bitmap.height}")
    }

    /**
     * Set clothing image from assets
     */
    fun setClothingImageFromAssets(context: Context, fileName: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "Loading clothing image from assets: $fileName")

                val bitmap = ImageUtils.loadBitmapFromAssets(context, fileName, "products")

                if (bitmap != null) {
                    _clothingBitmap.value = bitmap
                    _selectedImages.value = _selectedImages.value.copy(clothingBitmap = bitmap)
                    android.util.Log.d(TAG, "Clothing image loaded from assets: ${bitmap.width}x${bitmap.height}")
                } else {
                    android.util.Log.e(TAG, "Failed to load clothing image from assets")
                    _tryOnState.value = TryOnState.Error(
                        message = "Failed to load clothing image from assets: $fileName",
                        retryable = false
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading clothing image from assets: ${e.message}", e)
                _tryOnState.value = TryOnState.Error(
                    message = "Error loading clothing image: ${e.message}",
                    exception = e,
                    retryable = false
                )
            }
        }
    }

    /**
     * Set avatar/user image from URI
     */
    fun setAvatarImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "Loading avatar image from URI: $uri")

                val bitmap = ImageUtils.loadBitmapFromUri(context, uri)

                if (bitmap != null) {
                    _avatarBitmap.value = bitmap
                    _selectedImages.value = _selectedImages.value.copy(
                        avatarUri = uri,
                        avatarBitmap = bitmap
                    )
                    android.util.Log.d(TAG, "Avatar image loaded: ${bitmap.width}x${bitmap.height}")
                } else {
                    android.util.Log.e(TAG, "Failed to load avatar image from URI")
                    _tryOnState.value = TryOnState.Error(
                        message = "Failed to load avatar image",
                        retryable = false
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading avatar image: ${e.message}", e)
                _tryOnState.value = TryOnState.Error(
                    message = "Error loading avatar image: ${e.message}",
                    exception = e,
                    retryable = false
                )
            }
        }
    }

    /**
     * Set avatar/user image from Bitmap
     */
    fun setAvatarImage(bitmap: Bitmap) {
        _avatarBitmap.value = bitmap
        _selectedImages.value = _selectedImages.value.copy(avatarBitmap = bitmap)
        android.util.Log.d(TAG, "Avatar bitmap set: ${bitmap.width}x${bitmap.height}")
    }

    /**
     * Process virtual try-on with current selected images
     */
    fun processTryOn(
        context: Context,
        clothingPrompt: String? = null,
        avatarSex: String? = null,
        avatarPrompt: String? = null
    ) {
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "===== STARTING TRY-ON PROCESS =====")

                // Validate that both images are selected
                val images = _selectedImages.value
                if (!images.isComplete()) {
                    android.util.Log.e(TAG, "Cannot process: Missing images")
                    _tryOnState.value = TryOnState.Error(
                        message = "Please select both clothing and avatar images",
                        retryable = false
                    )
                    return@launch
                }

                val clothingBitmap = images.clothingBitmap ?: _clothingBitmap.value
                val avatarBitmap = images.avatarBitmap ?: _avatarBitmap.value

                if (clothingBitmap == null || avatarBitmap == null) {
                    android.util.Log.e(TAG, "Cannot process: Images not loaded")
                    _tryOnState.value = TryOnState.Error(
                        message = "Images not properly loaded. Please try selecting again.",
                        retryable = false
                    )
                    return@launch
                }

                // Update state to loading
                _tryOnState.value = TryOnState.Loading("Preparing images...")

                android.util.Log.d(TAG, "Clothing bitmap: ${clothingBitmap.width}x${clothingBitmap.height}")
                android.util.Log.d(TAG, "Avatar bitmap: ${avatarBitmap.width}x${avatarBitmap.height}")

                // Update progress
                _tryOnState.value = TryOnState.Loading("Processing with AI... This may take 30-60 seconds")

                // Call repository to process try-on
                val result = repository.processTryOn(
                    context = context,
                    clothingBitmap = clothingBitmap,
                    avatarBitmap = avatarBitmap,
                    clothingPrompt = clothingPrompt,
                    avatarSex = avatarSex,
                    avatarPrompt = avatarPrompt
                )

                // Handle result
                when (result) {
                    is TryOnResult.Success -> {
                        android.util.Log.d(TAG, "Try-on successful!")
                        _tryOnState.value = TryOnState.Success(result.imageBitmap)
                    }
                    is TryOnResult.Failure -> {
                        android.util.Log.e(TAG, "Try-on failed: ${result.error.getUserMessage()}")
                        _tryOnState.value = TryOnState.Error(
                            message = result.error.getUserMessage(),
                            code = result.error.httpCode,
                            exception = result.error.exception,
                            retryable = result.error.isRetryable()
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Unexpected error in processTryOn: ${e.message}", e)
                _tryOnState.value = TryOnState.Error(
                    message = "Unexpected error: ${e.message}",
                    exception = e,
                    retryable = true
                )
            }
        }
    }

    /**
     * Retry the last try-on operation
     */
    fun retry(context: Context) {
        android.util.Log.d(TAG, "Retrying try-on...")
        processTryOn(context)
    }

    /**
     * Reset to idle state
     */
    fun resetState() {
        android.util.Log.d(TAG, "Resetting state to Idle")
        _tryOnState.value = TryOnState.Idle
    }

    /**
     * Clear all selected images and reset state
     */
    fun clearAll() {
        android.util.Log.d(TAG, "Clearing all images and state")
        _clothingBitmap.value?.recycle()
        _avatarBitmap.value?.recycle()
        _clothingBitmap.value = null
        _avatarBitmap.value = null
        _selectedImages.value = SelectedImages()
        _tryOnState.value = TryOnState.Idle
    }

    /**
     * Clear clothing image
     */
    fun clearClothingImage() {
        android.util.Log.d(TAG, "Clearing clothing image")
        _clothingBitmap.value?.recycle()
        _clothingBitmap.value = null
        _selectedImages.value = _selectedImages.value.copy(
            clothingUri = null,
            clothingBitmap = null
        )
    }

    /**
     * Clear avatar image
     */
    fun clearAvatarImage() {
        android.util.Log.d(TAG, "Clearing avatar image")
        _avatarBitmap.value?.recycle()
        _avatarBitmap.value = null
        _selectedImages.value = _selectedImages.value.copy(
            avatarUri = null,
            avatarBitmap = null
        )
    }

    /**
     * Check if ready to process try-on
     */
    fun isReadyToProcess(): Boolean {
        return _selectedImages.value.isComplete()
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up bitmaps when ViewModel is destroyed
        android.util.Log.d(TAG, "ViewModel cleared, recycling bitmaps")
        _clothingBitmap.value?.recycle()
        _avatarBitmap.value?.recycle()
    }
}

/**
 * Factory for creating TryOnViewModel with dependencies
 */
class TryOnViewModelFactory(
    private val repository: TryOnRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TryOnViewModel::class.java)) {
            return TryOnViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
