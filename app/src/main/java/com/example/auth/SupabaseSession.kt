package com.example.auth

import com.example.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

/**
 * Centralised Supabase configuration and client.
 *
 * Secret values are injected at build time from the local `.env` / `.env.example`
 * files via the Secrets Gradle Plugin (exposed through [BuildConfig]). Until real
 * values are supplied, [SupabaseAuthService] returns an honest
 * PROVIDER_NOT_CONFIGURED error. No simulated credentials are ever used.
 */
object SupabaseAuthConfig {
    const val SUPABASE_URL_KEY = "SUPABASE_URL"
    const val ANON_KEY_KEY = "SUPABASE_ANON_KEY"
    const val GOOGLE_WEB_CLIENT_ID_KEY = "GOOGLE_WEB_CLIENT_ID"

    val supabaseUrl: String get() = BuildConfig.SUPABASE_URL ?: ""
    val anonKey: String get() = BuildConfig.SUPABASE_ANON_KEY ?: ""
    val googleWebClientId: String get() = BuildConfig.GOOGLE_WEB_CLIENT_ID ?: ""

    fun isConfigured(): Boolean =
        supabaseUrl.isNotBlank() && supabaseUrl.startsWith("https://") &&
            anonKey.isNotBlank() && !anonKey.startsWith("YOUR_")

    fun googleIsConfigured(): Boolean =
        googleWebClientId.isNotBlank() && !googleWebClientId.startsWith("YOUR_") &&
            !googleWebClientId.startsWith("DEFAULT_")

    val client: SupabaseClient by lazy {
        require(isConfigured()) { "Supabase is not configured. Set SUPABASE_URL and SUPABASE_ANON_KEY in .env" }
        createSupabaseClient(supabaseUrl, anonKey) {
            install(Auth)
            install(Postgrest)
        }
    }
}

/**
 * Mirrors the `profiles` table row that carries role, account status and
 * statutory identity details. Column names must match the database schema.
 */
@Serializable
data class ProfileRow(
    val id: String? = null,
    val auth_user_id: String? = null,
    val role: String? = null,
    val account_status: String? = null,
    val display_name: String? = null,
    val entity_name: String? = null,
    val statutory_identifier: String? = null,
    val phone_number: String? = null,
    val email: String? = null
)