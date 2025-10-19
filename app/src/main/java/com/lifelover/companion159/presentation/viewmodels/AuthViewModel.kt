package com.lifelover.companion159.presentation.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.repository.PositionRepository
import com.lifelover.companion159.domain.models.AppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for authentication
 */
data class AuthState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val userEmail: String? = null,
    val hasExplicitlyLoggedOut: Boolean = false
)

/**
 * ViewModel for authentication management
 *
 * Responsibilities:
 * - Handle Google authentication flow
 * - Manage authentication state
 * - Provide user session information
 * - Clear user data on logout (including position)
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: SupabaseAuthService,
    private val positionRepository: PositionRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        checkAuthStatus()
    }

    /**
     * Observe authentication status from Supabase
     */
    private fun checkAuthStatus() {
        viewModelScope.launch {
            authService.isAuthenticated.collect { isAuth ->
                _state.update {
                    it.copy(
                        isAuthenticated = isAuth,
                        userEmail = authService.getCurrentUser()?.email
                    )
                }
            }
        }
    }

    /**
     * Sign in with Google
     * The only authentication method available
     *
     * After successful authentication:
     * - User will see Position screen if position not set
     * - Position screen shows automatically via AppNavigation logic
     */
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            authService.signInWithGoogle(context)
                .onSuccess {
                    Log.d(TAG, "✅ Authentication successful")
                    Log.d(TAG, "📍 User must select position (if not set)")

                    _state.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            userEmail = authService.getCurrentUser()?.email
                        )
                    }
                }
                .onFailure { exception ->
                    Log.e(TAG, "❌ Authentication failed: ${exception.message}")

                    val error = when {
                        exception.message?.contains(
                            "Google Sign-In failed",
                            ignoreCase = true
                        ) == true ->
                            AppError.Authentication.GoogleSignInFailed(exception)

                        exception.message?.contains("cancelled", ignoreCase = true) == true ->
                            AppError.Authentication.GoogleSignInFailed(exception)

                        exception.message?.contains("network", ignoreCase = true) == true ->
                            AppError.Network.NoConnection(exception)

                        else -> AppError.Unknown(
                            exception.message ?: "Authentication error",
                            exception
                        )
                    }

                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error
                        )
                    }
                }
        }
    }

    /**
     * Sign out from all services and clear user data
     *
     * Cleanup sequence:
     * 1. Sign out from Supabase and Google
     * 2. Clear position from database
     * 3. Mark explicit logout (for navigation)
     * 4. UI will navigate to Login screen
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🚪 Signing out...")

                // Sign out from all services
                authService.signOut()

                // Clear position - user must re-select on next login
                Log.d(TAG, "🗑️ Clearing position...")
                positionRepository.clearPosition()

                Log.d(TAG, "✅ Logout completed - clearing position")

                _state.update {
                    AuthState(
                        hasExplicitlyLoggedOut = true
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Logout failed", e)
                _state.update {
                    it.copy(
                        error = AppError.Unknown("Logout failed: ${e.message}", e)
                    )
                }
            }
        }
    }

    /**
     * Clear logout flag after navigation
     * Called by UI after successful navigation to Login
     */
    fun clearLogoutFlag() {
        _state.update { it.copy(hasExplicitlyLoggedOut = false) }
    }
}