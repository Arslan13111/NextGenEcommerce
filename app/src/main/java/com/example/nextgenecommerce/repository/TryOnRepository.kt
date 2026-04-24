package com.example.nextgenecommerce.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.nextgenecommerce.api.TryOnDiffusionApiService
import com.example.nextgenecommerce.data.remote.ApiService
import com.example.nextgenecommerce.data.remote.TryOnDiffusionRequest
import com.example.nextgenecommerce.models.TryOnError
import com.example.nextgenecommerce.models.TryOnResult
import com.example.nextgenecommerce.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException
/**
 * Repository for Try-On Diffusion API
 * Handles all API communication and error handling.
 * Provided via Hilt AppModule.
 *
 * Architecture:
 * - Product images come from products.image_url (Supabase Storage public URLs)
 * - User avatar comes from users.profile_image_url OR a freshly uploaded photo
 * - NO local asset/filename guessing - all URLs are authoritative from the database
 */
class TryOnRepository(
    private val apiService: TryOnDiffusionApiService,
    private val backendApiService: ApiService
) {
    private val TAG = "TryOnRepository"

    // RapidAPI credentials
    companion object {
        const val RAPID_API_KEY = "5c1a41631fmsh8e573a5fbb4c9f6p1fd101jsnfebe441d7708"
        const val RAPID_API_HOST = "try-on-diffusion.p.rapidapi.com"
    }

    /**
     * Process Try-On via Backend using product image URL and user avatar bitmap.
     *
     * Flow:
     * 1. Convert avatar bitmap to base64
     * 2. Send base64 avatar + product image URL to backend
     * 3. Backend uploads avatar to Supabase, calls RapidAPI with both URLs
     * 4. Return result bitmap
     *
     * @param context Application context
     * @param avatarBitmap The user's photo (from gallery, camera, or profile)
     * @param productImageUrl The product's image URL from products.image_url (Supabase Storage)
     * @param avatarSex "male" or "female"
     * @param clothingPrompt Optional clothing description
     */
    suspend fun processTryOnViaBackend(
        context: Context,
        avatarBitmap: Bitmap,
        productImageUrl: String,
        avatarSex: String = "female",
        clothingPrompt: String? = null,
        avatarProfileUrl: String? = null
    ): TryOnResult = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d(TAG, "Processing Try-On via Backend")
            android.util.Log.d(TAG, "Product URL: $productImageUrl")
            android.util.Log.d(TAG, "Avatar sex: $avatarSex")

            // Build request: use profile URL directly if available, otherwise convert to base64
            val request = if (!avatarProfileUrl.isNullOrEmpty()) {
                android.util.Log.d(TAG, "Using profile avatar URL: $avatarProfileUrl")
                TryOnDiffusionRequest(
                    avatarImageUrl = avatarProfileUrl,
                    productImageUrl = productImageUrl,
                    avatarSex = avatarSex,
                    clothingPrompt = clothingPrompt
                )
            } else {
                val avatarFile = ImageUtils.bitmapToFile(
                    context, avatarBitmap,
                    "avatar_tryon_${System.currentTimeMillis()}.jpg"
                )
                val userImageBase64 = android.util.Base64.encodeToString(
                    avatarFile.readBytes(), android.util.Base64.NO_WRAP
                )
                android.util.Log.d(TAG, "Using base64 avatar (${userImageBase64.length} chars)")
                TryOnDiffusionRequest(
                    userImage = userImageBase64,
                    productImageUrl = productImageUrl,
                    avatarSex = avatarSex,
                    clothingPrompt = clothingPrompt
                )
            }

            val response = backendApiService.tryOnDiffusion(request)

            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                if (result.success && !result.resultImage.isNullOrEmpty()) {
                    val imageUrl = result.resultImage
                    android.util.Log.d(TAG, "Try-On Success. Image URL: $imageUrl")

                    // Download the result image
                    val url = java.net.URL(imageUrl)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.doInput = true
                    connection.connect()
                    val inputStream = connection.inputStream
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()

                    if (bitmap != null) {
                        TryOnResult.Success(bitmap)
                    } else {
                        TryOnResult.Failure(TryOnError(
                            type = TryOnError.ErrorType.API_ERROR,
                            message = "Failed to decode result image"
                        ))
                    }
                } else {
                    TryOnResult.Failure(TryOnError(
                        type = TryOnError.ErrorType.API_ERROR,
                        message = result.message ?: "Unknown API error"
                    ))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown network error"
                android.util.Log.e(TAG, "Backend API Error: $errorBody")
                TryOnResult.Failure(TryOnError(
                    type = TryOnError.ErrorType.NETWORK_ERROR,
                    message = errorBody
                ))
            }
        } catch (e: UnknownHostException) {
            android.util.Log.e(TAG, "Network error: ${e.message}", e)
            TryOnResult.Failure(TryOnError(
                type = TryOnError.ErrorType.NETWORK_ERROR,
                message = "No internet connection",
                exception = e
            ))
        } catch (e: SocketTimeoutException) {
            android.util.Log.e(TAG, "Timeout error: ${e.message}", e)
            TryOnResult.Failure(TryOnError(
                type = TryOnError.ErrorType.TIMEOUT_ERROR,
                message = "Request timed out. AI processing may take up to 60 seconds.",
                exception = e
            ))
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Exception in processTryOnViaBackend: ${e.message}", e)
            TryOnResult.Failure(TryOnError(
                type = TryOnError.ErrorType.UNKNOWN_ERROR,
                message = e.message ?: "Unknown error",
                exception = e
            ))
        }
    }

    /**
     * Process Try-On directly via RapidAPI using file uploads.
     * This bypasses the backend and sends images directly to RapidAPI.
     *
     * @param context Application context
     * @param clothingBitmap Product image bitmap (downloaded from products.image_url)
     * @param avatarBitmap User's photo bitmap
     * @param clothingPrompt Optional clothing description
     * @param avatarSex "male" or "female"
     */
    suspend fun processTryOnDirect(
        context: Context,
        clothingBitmap: Bitmap,
        avatarBitmap: Bitmap,
        clothingPrompt: String? = null,
        avatarSex: String? = null
    ): TryOnResult = withContext(Dispatchers.IO) {
        var clothingFile: File? = null
        var avatarFile: File? = null

        try {
            android.util.Log.d(TAG, "Processing Try-On directly via RapidAPI")

            // Convert bitmaps to files
            clothingFile = ImageUtils.bitmapToFile(
                context, clothingBitmap,
                "clothing_temp_${System.currentTimeMillis()}.jpg"
            )
            avatarFile = ImageUtils.bitmapToFile(
                context, avatarBitmap,
                "avatar_temp_${System.currentTimeMillis()}.jpg"
            )

            // Validate files
            val clothingValidation = ImageUtils.validateImageFile(clothingFile)
            if (!clothingValidation.first) {
                return@withContext TryOnResult.Failure(TryOnError(
                    type = TryOnError.ErrorType.INVALID_IMAGE,
                    message = "Clothing image validation failed: ${clothingValidation.second}"
                ))
            }

            val avatarValidation = ImageUtils.validateImageFile(avatarFile)
            if (!avatarValidation.first) {
                return@withContext TryOnResult.Failure(TryOnError(
                    type = TryOnError.ErrorType.INVALID_IMAGE,
                    message = "Avatar image validation failed: ${avatarValidation.second}"
                ))
            }

            // Create multipart parts
            val clothingPart = ImageUtils.fileToMultipartBodyPart(
                file = clothingFile,
                partName = "clothing_image",
                mimeType = ImageUtils.getMimeType(clothingFile.name)
            )
            val avatarPart = ImageUtils.fileToMultipartBodyPart(
                file = avatarFile,
                partName = "avatar_image",
                mimeType = ImageUtils.getMimeType(avatarFile.name)
            )

            // Call RapidAPI directly
            val response = apiService.tryOnFile(
                avatar_image = avatarPart,
                clothing_image = clothingPart,
                rapidApiHost = RAPID_API_HOST,
                rapidApiKey = RAPID_API_KEY
            )

            if (response.isSuccessful && response.body() != null) {
                val responseBody = response.body()!!
                val imageBytes = responseBody.bytes()
                val resultBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                if (resultBitmap != null) {
                    android.util.Log.d(TAG, "Direct Try-On successful: ${resultBitmap.width}x${resultBitmap.height}")
                    TryOnResult.Success(resultBitmap)
                } else {
                    TryOnResult.Failure(TryOnError(
                        type = TryOnError.ErrorType.API_ERROR,
                        message = "Failed to decode result image",
                        httpCode = response.code()
                    ))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                android.util.Log.e(TAG, "RapidAPI Error: $errorBody")
                TryOnResult.Failure(TryOnError(
                    type = TryOnError.ErrorType.API_ERROR,
                    message = errorBody,
                    httpCode = response.code()
                ))
            }
        } catch (e: UnknownHostException) {
            TryOnResult.Failure(TryOnError(
                type = TryOnError.ErrorType.NETWORK_ERROR,
                message = "No internet connection",
                exception = e
            ))
        } catch (e: SocketTimeoutException) {
            TryOnResult.Failure(TryOnError(
                type = TryOnError.ErrorType.TIMEOUT_ERROR,
                message = "Request timed out",
                exception = e
            ))
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in processTryOnDirect: ${e.message}", e)
            TryOnResult.Failure(TryOnError(
                type = TryOnError.ErrorType.UNKNOWN_ERROR,
                message = e.message ?: "Unknown error occurred",
                exception = e
            ))
        } finally {
            clothingFile?.let { if (it.exists()) it.delete() }
            avatarFile?.let { if (it.exists()) it.delete() }
        }
    }

    /**
     * Download a bitmap from a URL.
     * Used to download product images from Supabase Storage or user profile images.
     */
    suspend fun downloadBitmap(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL(imageUrl)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()
            val inputStream = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            bitmap
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to download bitmap from $imageUrl: ${e.message}")
            null
        }
    }
}
