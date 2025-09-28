package com.lifelover.companion159.data.remote.auth

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
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sign in with Google OAuth
     */
    suspend fun signInWithGoogle(): Result<Unit> {
        return try {
            val googleResult = googleAuthService.signInWithGoogle()

            googleResult.fold(
                onSuccess = { googleSignInResult ->
                    // Authenticate with Supabase using Google ID token
                    client.auth.signInWith(IDToken) {
                        idToken = googleSignInResult.idToken
                        provider = io.github.jan.supabase.auth.providers.Google
                    }
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reset password
     */
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            client.auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get current user ID
     */
    fun getUserId(): String? = getCurrentUser()?.id
}