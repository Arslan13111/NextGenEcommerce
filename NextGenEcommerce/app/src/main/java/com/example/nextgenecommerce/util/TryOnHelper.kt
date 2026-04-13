package com.example.nextgenecommerce.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

object TryOnHelper {

    /**
     * Convert Bitmap to MultipartBody.Part for API upload
     */
    fun bitmapToMultipartBodyPart(
        bitmap: Bitmap,
        partName: String,
        fileName: String,
        context: Context
    ): MultipartBody.Part {
        android.util.Log.d("TryOnHelper", "Creating multipart for: $partName")
        android.util.Log.d("TryOnHelper", "Bitmap size: ${bitmap.width}x${bitmap.height}")

        // Create a temporary file
        val file = File(context.cacheDir, fileName)
        android.util.Log.d("TryOnHelper", "Temp file path: ${file.absolutePath}")

        val outputStream = FileOutputStream(file)

        // Compress and save bitmap to file
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.flush()
        outputStream.close()

        android.util.Log.d("TryOnHelper", "Temp file created, size: ${file.length()} bytes")

        // Create RequestBody from file
        val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())

        // Create MultipartBody.Part
        val multipartPart = MultipartBody.Part.createFormData(partName, file.name, requestBody)
        android.util.Log.d("TryOnHelper", "Multipart created with field name: $partName, filename: ${file.name}")

        return multipartPart
    }

    /**
     * Load bitmap from assets folder
     */
    fun loadBitmapFromAssets(context: Context, fileName: String): Bitmap? {
        return try {
            val productFiles = context.assets.list("products")
            android.util.Log.d("TryOnHelper", "Files in assets/products: ${productFiles?.joinToString()}")
            android.util.Log.d("TryOnHelper", "Looking for: $fileName")

            val actualFileName = productFiles?.find {
                it.equals(fileName, ignoreCase = true) ||
                        it.startsWith(fileName.substringBeforeLast("."), ignoreCase = true)
            } ?: fileName

            android.util.Log.d("TryOnHelper", "Attempting to load: $actualFileName")

            val inputStream = context.assets.open("products/$actualFileName")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap != null) {
                android.util.Log.d("TryOnHelper", "Successfully loaded image: $actualFileName")
            } else {
                android.util.Log.e("TryOnHelper", "Bitmap is null for: $actualFileName")
            }

            bitmap
        } catch (e: Exception) {
            android.util.Log.e("TryOnHelper", "Error loading image: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Clean up temporary files created during the process
     */
    fun cleanupTempFiles(context: Context, vararg fileNames: String) {
        fileNames.forEach { fileName ->
            try {
                val file = File(context.cacheDir, fileName)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                android.util.Log.e("TryOnHelper", "Error deleting temp file: ${e.message}")
            }
        }
    }
}
