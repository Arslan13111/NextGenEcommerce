# Tryona API Migration Guide

This guide outlines the surgical steps to replace the legacy AI Diffusion (RapidAPI) with the Tryona Private API.

## 1. API Constants (app/src/main/java/com/example/nextgenecommerce/util/ApiConstants.kt)
Define the Tryona configuration block and update the validation logic.

```kotlin
object Tryona {
    const val BASE_URL = "https://api.tryona.com/"
    const val API_KEY = "ev7tVFRP1vTnYfGjhDYv46eS" // Private Key

    // Multipart field names
    const val FIELD_PERSON_FILE = "person_file"
    const val FIELD_GARMENT_FILE = "garment_file"

    // Response keys
    const val RESPONSE_IMAGE_URL = "tryonImageUrl"
    
    // Timeouts
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 120L
}

// Update validation
fun isApiKeyConfigured(): Boolean = Tryona.API_KEY.isNotBlank()
```

## 2. API Service (app/src/main/java/com/example/nextgenecommerce/api/TryonaApiService.kt)
Create the Retrofit interface using the multipart/form-data and tokenapi header pattern.

```kotlin
interface TryonaApiService {
    @Multipart
    @Headers("Accept: application/json")
    @POST("v1/tryon/simple")
    suspend fun tryOnSimple(
        @Part person_file: MultipartBody.Part,
        @Part garment_file: MultipartBody.Part,
        @Header("tokenapi") tokenapi: String
    ): Response<ResponseBody>
}
```

## 3. Repository Implementation (app/src/main/java/com/example/nextgenecommerce/repository/TryOnRepository.kt)
The repository must handle image downloading, file conversion, and multipart construction.
* **Step A:** Download the product image from Supabase URL using HttpURLConnection.
* **Step B:** Save both user (avatar) and product (garment) bitmaps to temporary files in context.cacheDir.
* **Step C:** Convert files to MultipartBody.Part using ImageUtils.
* **Step D:** Execute `apiService.tryOnSimple` with the `tokenapi` header.
* **Step E:** Parse the JSON response for `tryonImageUrl` and download the final bitmap.

## 4. ViewModel Strategy (app/src/main/java/com/example/nextgenecommerce/viewmodel/TryOnViewModel.kt)
* **Memory Safety:** Use `ImageUtils.loadBitmapFromUri` with a two-pass decode (downsampling to max 2048px) to prevent OutOfMemoryError.
* **Threading:** Always wrap bitmap operations in `withContext(Dispatchers.IO)`.
* **State Management:** Use `TryOnState` (Idle, Loading, Success, Error) to drive the Compose UI.

## 5. Dependency Injection (AppModule.kt)
* Provide `TryonaClient` with custom timeouts (120s Read).
* Inject `TryonaApiService` into `TryOnRepository`.
* Ensure legacy Fashn or RapidAPI services are removed to prevent build collisions.
