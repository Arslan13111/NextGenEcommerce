package com.example.nextgenecommerce.data.config

import com.example.nextgenecommerce.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseConfig {

    private val SUPABASE_URL = BuildConfig.SUPABASE_URL
    private val SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY

    /**
     * Google Web Client ID for Sign-In.
     * 
     * To find your Web Client ID:
     * 1. Go to Google Cloud Console (https://console.cloud.google.com/)
     * 2. Select your project
     * 3. Go to APIs & Services > Credentials
     * 4. Look for "Web client (Auto-created for Google Sign-in)" or similar under OAuth 2.0 Client IDs
     * 5. DO NOT use the "Android" client ID here; use the "Web" client ID.
     */
    val GOOGLE_WEB_CLIENT_ID: String = BuildConfig.GOOGLE_WEB_CLIENT_ID

    val client: SupabaseClient by lazy {
        require(SUPABASE_URL.isNotBlank()) { "SUPABASE_URL must be configured in local.properties or CI." }
        require(SUPABASE_ANON_KEY.isNotBlank()) { "SUPABASE_ANON_KEY must be configured in local.properties or CI." }
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest) {
                serializer = KotlinXSerializer(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                })
            }
            install(Storage)
            install(Realtime)
        }
    }

    val auth: Auth
        get() = client.auth

    val database: Postgrest
        get() = client.postgrest

    val storage: Storage
        get() = client.storage

    val realtime: Realtime
        get() = client.realtime
}
