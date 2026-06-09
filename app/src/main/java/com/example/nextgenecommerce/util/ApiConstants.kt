package com.example.nextgenecommerce.util

import com.example.nextgenecommerce.BuildConfig

/**
 * API Configuration Constants
 *
 * IMPORTANT SECURITY NOTE:
 * This file contains API keys for demonstration purposes.
 * In production, you should:
 * 1. Store API keys in local.properties (added to .gitignore)
 * 2. Access them via BuildConfig
 * 3. Use environment variables for CI/CD
 * 4. Consider using Android Keystore for sensitive data
 *
 * Example for production:
 * 1. Add to local.properties:
 *    TRYONA_API_KEY=your_actual_api_key_here
 *
 * 2. Add to build.gradle.kts:
 *    android {
 *        ...
 *        defaultConfig {
 *            ...
 *            val properties = Properties()
 *            properties.load(project.rootProject.file("local.properties").inputStream())
 *            buildConfigField("String", "TRYONA_API_KEY", "\"${properties.getProperty("TRYONA_API_KEY")}\"")
 *        }
 *    }
 *
 * 3. Access via:
 *    BuildConfig.TRYONA_API_KEY
 */
object ApiConstants {

    /**
     * Tryona AI Try-On API Configuration
     */
    object Tryona {
        val BASE_URL: String = BuildConfig.TRYONA_BASE_URL
        val API_KEY: String = BuildConfig.TRYONA_API_KEY

        // Multipart field names
        const val FIELD_PERSON_FILE = "person_file"
        const val FIELD_GARMENT_FILE = "garment_file"

        // Response keys
        const val RESPONSE_IMAGE_URL = "tryonImageUrl"

        // Timeouts
        const val CONNECT_TIMEOUT = 30L
        const val READ_TIMEOUT = 120L
    }

    /**
     * Image Processing Configuration
     */
    object ImageProcessing {
        // Maximum file size in bytes (10 MB)
        const val MAX_FILE_SIZE = 10 * 1024 * 1024

        // Target file size for compression (2 MB)
        const val TARGET_FILE_SIZE = 2 * 1024 * 1024

        // Maximum image dimension (width or height)
        const val MAX_DIMENSION = 2048

        // JPEG compression quality
        const val INITIAL_QUALITY = 90
        const val MIN_QUALITY = 60

        // Supported MIME types
        val SUPPORTED_MIME_TYPES = listOf(
            "image/jpeg",
            "image/png",
            "image/webp"
        )
    }

    /**
     * App Configuration
     */
    object App {
        // Enable debug logging
        val DEBUG_MODE: Boolean = BuildConfig.DEBUG

        // Retry configuration
        const val MAX_RETRY_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 2000L

        // Cache settings
        const val ENABLE_CACHE = true
        const val CACHE_SIZE_MB = 50
    }

    /**
     * Get API key from BuildConfig (for production)
     * Uncomment and use this method when you've set up BuildConfig
     */
    /*
    fun getApiKey(): String {
        return try {
            BuildConfig.TRYONA_API_KEY
        } catch (e: Exception) {
            // Fallback to hardcoded key for development
            android.util.Log.w("ApiConstants", "Failed to load API key from BuildConfig, using fallback")
            Tryona.API_KEY
        }
    }
    */

    /**
     * Check if API key is configured
     */
    fun isApiKeyConfigured(): Boolean {
        return Tryona.API_KEY.isNotBlank()
    }

    /**
     * Validate API configuration
     */
    fun validateConfiguration(): Pair<Boolean, String?> {
        if (!isApiKeyConfigured()) {
            return Pair(false, "API key is not configured. Please add your Tryona API key.")
        }

        if (Tryona.BASE_URL.isBlank()) {
            return Pair(false, "Base URL is not configured.")
        }

        return Pair(true, null)
    }
}
