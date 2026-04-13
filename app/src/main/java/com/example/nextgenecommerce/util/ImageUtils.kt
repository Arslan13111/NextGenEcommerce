package com.example.nextgenecommerce.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Comprehensive Image Utilities for Try-On Feature
 * Handles image validation, compression, conversion, and cleanup
 */
object ImageUtils {

    private const val TAG = "ImageUtils"

    // Image constraints
    private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 // 10MB
    private const val TARGET_FILE_SIZE_BYTES = 2 * 1024 * 1024 // 2MB for compression
    private const val MAX_DIMENSION = 2048
    private const val INITIAL_QUALITY = 90
    private const val MIN_QUALITY = 60

    /**
     * Validates image file
     * @return true if valid, false otherwise
     */
    fun validateImageFile(file: File): Pair<Boolean, String?> {
        // Check if file exists
        if (!file.exists()) {
            return Pair(false, "File does not exist")
        }

        // Check file size
        if (file.length() > MAX_FILE_SIZE_BYTES) {
            return Pair(false, "File size exceeds 10MB limit (${file.length() / 1024 / 1024}MB)")
        }

        // Check if it's a valid image format
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)

        if (options.outWidth == -1 || options.outHeight == -1) {
            return Pair(false, "Invalid image format")
        }

        // Check mime type
        val mimeType = options.outMimeType
        if (mimeType != "image/jpeg" && mimeType != "image/png" && mimeType != "image/webp") {
            return Pair(false, "Unsupported format: $mimeType. Only JPEG, PNG, and WebP are supported")
        }

