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
            // Check if Web Client ID is configured
            if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isEmpty() ||
                BuildConfig.GOOGLE_WEB_CLIENT_ID == "\"\"") {
                Log.e(TAG, "GOOGLE_WEB_CLIENT_ID is not configured in local.properties")
                return@withContext Result.failure(
                    Exception("Google Web Client ID is not configured. Please add GOOGLE_WEB_CLIENT_ID to local.properties")
                )
            }

            // Configure Google ID option
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            // Create credential request
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // Get Google credential
            val result = credentialManager.getCredential(
                request = request,
                context = activityContext // Use Activity context instead of Application context
            )

            // Parse Google ID token
            val credential = GoogleIdTokenCredential.createFrom(result.credential.data)

            val signInResult = GoogleSignInResult(
                idToken = credential.idToken,
                displayName = credential.displayName,
                email = credential.id,
                profilePictureUri = credential.profilePictureUri?.toString()
            )

            Log.d(TAG, "=== Google Sign-In SUCCESS ===")
            Result.success(signInResult)

        } catch (e: GetCredentialException) {
            Result.failure(Exception("Google Sign-In failed: ${e.message}"))

        } catch (e: GoogleIdTokenParsingException) {
            Result.failure(Exception("Invalid Google token: ${e.message}"))

        } catch (e: Exception) {
            Result.failure(Exception("Authentication error: ${e.message}"))
        }
    }

    /**
     * Sign out from Google (handled by app auth state)
     */
    suspend fun signOut(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Credential Manager handles sign out automatically
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}