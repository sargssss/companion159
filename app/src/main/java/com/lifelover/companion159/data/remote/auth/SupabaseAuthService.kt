package com.lifelover.companion159.data.remote.auth

import android.content.Context
import android.util.Log
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
    private val googleAuthService: GoogleAuthService
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
     * Sign up with email/password
     */
    suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            Log.d(TAG, "Attempting sign up for email: $email")
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Log.d(TAG, "Sign up successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sign in with email/password
     */
    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            Log.d(TAG, "Attempting sign in for email: $email")
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Log.d(TAG, "Sign in successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            Result.failure(e)
        }
    }

    /**
     * Sign in with Google OAuth
     */
    suspend fun signInWithGoogle(activityContext: Context): Result<Unit> {
        return try {
            Log.d(TAG, "=== Starting Supabase Google Sign-In ===")

            // Step 1: Get Google ID token
            Log.d(TAG, "Step 1: Getting Google credentials...")
            val googleResult = googleAuthService.signInWithGoogle(activityContext)

            googleResult.fold(
                onSuccess = { googleSignInResult ->
                    Log.d(TAG, "Step 1: SUCCESS - Got Google ID token")
                    Log.d(TAG, "Email: ${googleSignInResult.email}")
                    Log.d(TAG, "Display name: ${googleSignInResult.displayName}")

                    // Step 2: Authenticate with Supabase using Google ID token
                    Log.d(TAG, "Step 2: Authenticating with Supabase...")
                    try {
                        client.auth.signInWith(IDToken) {
                            idToken = googleSignInResult.idToken
                            provider = io.github.jan.supabase.auth.providers.Google
                        }
                        Log.d(TAG, "Step 2: SUCCESS - Authenticated with Supabase")
                        Log.d(TAG, "=== Supabase Google Sign-In COMPLETE ===")
                        Result.success(Unit)
                    } catch (e: Exception) {
                        Log.e(TAG, "Step 2: FAILED - Supabase authentication error")
                        Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
                        Log.e(TAG, "Error message: ${e.message}")
                        Log.e(TAG, "Stack trace:", e)
                        Result.failure(Exception("Supabase authentication failed: ${e.message}"))
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "Step 1: FAILED - Google Sign-In error")
                    Log.e(TAG, "Error: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "=== Unexpected error in signInWithGoogle ===")
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Error message: ${e.message}")
            Log.e(TAG, "Stack trace:", e)
            Result.failure(e)
        }
    }

    /**
     * Sign out
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            Log.d(TAG, "Signing out from Supabase")
            client.auth.signOut()
            googleAuthService.signOut()
            Log.d(TAG, "Sign out successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign out failed", e)
            Result.failure(e)
        }
    }

    /**
     * Reset password
     */
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            Log.d(TAG, "Requesting password reset for: $email")
            client.auth.resetPasswordForEmail(email)
            Log.d(TAG, "Password reset email sent")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Password reset failed", e)
            Result.failure(e)
        }
    }

    /**
     * Get current user ID
     */
    fun getUserId(): String? = getCurrentUser()?.id
}