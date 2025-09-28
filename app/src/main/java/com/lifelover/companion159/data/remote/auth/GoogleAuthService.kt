package com.lifelover.companion159.data.remote.auth

import android.content.Context
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
    private val credentialManager = CredentialManager.create(context)

    /**
     * Sign in with Google using Credential Manager API
     * Works for apps distributed outside Google Play Store
     */
    suspend fun signInWithGoogle(): Result<GoogleSignInResult> = withContext(Dispatchers.IO) {
        try {
            // Configure Google ID option
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false) // Allow account picker
                .setAutoSelectEnabled(false) // Always show account picker
                .build()

            // Create credential request
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            // Get Google credential
            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            // Parse Google ID token
            val credential = GoogleIdTokenCredential.createFrom(result.credential.data)

            val signInResult = GoogleSignInResult(
                idToken = credential.idToken,
                displayName = credential.displayName,
                email = credential.id,
                profilePictureUri = credential.profilePictureUri?.toString()
            )

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