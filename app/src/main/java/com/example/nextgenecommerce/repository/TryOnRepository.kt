package com.example.nextgenecommerce.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.nextgenecommerce.api.TryOnDiffusionApiService
import com.example.nextgenecommerce.models.TryOnError
import com.example.nextgenecommerce.models.TryOnResult
import com.example.nextgenecommerce.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Repository for Try-On Diffusion API
 * Handles all API communication and error handling
 */
class TryOnRepository(
    private val apiService: TryOnDiffusionApiService,
    private val apiKey: String,
    private val apiHost: String = "try-on-diffusion.p.rapidapi.com"
) {
    private val TAG = "TryOnRepository"

    /**
     * Process try-on with two image files
     * This is a suspend function that should be called from a coroutine
     *
     * @param context Application context for file operations
     * @param clothingImageFile File containing clothing image
     * @param avatarImageFile File containing avatar/person image
     * @param clothingPrompt Optional text prompt describing the clothing
     * @param avatarSex Optional gender ("male" or "female")
     * @param avatarPrompt Optional text prompt describing the person
     * @return TryOnResult (Success with Bitmap or Failure with error details)
     */
    suspend fun processTryOn(
        context: Context,
        clothingImageFile: File,
        avatarImageFile: File,
        clothingPrompt: String? = null,
        avatarSex: String? = null,
        avatarPrompt: String? = null
    ): TryOnResult = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d(TAG, "===== STARTING TRY-ON PROCESS =====")
            android.util.Log.d(TAG, "Clothing image: ${clothingImageFile.absolutePath} (${clothingImageFile.length()} bytes)")
            android.util.Log.d(TAG, "Avatar image: ${avatarImageFile.absolutePath} (${avatarImageFile.length()} bytes)")

            // Validate image files
            val clothingValidation = ImageUtils.validateImageFile(clothingImageFile)
            if (!clothingValidation.first) {
                android.util.Log.e(TAG, "Clothing image validation failed: ${clothingValidation.second}")
                return@withContext TryOnResult.Failure(
                    TryOnError(
                        type = TryOnError.ErrorType.INVALID_IMAGE,
                        message = "Clothing image validation failed: ${clothingValidation.second}"
                    )
                )
            }

            val avatarValidation = ImageUtils.validateImageFile(avatarImageFile)
            if (!avatarValidation.first) {
                android.util.Log.e(TAG, "Avatar image validation failed: ${avatarValidation.second}")
                return@withContext TryOnResult.Failure(
                    TryOnError(
                        type = TryOnError.ErrorType.INVALID_IMAGE,
                        message = "Avatar image validation failed: ${avatarValidation.second}"
                    )
                )
            }

            // Create multipart body parts for images
            android.util.Log.d(TAG, "Creating multipart body parts...")
            val clothingPart = ImageUtils.fileToMultipartBodyPart(
                file = clothingImageFile,
                partName = "clothing_image",
                mimeType = ImageUtils.getMimeType(clothingImageFile.name)
            )

            val avatarPart = ImageUtils.fileToMultipartBodyPart(
                file = avatarImageFile,
                partName = "avatar_image",
                mimeType = ImageUtils.getMimeType(avatarImageFile.name)
            )

            // Log API request details
            android.util.Log.d(TAG, "===== API REQUEST =====")
            android.util.Log.d(TAG, "Host: $apiHost")
            android.util.Log.d(TAG, "API Key: ${apiKey.take(10)}...")
            android.util.Log.d(TAG, "Clothing prompt: ${clothingPrompt ?: "none"}")
            android.util.Log.d(TAG, "Avatar sex: ${avatarSex ?: "none"}")
            android.util.Log.d(TAG, "Avatar prompt: ${avatarPrompt ?: "none"}")

            // Make API call
            val response = apiService.tryOnFile(
                avatar_image = avatarPart,
                clothing_image = clothingPart,
                rapidApiHost = apiHost,
                rapidApiKey = apiKey
            )

            android.util.Log.d(TAG, "===== API RESPONSE =====")
            android.util.Log.d(TAG, "Status code: ${response.code()}")
            android.util.Log.d(TAG, "Is successful: ${response.isSuccessful}")

            // Handle response
            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                val imageBytes = responseBody.bytes()

                android.util.Log.d(TAG, "Response body size: ${imageBytes.size} bytes")

                // Decode image from response
                val resultBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                if (resultBitmap != null) {
                    android.util.Log.d(TAG, "Result image decoded: ${resultBitmap.width}x${resultBitmap.height}")
                    android.util.Log.d(TAG, "===== TRY-ON SUCCESSFUL =====")
                    TryOnResult.Success(resultBitmap)
                } else {
                    android.util.Log.e(TAG, "Failed to decode result bitmap")
                    TryOnResult.Failure(
                        TryOnError(
                            type = TryOnError.ErrorType.API_ERROR,
                            message = "Failed to decode result image from API response",
                            httpCode = response.code()
                        )
                    )
                }
            } else {
                // Handle API error
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                android.util.Log.e(TAG, "===== API ERROR =====")
                android.util.Log.e(TAG, "Status code: ${response.code()}")
                android.util.Log.e(TAG, "Error body: $errorBody")

                TryOnResult.Failure(
                    TryOnError(
                        type = TryOnError.ErrorType.API_ERROR,
                        message = errorBody,
                        httpCode = response.code()
                    )
                )
            }
        } catch (e: UnknownHostException) {
            android.util.Log.e(TAG, "Network error: ${e.message}", e)
            TryOnResult.Failure(
                TryOnError(
                    type = TryOnError.ErrorType.NETWORK_ERROR,
                    message = "No internet connection",
                    exception = e
                )
            )
        } catch (e: SocketTimeoutException) {
            android.util.Log.e(TAG, "Timeout error: ${e.message}", e)
            TryOnResult.Failure(
                TryOnError(
                    type = TryOnError.ErrorType.TIMEOUT_ERROR,
                    message = "Request timed out",
                    exception = e
                )
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Unexpected error: ${e.message}", e)
            TryOnResult.Failure(
                TryOnError(
                    type = TryOnError.ErrorType.UNKNOWN_ERROR,
                    message = e.message ?: "Unknown error occurred",
                    exception = e
                )
            )
        }
    }

    /**
     * Process try-on with Bitmaps
     * Converts bitmaps to files and calls the file-based method
     *
     * @param context Application context for file operations
     * @param clothingBitmap Bitmap of clothing image
     * @param avatarBitmap Bitmap of person image
     * @return TryOnResult (Success with Bitmap or Failure with error details)
     */
    suspend fun processTryOn(
        context: Context,
        clothingBitmap: Bitmap,
        avatarBitmap: Bitmap,
        clothingPrompt: String? = null,
        avatarSex: String? = null,
        avatarPrompt: String? = null
    ): TryOnResult = withContext(Dispatchers.IO) {
        var clothingFile: File? = null
        var avatarFile: File? = null

        try {
            android.util.Log.d(TAG, "Converting bitmaps to files...")

            // Convert bitmaps to files with compression
            clothingFile = ImageUtils.bitmapToFile(
                context = context,
                bitmap = clothingBitmap,
                fileName = "clothing_temp_${System.currentTimeMillis()}.jpg"
            )

            avatarFile = ImageUtils.bitmapToFile(
                context = context,
                bitmap = avatarBitmap,
                fileName = "avatar_temp_${System.currentTimeMillis()}.jpg"
            )

            android.util.Log.d(TAG, "Bitmaps converted to files successfully")

            // Call file-based method
            processTryOn(
                context = context,
                clothingImageFile = clothingFile,
                avatarImageFile = avatarFile,
                clothingPrompt = clothingPrompt,
                avatarSex = avatarSex,
                avatarPrompt = avatarPrompt
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error converting bitmaps: ${e.message}", e)
            TryOnResult.Failure(
                TryOnError(
                    type = TryOnError.ErrorType.UNKNOWN_ERROR,
                    message = "Failed to process images: ${e.message}",
                    exception = e
                )
            )
        } finally {
            // Clean up temporary files
            clothingFile?.let {
                if (it.exists()) {
                    it.delete()
                    android.util.Log.d(TAG, "Cleaned up clothing temp file")
                }
            }
            avatarFile?.let {
                if (it.exists()) {
                    it.delete()
                    android.util.Log.d(TAG, "Cleaned up avatar temp file")
                }
            }
        }
    }

    /**
     * Callback-based method for legacy support or when not using coroutines
     */
    fun processTryOnWithCallback(
        context: Context,
        clothingImageFile: File,
        avatarImageFile: File,
        onSuccess: (Bitmap) -> Unit,
        onError: (TryOnError) -> Unit,
        clothingPrompt: String? = null,
        avatarSex: String? = null,
        avatarPrompt: String? = null
    ) {
        // Note: This would require launching a coroutine scope
        // For simplicity, recommend using the suspend function with viewModelScope
        throw UnsupportedOperationException(
            "Please use the suspend function 'processTryOn' with coroutines. " +
            "Call it from viewModelScope in your ViewModel."
        )
    }
}
