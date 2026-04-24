package com.example.nextgenecommerce.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nextgenecommerce.models.TryOnResult
import com.example.nextgenecommerce.models.TryOnState
import com.example.nextgenecommerce.repository.TryOnRepository
import com.example.nextgenecommerce.util.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Try-On Feature.
 *
 * Architecture:
 * - Product image URL comes from products.image_url (Supabase)
 * - Avatar image comes from users.profile_image_url OR a freshly uploaded/captured photo
 * - NO local assets, NO filename guessing
 */
@HiltViewModel
class TryOnViewModel @Inject constructor(
    private val repository: TryOnRepository
) : ViewModel() {

    private val TAG = "TryOnViewModel"

    // Try-on processing state
    private val _tryOnState = MutableStateFlow<TryOnState>(TryOnState.Idle)
    val tryOnState: StateFlow<TryOnState> = _tryOnState.asStateFlow()

    // Avatar image (user photo) - from gallery, camera, or profile
    private val _avatarBitmap = MutableStateFlow<Bitmap?>(null)
    val avatarBitmap: StateFlow<Bitmap?> = _avatarBitmap.asStateFlow()

    // Avatar source label for UI
    private val _avatarSource = MutableStateFlow<String?>(null)
    val avatarSource: StateFlow<String?> = _avatarSource.asStateFlow()

    // Profile image URL - stored when avatar source is the user's profile
    private val _avatarProfileUrl = MutableStateFlow<String?>(null)

    // Product image URL (from products.image_url in Supabase)
    private val _productImageUrl = MutableStateFlow<String?>(null)
    val productImageUrl: StateFlow<String?> = _productImageUrl.asStateFlow()

    // Result bitmap
    private val _resultBitmap = MutableStateFlow<Bitmap?>(null)
    val resultBitmap: StateFlow<Bitmap?> = _resultBitmap.asStateFlow()

    // Last try-on parameters for retry
    private var lastAvatarSex: String = "female"
    private var lastClothingPrompt: String? = null

    /**
     * Set the product image URL from products.image_url.
     * This is the ONLY source for product images - no local assets.
     */
    fun setProductImageUrl(url: String) {
        _productImageUrl.value = url
    }

    /**
     * Set avatar from a gallery/camera URI.
     */
    fun setAvatarFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val bitmap = ImageUtils.loadBitmapFromUri(context, uri)
                if (bitmap != null) {
                    _avatarBitmap.value = bitmap
                    _avatarSource.value = "Uploaded Photo"
                    _avatarProfileUrl.value = null // Not from profile
                } else {
                    _tryOnState.value = TryOnState.Error(
                        message = "Failed to load photo. Please try again.",
                        retryable = false
                    )
                }
            } catch (e: Exception) {
                _tryOnState.value = TryOnState.Error(
                    message = "Error loading photo: ${e.message}",
                    exception = e,
                    retryable = false
                )
            }
        }
    }

    /**
     * Set avatar from a captured camera bitmap.
     */
    fun setAvatarFromBitmap(bitmap: Bitmap) {
        _avatarBitmap.value = bitmap
        _avatarSource.value = "Camera Capture"
        _avatarProfileUrl.value = null // Not from profile
    }

    /**
     * Set avatar from the user's profile image URL.
     * Downloads the image from users.profile_image_url.
     */
    fun setAvatarFromProfileUrl(profileImageUrl: String) {
        viewModelScope.launch {
            try {
                _tryOnState.value = TryOnState.Loading("Loading your profile photo...")
                val bitmap = repository.downloadBitmap(profileImageUrl)
                if (bitmap != null) {
                    _avatarBitmap.value = bitmap
                    _avatarSource.value = "Profile Photo"
                    _avatarProfileUrl.value = profileImageUrl // Store for direct URL pass-through
                    _tryOnState.value = TryOnState.Idle
                } else {
                    _tryOnState.value = TryOnState.Error(
                        message = "Failed to load profile photo. Please upload a different photo.",
                        retryable = false
                    )
                }
            } catch (e: Exception) {
                _tryOnState.value = TryOnState.Error(
                    message = "Error loading profile photo: ${e.message}",
                    exception = e,
                    retryable = false
                )
            }
        }
    }

    /**
     * Process try-on using the current avatar bitmap and product image URL.
     * Sends the product's Supabase URL directly - no local image mapping.
     *
     * @param context Application context
     * @param avatarSex "male" or "female"
     * @param clothingPrompt Optional clothing description
     */
    fun processTryOn(
        context: Context,
        avatarSex: String = "female",
        clothingPrompt: String? = null
    ) {
        viewModelScope.launch {
            try {
                val avatar = _avatarBitmap.value
                val productUrl = _productImageUrl.value

                if (avatar == null) {
                    _tryOnState.value = TryOnState.Error(
                        message = "Please select your photo first.",
                        retryable = false
                    )
                    return@launch
                }

                if (productUrl.isNullOrEmpty()) {
                    _tryOnState.value = TryOnState.Error(
                        message = "Product image not available. Please go back and try again.",
                        retryable = false
                    )
                    return@launch
                }

                // Save for retry
                lastAvatarSex = avatarSex
                lastClothingPrompt = clothingPrompt

                _tryOnState.value = TryOnState.Loading("Processing with AI... This may take 30-60 seconds")
                _resultBitmap.value = null

                val productBitmap = repository.downloadBitmap(productUrl)
                if (productBitmap == null) {
                    _tryOnState.value = TryOnState.Error(
                        message = "Failed to download product image. Please try again.",
                        retryable = true
                    )
                    return@launch
                }

                // Use direct RapidAPI call
                val result = repository.processTryOnDirect(
                    context = context,
                    clothingBitmap = productBitmap,
                    avatarBitmap = avatar,
                    avatarSex = avatarSex,
                    clothingPrompt = clothingPrompt
                )

                when (result) {
                    is TryOnResult.Success -> {
                        _resultBitmap.value = result.imageBitmap
                        _tryOnState.value = TryOnState.Success(result.imageBitmap)
                    }
                    is TryOnResult.Failure -> {
                        _tryOnState.value = TryOnState.Error(
                            message = result.error.getUserMessage(),
                            code = result.error.httpCode,
                            exception = result.error.exception,
                            retryable = result.error.isRetryable()
                        )
                    }
                }
            } catch (e: Exception) {
                _tryOnState.value = TryOnState.Error(
                    message = "Unexpected error: ${e.message}",
                    exception = e,
                    retryable = true
                )
            }
        }
    }

    /**
     * Retry the last try-on operation.
     */
    fun retry(context: Context) {
        processTryOn(context, lastAvatarSex, lastClothingPrompt)
    }

    /**
     * Reset to idle state.
     */
    fun resetState() {
        _tryOnState.value = TryOnState.Idle
    }

    /**
     * Clear avatar and reset.
     */
    fun clearAvatar() {
        _avatarBitmap.value?.recycle()
        _avatarBitmap.value = null
        _avatarSource.value = null
        _avatarProfileUrl.value = null
        _resultBitmap.value = null
        _tryOnState.value = TryOnState.Idle
    }

    /**
     * Clear everything and reset to initial state.
     */
    fun clearAll() {
        _avatarBitmap.value?.recycle()
        _avatarBitmap.value = null
        _avatarSource.value = null
        _avatarProfileUrl.value = null
        _resultBitmap.value?.recycle()
        _resultBitmap.value = null
        _productImageUrl.value = null
        _tryOnState.value = TryOnState.Idle
    }

    /**
     * Check if ready to process (has both avatar and product URL).
     */
    fun isReadyToProcess(): Boolean {
        return _avatarBitmap.value != null && !_productImageUrl.value.isNullOrEmpty()
    }

    override fun onCleared() {
        super.onCleared()
        _avatarBitmap.value?.recycle()
        _resultBitmap.value?.recycle()
    }
}
