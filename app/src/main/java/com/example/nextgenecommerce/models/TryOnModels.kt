package com.example.nextgenecommerce.models

import android.graphics.Bitmap
import android.net.Uri

/**
 * Sealed class representing the state of the Try-On process
 * Used with StateFlow for reactive UI updates
 */
sealed class TryOnState {
    /**
     * Initial state - no operation in progress
     */
    object Idle : TryOnState()

    /**
     * Loading state with optional progress message
     * @param message User-friendly message describing current step
     * @param progress Optional progress percentage (0-100)
     */
    data class Loading(
        val message: String = "Processing...",
        val progress: Int? = null
    ) : TryOnState()

    /**
     * Success state with result image
     * @param resultBitmap The generated try-on result image
     */
    data class Success(
        val resultBitmap: Bitmap
    ) : TryOnState()

    /**
     * Error state with error details
     * @param message User-friendly error message
     * @param code Optional HTTP error code
     * @param exception Optional exception for logging
     * @param retryable Whether the operation can be retried
     */
    data class Error(
        val message: String,
        val code: Int? = null,
        val exception: Throwable? = null,
        val retryable: Boolean = true
    ) : TryOnState()
}

/**
 * Data class holding selected images for try-on
 */
data class SelectedImages(
    val clothingUri: Uri? = null,
    val clothingBitmap: Bitmap? = null,
    val avatarUri: Uri? = null,
    val avatarBitmap: Bitmap? = null
) {
    /**
     * Check if both images are selected
     */
    fun isComplete(): Boolean {
        return (clothingUri != null || clothingBitmap != null) &&
                (avatarUri != null || avatarBitmap != null)
    }

    /**
     * Check if clothing image is selected
     */
    fun hasClothingImage(): Boolean {
        return clothingUri != null || clothingBitmap != null
    }

    /**
     * Check if avatar image is selected
     */
    fun hasAvatarImage(): Boolean {
        return avatarUri != null || avatarBitmap != null
    }
}

/**
 * Request parameters for Try-On API
 */
data class TryOnRequest(
    val clothingImagePath: String,
    val avatarImagePath: String,
    val clothingPrompt: String? = null,
    val avatarSex: String? = null, // "male" or "female"
    val avatarPrompt: String? = null
)

/**
 * Response wrapper for Try-On API results
 */
sealed class TryOnResult {
    /**
     * Successful API response
     * @param imageBitmap The resulting try-on image
     */
    data class Success(val imageBitmap: Bitmap) : TryOnResult()

    /**
     * API error response
     * @param error The error details
     */
    data class Failure(val error: TryOnError) : TryOnResult()
}

/**
 * Detailed error information for Try-On operations
 */
data class TryOnError(
    val type: ErrorType,
    val message: String,
    val httpCode: Int? = null,
    val exception: Throwable? = null
) {
    enum class ErrorType {
        NETWORK_ERROR,          // No internet connection
        TIMEOUT_ERROR,          // Request timeout
        API_ERROR,              // API returned error response
        INVALID_IMAGE,          // Image validation failed
        FILE_NOT_FOUND,         // Image file not found
        PERMISSION_DENIED,      // Missing required permissions
        UNKNOWN_ERROR           // Unexpected error
    }

    /**
     * Get user-friendly error message
     */
    fun getUserMessage(): String {
        return when (type) {
            ErrorType.NETWORK_ERROR -> "No internet connection. Please check your network and try again."
            ErrorType.TIMEOUT_ERROR -> "Request timed out. The service might be busy, please try again."
            ErrorType.API_ERROR -> when (httpCode) {
                400 -> "Bad request. Please ensure images are valid."
                401 -> "Unauthorized. Please check your API credentials."
                403 -> "Access forbidden. Your API subscription may have expired."
                429 -> "Rate limit exceeded. Please try again later."
                500, 502, 503 -> "Server error. Please try again later."
                else -> message
            }
            ErrorType.INVALID_IMAGE -> "Invalid image format. Please use JPEG, PNG, or WebP images."
            ErrorType.FILE_NOT_FOUND -> "Image file not found. Please select images again."
            ErrorType.PERMISSION_DENIED -> "Storage permission required. Please grant permission in settings."
            ErrorType.UNKNOWN_ERROR -> "An unexpected error occurred: $message"
        }
    }

    /**
     * Check if error is retryable
     */
    fun isRetryable(): Boolean {
        return when (type) {
            ErrorType.NETWORK_ERROR,
            ErrorType.TIMEOUT_ERROR -> true
            ErrorType.API_ERROR -> httpCode in listOf(429, 500, 502, 503)
            else -> false
        }
    }
}

/**
 * Image selection type
 */
enum class ImageSelectionType {
    CLOTHING,
    AVATAR
}

/**
 * Image source type
 */
enum class ImageSource {
    GALLERY,
    CAMERA,
    ASSETS,
    FILE
}

/**
 * API configuration constants
 */
object TryOnConstants {
    const val API_TIMEOUT_SECONDS = 90L
    const val MAX_IMAGE_SIZE_MB = 10
    const val TARGET_IMAGE_SIZE_MB = 2
    const val MAX_IMAGE_DIMENSION = 2048

    // Supported image formats
    val SUPPORTED_FORMATS = listOf("image/jpeg", "image/png", "image/webp")

    // API field names (must match API documentation)
    const val FIELD_CLOTHING_IMAGE = "clothing_image"
    const val FIELD_AVATAR_IMAGE = "avatar_image"
    const val FIELD_CLOTHING_PROMPT = "clothing_prompt"
    const val FIELD_AVATAR_SEX = "avatar_sex"
    const val FIELD_AVATAR_PROMPT = "avatar_prompt"
}
