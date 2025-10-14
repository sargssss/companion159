package com.lifelover.companion159.data.remote.auth

import android.content.Context
import android.util.Log
import com.lifelover.companion159.data.local.UserPreferences
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import org.slf4j.MDC.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication service for Supabase
 * Handles Google-only authentication
 * Works with GoogleAuthService that returns GoogleSignInResult
 */
@Singleton
class SupabaseAuthService @Inject constructor(
    private val googleAuthService: GoogleAuthService,
    private val userPreferences: UserPreferences
) {
    companion object {
        private const val TAG = "SupabaseAuthService"
    }

    private val client: SupabaseClient by lazy {
        com.lifelover.companion159.data.remote.SupabaseClient.client
    }

    private val auth: Auth by lazy {
        client.auth
    }

    /**
     * Flow that emits authentication state
     */
    val isAuthenticated: Flow<Boolean> = auth.sessionStatus.map { status ->
        status is SessionStatus.Authenticated
    }

    /**
     * Sign in with Google
     * The only authentication method available
     */
    suspend fun signInWithGoogle(context: Context): Result<Unit> {
        return try {
            Log.d(TAG, "🔐 Starting Google Sign-In...")

            // Step 1: Get Google credentials
            val signInResult = googleAuthService.signInWithGoogle(context)
                .getOrElse { error ->
                    Log.e(TAG, "❌ Google Sign-In failed: ${error.message}")
                    return Result.failure(error)
                }

            Log.d(TAG, "✅ Got Google credentials")
            Log.d(TAG, "   Email: ${signInResult.email}")
            Log.d(TAG, "   Display Name: ${signInResult.displayName}")

            // Step 2: Authenticate with Supabase using Google ID token
            Log.d(TAG, "🔄 Authenticating with Supabase...")

            try {
                auth.signInWith(IDToken) {
                    idToken = signInResult.idToken
                    provider = Google
                }
                Log.d(TAG, "✅ Authenticated with Supabase (IDToken provider)")
            } catch (idTokenError: Exception) {
                Log.e(TAG, "❌ Both auth methods failed")
            }

            // Step 3: Save user info
            val userId = getUserId()
            val userEmail = getCurrentUser()?.email

            if (userId != null) {
                userPreferences.setLastUserId(userId)
                Log.d(TAG, "✅ Saved userId: $userId")
                Log.d(TAG, "✅ Supabase email: $userEmail")
            } else {
                Log.w(TAG, "⚠️ WARNING: userId is null after authentication")
            }

            Log.d(TAG, "🎉 Authentication completed successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Authentication failed")
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Sign out
     */
    suspend fun signOut() {
        try {
            Log.d(TAG, "🚪 Signing out...")

            // Sign out from Supabase
            auth.signOut()

            // Sign out from Google
            googleAuthService.signOut()

            // Clear local preferences
            userPreferences.setLastUserId(null)

            Log.d(TAG, "✅ Signed out successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sign out failed", e)
        }
    }

    /**
     * Get current user ID
     */
    fun getUserId(): String? {
        return auth.currentUserOrNull()?.id
    }

    /**
     * Get current user
     */
    fun getCurrentUser() = auth.currentUserOrNull()

    /**
     * Check if user is currently authenticated
     */
    fun isUserAuthenticated(): Boolean {
        return auth.currentUserOrNull() != null
    }

    /**
     * Update user metadata with crew_name
     * Called after user selects their position
     *
     * @param crewName Position name (e.g. "Барі", "Редбул", etc.)
     */
    /**
     * Update user metadata with crew_name
     * Called after user selects their position
     *
     * @param crewName Position name (e.g. "Барі", "Редбул", etc.)
     */
    suspend fun updateUserCrewName(crewName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📝 Updating user metadata with crew_name: $crewName")

            // Import kotlinx.serialization.json.buildJsonObject
            // Update user metadata in Supabase
            client.auth.updateUser {
                data = buildJsonObject {
                    put("crew_name", crewName)
                }
            }

            Log.d(TAG, "✅ User metadata updated successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update user metadata", e)
            Result.failure(e)
        }
    }

    /**
     * Get current user's crew_name from metadata
     * Returns null if not set
     */
    suspend fun getUserCrewName(): String? = withContext(Dispatchers.IO) {
        try {
            val session = client.auth.currentSessionOrNull()
            val crewName = session?.user?.userMetadata?.get("crew_name") as? String
            Log.d(TAG, "Current crew_name from metadata: $crewName")
            crewName
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get crew_name from metadata", e)
            null
        }
    }
}