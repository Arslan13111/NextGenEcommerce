package com.example.nextgenecommerce.data.repository

import android.content.Context
import android.net.Uri
import com.example.nextgenecommerce.util.Resource
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.upload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val storage: Storage,
    private val context: Context
) {

    private val PROFILE_BUCKET = "profiles"
    private val PRODUCTS_BUCKET = "products"

    /**
     * Upload a profile image to Supabase Storage
     * @param userId The user ID to organize files by user
     * @param imageUri The local URI of the image to upload
     * @return Flow with the public URL of the uploaded image
     */
    suspend fun uploadProfileImage(userId: String, imageUri: Uri): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            // Read file bytes
            val bytes = context.contentResolver.openInputStream(imageUri)?.readBytes()
                ?: throw Exception("Unable to read image file")

            // Create a unique filename
            val filename = "profile_${userId}_${System.currentTimeMillis()}.jpg"
            val path = "$userId/$filename"

            // Upload to Supabase Storage
            val bucket = storage.from(PROFILE_BUCKET)
            bucket.upload(path, bytes, upsert = true)

            // Get public URL
            val publicUrl = bucket.publicUrl(path)

            emit(Resource.Success(publicUrl))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to upload image"))
        }
    }

    /**
     * Delete a profile image from Supabase Storage
     * @param userId The user ID
     * @param filename The filename to delete
     */
    suspend fun deleteProfileImage(userId: String, filename: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val path = "$userId/$filename"
            val bucket = storage.from(PROFILE_BUCKET)
            bucket.delete(path)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to delete image"))
        }
    }

    /**
     * Upload any image to Supabase Storage (generic)
     * @param bucketName The storage bucket name (e.g., "products", "banners")
     * @param imageUri The local URI of the image to upload
     * @param folder Optional folder path within the bucket
     * @return Flow with the public URL of the uploaded image
     */
    suspend fun uploadImage(
        bucketName: String,
        imageUri: Uri,
        folder: String = ""
    ): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            // Read file bytes
            val bytes = context.contentResolver.openInputStream(imageUri)?.readBytes()
                ?: throw Exception("Unable to read image file")

            // Create a unique filename
            val filename = "${UUID.randomUUID()}.jpg"
            val path = if (folder.isNotEmpty()) "$folder/$filename" else filename

            // Upload to Supabase Storage
            val bucket = storage.from(bucketName)
            bucket.upload(path, bytes, upsert = true)

            // Get public URL
            val publicUrl = bucket.publicUrl(path)

            emit(Resource.Success(publicUrl))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to upload image"))
        }
    }

    /**
     * Get public URL for an existing file
     * @param bucketName The storage bucket name
     * @param path The file path within the bucket
     * @return The public URL
     */
    fun getPublicUrl(bucketName: String, path: String): String {
        return storage.from(bucketName).publicUrl(path)
    }

    /**
     * Delete a file from storage
     * @param bucketName The storage bucket name
     * @param path The file path to delete
     */
    suspend fun deleteFile(bucketName: String, path: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val bucket = storage.from(bucketName)
            bucket.delete(path)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to delete file"))
        }
    }

    /**
     * List files in a folder
     * @param bucketName The storage bucket name
     * @param folder The folder path
     */
    suspend fun listFiles(bucketName: String, folder: String = ""): Flow<Resource<List<String>>> = flow {
        emit(Resource.Loading())
        try {
            val bucket = storage.from(bucketName)
            val files = bucket.list(folder)
            val filePaths = files.map { it.name }
            emit(Resource.Success(filePaths))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to list files"))
        }
    }
}
