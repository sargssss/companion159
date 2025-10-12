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
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication service for Supabase
 * Handles Google-only authentication
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
        com.lifelover.companion159.data.remote.client.SupabaseClient.client
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

            // Step 1: Get Google ID token
            val googleResult = googleAuthService.signInWithGoogle(context)

            val googleIdToken = googleResult.toString()

            Log.d(TAG, "✅ Got Google ID token")

            // Step 2: Authenticate with Supabase using Google token
            auth.signInWith(IDToken) {
                idToken = googleIdToken
                provider = Google
            }

            Log.d(TAG, "✅ Authenticated with Supabase")

            // Step 3: Save user info
            val userId = getUserId()
            if (userId != null) {
                userPreferences.setLastUserId(userId)
                Log.d(TAG, "✅ Saved userId: $userId")
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Google Sign-In failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sign out
     */
    suspend fun signOut() {
        try {
            Log.d(TAG, "🚪 Signing out...")
            auth.signOut()
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
}