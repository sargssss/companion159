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

    val isAuthenticated: Flow<Boolean> = auth.sessionStatus.map { status ->
        status is SessionStatus.Authenticated
    }

    /**
     * Sign in with Google
     * Records last login time for position re-selection logic
     */
    suspend fun signInWithGoogle(context: Context): Result<Unit> {
        return try {
            Log.d(TAG, "🔐 Starting Google Sign-In...")

            // Get Google credentials
            val signInResult = googleAuthService.signInWithGoogle(context)
                .getOrElse { error ->
                    Log.e(TAG, "❌ Google Sign-In failed: ${error.message}")
                    return Result.failure(error)
                }

            try {
                auth.signInWith(IDToken) {
                    idToken = signInResult.idToken
                    provider = Google
                }
                Log.d(TAG, "✅ Authenticated with Supabase (IDToken provider)")
            } catch (idTokenError: Exception) {
                Log.e(TAG, "❌ Both auth methods failed")
            }

            // Save user info
            val userId = getUserId()
            val userEmail = getCurrentUser()?.email

            if (userId != null) {
                userPreferences.setLastUserId(userId)
                userPreferences.setLastLoginTime()
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
     * Clears login time so position will be re-shown on next login
     */
    suspend fun signOut() {
        try {
            Log.d(TAG, "🚪 Signing out...")

            // Sign out from Supabase
            auth.signOut()

            // Sign out from Google
            googleAuthService.signOut()

            // Clear local preferences and login time
            userPreferences.setLastUserId(null)
            userPreferences.setLastLoginTime(0L)

            Log.d(TAG, "✅ Signed out successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Sign out failed", e)
        }
    }

    fun getUserId(): String? {
        return auth.currentUserOrNull()?.id
    }

    fun getCurrentUser() = auth.currentUserOrNull()

    fun isUserAuthenticated(): Boolean {
        return auth.currentUserOrNull() != null
    }

    suspend fun updateUserCrewName(crewName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📝 Updating user metadata with crew_name: $crewName")

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