        return Pair(true, null)
    }

    /**
     * Convert Android Uri to File
     * Handles different URI sources (gallery, camera, file provider)
     */
    fun uriToFile(context: Context, uri: Uri, fileName: String): File? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, fileName)

            inputStream?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            android.util.Log.d(TAG, "Converted URI to file: ${file.absolutePath}, size: ${file.length()} bytes")
            file
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error converting URI to file: ${e.message}", e)
            null
        }
    }

    /**
     * Convert Bitmap to File with compression
     */
    fun bitmapToFile(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        targetSizeBytes: Long = TARGET_FILE_SIZE_BYTES.toLong()
    ): File {
        val file = File(context.cacheDir, fileName)
        var quality = INITIAL_QUALITY

        // Resize bitmap if dimensions are too large
        val resizedBitmap = if (bitmap.width > MAX_DIMENSION || bitmap.height > MAX_DIMENSION) {
            resizeBitmap(bitmap, MAX_DIMENSION)
        } else {
            bitmap
        }

        // Compress with decreasing quality until target size is reached
        do {
            FileOutputStream(file).use { outputStream ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            }

            if (file.length() <= targetSizeBytes || quality <= MIN_QUALITY) {
                break
            }

            quality -= 10
            android.util.Log.d(TAG, "Compressing image with quality $quality, current size: ${file.length()} bytes")
        } while (file.length() > targetSizeBytes && quality >= MIN_QUALITY)

        android.util.Log.d(TAG, "Final file size: ${file.length()} bytes, quality: $quality")

        // Cleanup resized bitmap if it's different from original
        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }

        return file
    }

    /**
     * Resize bitmap to fit within max dimensions while maintaining aspect ratio
     */
    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val scale = if (width > height) {
            maxDimension.toFloat() / width
        } else {
            maxDimension.toFloat() / height
        }

        if (scale >= 1.0f) {
            return bitmap // No need to resize
        }

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        android.util.Log.d(TAG, "Resizing bitmap from ${width}x${height} to ${newWidth}x${newHeight}")

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Correct image orientation based on EXIF data
     */
    fun correctImageOrientation(file: File): Bitmap? {
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null

            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }

            if (!matrix.isIdentity) {
                val correctedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )
                bitmap.recycle()
                return correctedBitmap
            }

            return bitmap
        } catch (e: IOException) {
            android.util.Log.e(TAG, "Error correcting image orientation: ${e.message}", e)
            null
        }
    }

    /**
     * Load bitmap from Uri with proper orientation correction
     */
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            var bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)

            // Try to correct orientation if possible
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                try {
                    val exif = ExifInterface(inputStream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )

                    val matrix = Matrix()
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    }

                    if (!matrix.isIdentity) {
                        val correctedBitmap = Bitmap.createBitmap(
                            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                        )
                        bitmap.recycle()
                        bitmap = correctedBitmap
                    }
                    Unit // Explicitly return Unit from try block
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "Could not read EXIF data from URI", e)
                }
            }

            bitmap
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error loading bitmap from URI: ${e.message}", e)
            null
        }
    }

    /**
     * Load bitmap from assets folder with error handling
     */
    fun loadBitmapFromAssets(context: Context, fileName: String, subFolder: String = "products"): Bitmap? {
        return try {
            // List all files in the folder for debugging
            val files = context.assets.list(subFolder)
            android.util.Log.d(TAG, "Files in assets/$subFolder: ${files?.joinToString()}")
            android.util.Log.d(TAG, "Looking for: $fileName")

            // Try to find the file (case-insensitive)
            val actualFileName = files?.find {
                it.equals(fileName, ignoreCase = true) ||
                it.startsWith(fileName.substringBeforeLast("."), ignoreCase = true)
            } ?: fileName

            android.util.Log.d(TAG, "Attempting to load: $actualFileName")

            val inputStream = context.assets.open("$subFolder/$actualFileName")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap != null) {
                android.util.Log.d(TAG, "Successfully loaded: $actualFileName (${bitmap.width}x${bitmap.height})")
            } else {
                android.util.Log.e(TAG, "Bitmap is null for: $actualFileName")
            }

            bitmap
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error loading image from assets: ${e.message}", e)
            null
        }
    }

    /**
     * Convert File to MultipartBody.Part for API upload
     * CRITICAL: Does NOT manually set Content-Type header - OkHttp handles this
     */
    fun fileToMultipartBodyPart(
        file: File,
        partName: String,
        mimeType: String = "image/jpeg"
    ): MultipartBody.Part {
        android.util.Log.d(TAG, "Creating multipart for: $partName")
        android.util.Log.d(TAG, "File: ${file.absolutePath}, size: ${file.length()} bytes")

        // Create RequestBody from file with proper MIME type
        val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())

        // Create MultipartBody.Part
        // OkHttp will automatically set the correct Content-Type header
        val multipartPart = MultipartBody.Part.createFormData(partName, file.name, requestBody)

        android.util.Log.d(TAG, "Multipart created: field=$partName, filename=${file.name}")

        return multipartPart
    }

    /**
     * Convert Bitmap to MultipartBody.Part (legacy support)
     */
    fun bitmapToMultipartBodyPart(
        context: Context,
        bitmap: Bitmap,
        partName: String,
        fileName: String
    ): MultipartBody.Part {
        val file = bitmapToFile(context, bitmap, fileName)
        return fileToMultipartBodyPart(file, partName)
    }

    /**
     * Clean up temporary files
     */
    fun cleanupTempFiles(context: Context, vararg fileNames: String) {
        fileNames.forEach { fileName ->
            try {
                val file = File(context.cacheDir, fileName)
                if (file.exists()) {
                    val deleted = file.delete()
                    android.util.Log.d(TAG, "Cleanup: $fileName - ${if (deleted) "deleted" else "failed"}")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error deleting temp file $fileName: ${e.message}")
            }
        }
    }

    /**
     * Clean up all temporary image files in cache
     */
    fun cleanupAllTempImages(context: Context) {
        try {
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".jpg") ||
                    file.name.endsWith(".jpeg") ||
                    file.name.endsWith(".png") ||
                    file.name.endsWith(".webp")) {
                    file.delete()
                    android.util.Log.d(TAG, "Cleaned up: ${file.name}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error cleaning up temp images: ${e.message}")
        }
    }

    /**
     * Get MIME type from file extension
     */
    fun getMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.').lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg" // Default
        }
    }
}
