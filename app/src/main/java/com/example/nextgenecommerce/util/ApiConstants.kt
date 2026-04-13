package com.example.nextgenecommerce.util

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
 *    RAPID_API_KEY=your_actual_api_key_here
 *
 * 2. Add to build.gradle.kts:
 *    android {
 *        ...
 *        defaultConfig {
 *            ...
 *            val properties = Properties()
 *            properties.load(project.rootProject.file("local.properties").inputStream())
 *            buildConfigField("String", "RAPID_API_KEY", "\"${properties.getProperty("RAPID_API_KEY")}\"")
 *        }
 *    }
 *
 * 3. Access via:
 *    BuildConfig.RAPID_API_KEY
 */
object ApiConstants {

    /**
     * Try-On Diffusion API Configuration
     */
    object TryOnDiffusion {
        // Base URL for the API
        const val BASE_URL = "https://try-on-diffusion.p.rapidapi.com/"

        // API Host (required for RapidAPI)
        const val API_HOST = "try-on-diffusion.p.rapidapi.com"

        // API Key - REPLACE THIS WITH YOUR OWN KEY
        // Get your key from: https://rapidapi.com/soltanlabs/api/try-on-diffusion
        const val API_KEY = "5c1a41631fmsh8e573a5fbb4c9f6p1fd101jsnfebe441d7708"

        // Endpoints
        const val ENDPOINT_TRY_ON_FILE = "try-on-file"
        const val ENDPOINT_TRY_ON_URL = "try-on-url"

        // Timeout settings (in seconds)
        const val CONNECT_TIMEOUT = 30L
        const val READ_TIMEOUT = 90L
        const val WRITE_TIMEOUT = 90L

        // Request field names (must match API documentation)
        const val FIELD_CLOTHING_IMAGE = "clothing_image"
        const val FIELD_AVATAR_IMAGE = "avatar_image"
        const val FIELD_CLOTHING_PROMPT = "clothing_prompt"
        const val FIELD_AVATAR_SEX = "avatar_sex"
        const val FIELD_AVATAR_PROMPT = "avatar_prompt"

        // HTTP Headers
        const val HEADER_RAPID_API_KEY = "x-rapidapi-key"
        const val HEADER_RAPID_API_HOST = "x-rapidapi-host"
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
        const val DEBUG_MODE = true

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
            BuildConfig.RAPID_API_KEY
        } catch (e: Exception) {
            // Fallback to hardcoded key for development
            android.util.Log.w("ApiConstants", "Failed to load API key from BuildConfig, using fallback")
            TryOnDiffusion.API_KEY
        }
    }
    */

    /**
     * Check if API key is configured
     */
    fun isApiKeyConfigured(): Boolean {
        return TryOnDiffusion.API_KEY.isNotBlank() &&
               TryOnDiffusion.API_KEY != "YOUR_API_KEY_HERE"
    }

    /**
     * Validate API configuration
     */
    fun validateConfiguration(): Pair<Boolean, String?> {
        if (!isApiKeyConfigured()) {
            return Pair(false, "API key is not configured. Please add your RapidAPI key.")
        }

        if (TryOnDiffusion.BASE_URL.isBlank()) {
            return Pair(false, "Base URL is not configured.")
        }

        return Pair(true, null)
    }
}
