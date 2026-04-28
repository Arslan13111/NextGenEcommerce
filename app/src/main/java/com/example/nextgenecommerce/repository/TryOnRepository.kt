package com.example.nextgenecommerce.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.nextgenecommerce.api.TryonaApiService
import com.example.nextgenecommerce.models.TryOnError
import com.example.nextgenecommerce.models.TryOnResult
import com.example.nextgenecommerce.util.ApiConstants
import com.example.nextgenecommerce.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/**
 * Repository for Tryona AI Try-On API
 * Handles image downloading, multipart construction, and API communication.
 */
class TryOnRepository(
    private val apiService: TryonaApiService
) {
    private val TAG = "TryOnRepository"

    /**
     * Process Try-On using Tryona Private API.
     *
     * @param context Application context
     * @param userBitmap The user's photo (avatar)
     * @param productImageUrl The product's image URL (garment)
     */
    suspend fun processTryOn(
        context: Context,
        userBitmap: Bitmap,
        productImageUrl: String
    ): TryOnResult = withContext(Dispatchers.IO) {
        var personFile: File? = null
        var garmentFile: File? = null

        try {
            android.util.Log.d(TAG, "Processing Try-On via Tryona API")
            android.util.Log.d(TAG, "Product URL: $productImageUrl")

            // Step A: Download the product image
            val productBitmap = downloadBitmap(productImageUrl) ?: return@withContext TryOnResult.Failure(
                TryOnError(TryOnError.ErrorType.INVALID_IMAGE, "Failed to download product image")
            )

            // Step B: Save both to temporary files
            val timestamp = System.currentTimeMillis()
            personFile = ImageUtils.bitmapToFile(context, userBitmap, "person_$timestamp.jpg")
            garmentFile = ImageUtils.bitmapToFile(context, productBitmap, "garment_$timestamp.jpg")

            // Step C: Convert to MultipartBody.Part
            val personPart = ImageUtils.fileToMultipartBodyPart(
                personFile, ApiConstants.Tryona.FIELD_PERSON_FILE
            )
            val garmentPart = ImageUtils.fileToMultipartBodyPart(
                garmentFile, ApiConstants.Tryona.FIELD_GARMENT_FILE
            )

            // Step D: Execute API call
            val response = apiService.tryOnSimple(
                person_file = personPart,
                garment_file = garmentPart,
                tokenapi = ApiConstants.Tryona.API_KEY
            )

            if (response.isSuccessful && response.body() != null) {
                // Step E: Parse JSON response and download result
                val responseString = response.body()?.string() ?: ""
                android.util.Log.d(TAG, "API Response: $responseString")
                
                val jsonObject = JSONObject(responseString)
                val tryonImageUrl = jsonObject.optString(ApiConstants.Tryona.RESPONSE_IMAGE_URL)

                if (!tryonImageUrl.isNullOrEmpty()) {
                    val resultBitmap = downloadBitmap(tryonImageUrl)
                    if (resultBitmap != null) {
                        TryOnResult.Success(resultBitmap)
                    } else {
                        TryOnResult.Failure(TryOnError(
                            type = TryOnError.ErrorType.API_ERROR,
                            message = "Failed to download result image from $tryonImageUrl"
                        ))
                    }
                } else {
                    TryOnResult.Failure(TryOnError(
                        type = TryOnError.ErrorType.API_ERROR,
                        message = "API response did not contain image URL"
                    ))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                android.util.Log.e(TAG, "Tryona API Error: $errorBody")
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
                message = "Request timed out. AI processing may take a while.",
                exception = e
            ))
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in processTryOn: ${e.message}", e)
            TryOnResult.Failure(TryOnError(
                type = TryOnError.ErrorType.UNKNOWN_ERROR,
                message = e.message ?: "Unknown error occurred",
                exception = e
            ))
        } finally {
            // Cleanup temp files
            personFile?.delete()
            garmentFile?.delete()
        }
    }

    /**
     * Download a bitmap from a URL.
     */
    suspend fun downloadBitmap(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
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
