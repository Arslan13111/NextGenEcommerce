package com.example.nextgenecommerce.data.config

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseConfig {

    // TODO: Replace these with your actual Supabase project credentials
    private const val SUPABASE_URL = "https://ccrscwaixfmfglylcjpj.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNjcnNjd2FpeGZtZmdseWxjanBqIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjI4OTcwMTEsImV4cCI6MjA3ODQ3MzAxMX0.175rDjZRCH4v7_UTw-43q3aXkjrzLNoDPl3avWsVAcE"

    val client: SupabaseClient by lazy {
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
        }
    }

    val auth: Auth
        get() = client.auth

    val database: Postgrest
        get() = client.postgrest

    val storage: Storage
        get() = client.storage
}
