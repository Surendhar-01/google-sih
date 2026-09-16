package com.example.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

private const val TAG = "GoogleAuthManager"

/**
 * Result model representing the outcome of a Google Sign-In operation.
 * The returned id_token is exchanged with Supabase (Sign In With ID Token).
 */
data class GoogleAuthResult(
    val isSuccess: Boolean,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val idToken: String? = null,
    val nonce: String? = null,
    val isNewUser: Boolean = false,
    val errorMessage: String? = null,
    val isCancelled: Boolean = false
)

/**
 * Manages Google Sign-In via Android Credential Manager only. The Google id_token
 * is handed to [SupabaseAuthService.loginWithGoogleAuthResult] which exchanges it
 * with Supabase. Firebase is intentionally not used.
 */
class GoogleAuthManager(private val context: Context) {

    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(context)
    }

    /**
     * Initiates the Google Sign-In flow using Credential Manager.
     *
     * @param activityContext The Activity Context hosting the bottom sheet.
     * @param serverClientId OAuth 2.0 Web Client ID (usually BuildConfig.GOOGLE_WEB_CLIENT_ID).
     */
    suspend fun signInWithGoogle(
        activityContext: Context,
        serverClientId: String
    ): GoogleAuthResult {
        if (serverClientId.isBlank()) {
            Log.w(TAG, "Google OAuth not configured: GOOGLE_WEB_CLIENT_ID is empty")
            return GoogleAuthResult(
                isSuccess = false,
                errorMessage = "Google Sign-In is not configured yet. Add GOOGLE_WEB_CLIENT_ID to the project .env file."
            )
        }
        return try {
            // Cryptographic raw nonce & its SHA-256 hash (verified by Supabase)
            val rawNonce = UUID.randomUUID().toString()
            val hashedNonce = sha256Hex(rawNonce)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val email = googleIdTokenCredential.id
                val displayName = googleIdTokenCredential.displayName ?: email.substringBefore("@")
                val photoUrl = googleIdTokenCredential.profilePictureUri?.toString()

                Log.d(TAG, "Google ID Token received for account: $email")
                GoogleAuthResult(
                    isSuccess = true,
                    email = email,
                    displayName = displayName,
                    photoUrl = photoUrl,
                    idToken = idToken,
                    nonce = rawNonce
                )
            } else {
                Log.w(TAG, "Unexpected credential type: ${credential::class.java.name}")
                GoogleAuthResult(
                    isSuccess = false,
                    errorMessage = "Unexpected credential format returned by Google Identity Services."
                )
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "Google Sign-In was cancelled by user")
            GoogleAuthResult(
                isSuccess = false,
                isCancelled = true,
                errorMessage = "Google Sign-In was cancelled."
            )
        } catch (e: NoCredentialException) {
            Log.w(TAG, "No Google credentials available on device: ${e.message}")
            GoogleAuthResult(
                isSuccess = false,
                errorMessage = "No Google Account found on this device. Please add a Google account in device Settings or choose another login option."
            )
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error: ${e.type} - ${e.message}")
            GoogleAuthResult(
                isSuccess = false,
                errorMessage = "Google Sign-In failed (${e.message ?: "Authentication error"})."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unhandled exception during Google Sign-In: ${e.message}", e)
            GoogleAuthResult(
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Failed to sign in with Google."
            )
        }
    }

    private fun sha256Hex(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.fold("") { str, byte -> str + "%02x".format(byte) }
    }
}