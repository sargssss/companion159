package com.lifelover.companion159.data.remote.auth

import android.content.Context
import android.util.Log
import com.lifelover.companion159.data.local.UserPreferences
import com.lifelover.companion159.data.remote.client.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    private val client = SupabaseClient.client

    /**
     * Authentication status flow
     */
    val isAuthenticated: Flow<Boolean> = client.auth.sessionStatus.map {
        it is io.github.jan.supabase.auth.status.SessionStatus.Authenticated
    }

    /**
     * Get current user
     */
    fun getCurrentUser(): UserInfo? = client.auth.currentUserOrNull()

    /**
     * Get current user ID
     */
    fun getUserId(): String? = getCurrentUser()?.id

    fun getUserIdForSync(): String? {
        val currentUserId = getUserId()
        if (currentUserId != null) {
            return currentUserId
        }

        return userPreferences.getLastUserId()
    }

    private fun saveCurrentUserAsLast() {
        val user = getCurrentUser()
        if (user != null) {
            userPreferences.saveLastUser(user.id, user.email)
            Log.d(TAG, "💾 Saved last user: ${user.email} (${user.id})")
        }
    }

    /**
     * Sign up with email/password
     */
    suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            saveCurrentUserAsLast()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign in with email/password
     */
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            saveCurrentUserAsLast()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign in with Google OAuth
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<Unit> {
        return try {
            // Step 1: Get Google ID token
            val googleResult = googleAuthService.signInWithGoogle(activityContext)

            googleResult.fold(
                onSuccess = { googleSignInResult ->
                    // Step 2: Authenticate with Supabase using Google ID token
                    try {
                        client.auth.signInWith(IDToken) {
                            idToken = googleSignInResult.idToken
                            provider = io.github.jan.supabase.auth.providers.Google
                        }
                        saveCurrentUserAsLast()
                        Result.success(Unit)
                    } catch (e: Exception) {
                        Result.failure(Exception("Supabase authentication failed: ${e.message}"))
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "FAILED - Google Sign-In Error: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "=== Unexpected error in signInWithGoogle ===")
            Result.failure(e)
        }
    }

    /**
     * Sign out
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            client.auth.signOut()
            googleAuthService.signOut()
            Log.d(TAG, "Sign out successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed", e)
            Result.failure(e)
        }
    }
}