package com.lifelover.companion159.data.remote.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.lifelover.companion159.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class GoogleSignInResult(
    val idToken: String,
    val displayName: String?,
    val email: String?,
    val profilePictureUri: String?
)

@Singleton
class GoogleAuthService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "GoogleAuthService"
    }

    private val credentialManager = CredentialManager.create(context)

    /**
     * Sign in with Google using Credential Manager API
     * Works for apps distributed outside Google Play Store
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<GoogleSignInResult> = withContext(Dispatchers.Main) {
        try {
            Log.d(TAG, "=== Starting Google Sign-In ===")
            Log.d(TAG, "Web Client ID: ${BuildConfig.GOOGLE_WEB_CLIENT_ID}")

            // Check if Web Client ID is configured
            if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isEmpty() ||
                BuildConfig.GOOGLE_WEB_CLIENT_ID == "\"\"") {
                Log.e(TAG, "GOOGLE_WEB_CLIENT_ID is not configured in local.properties")
                return@withContext Result.failure(
                    Exception("Google Web Client ID is not configured. Please add GOOGLE_WEB_CLIENT_ID to local.properties")
                )
            }

            // Configure Google ID option
            Log.d(TAG, "Creating GetGoogleIdOption...")
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false) // Allow account picker
                .setAutoSelectEnabled(false) // Always show account picker
                .build()

            Log.d(TAG, "GetGoogleIdOption created successfully")

            // Create credential request
            Log.d(TAG, "Creating GetCredentialRequest...")
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            Log.d(TAG, "GetCredentialRequest created successfully")

            // Get Google credential
            Log.d(TAG, "Requesting credentials from CredentialManager...")
            val result = credentialManager.getCredential(
                request = request,
                context = activityContext // Use Activity context instead of Application context
            )

            Log.d(TAG, "Credential received, type: ${result.credential.type}")

            // Parse Google ID token
            Log.d(TAG, "Parsing Google ID token...")
            val credential = GoogleIdTokenCredential.createFrom(result.credential.data)

            Log.d(TAG, "Google ID token parsed successfully")
            Log.d(TAG, "Display name: ${credential.displayName}")
            Log.d(TAG, "Email: ${credential.id}")

            val signInResult = GoogleSignInResult(
                idToken = credential.idToken,
                displayName = credential.displayName,
                email = credential.id,
                profilePictureUri = credential.profilePictureUri?.toString()
            )

            Log.d(TAG, "=== Google Sign-In SUCCESS ===")
            Result.success(signInResult)

        } catch (e: GetCredentialException) {
            Log.e(TAG, "=== GetCredentialException ===")
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            Log.e(TAG, "Stack trace:", e)
            Result.failure(Exception("Google Sign-In failed: ${e.message}"))

        } catch (e: GoogleIdTokenParsingException) {
            Log.e(TAG, "=== GoogleIdTokenParsingException ===")
            Log.e(TAG, "Exception message: ${e.message}")
            Log.e(TAG, "Stack trace:", e)
            Result.failure(Exception("Invalid Google token: ${e.message}"))

        } catch (e: Exception) {
            Log.e(TAG, "=== Unexpected Exception ===")
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            Log.e(TAG, "Stack trace:", e)
            Result.failure(Exception("Authentication error: ${e.message}"))
        }
    }

    /**
     * Sign out from Google (handled by app auth state)
     */
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Signing out from Google")
            // Credential Manager handles sign out automatically
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error", e)
            Result.failure(e)
        }
    }
}