package com.lifelover.companion159.presentation.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelover.companion159.AppNavigation
import com.lifelover.companion159.R
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.domain.models.AppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: SupabaseAuthService
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
                _state.update { it.copy(
                    isAuthenticated = isAuth,
                    userEmail = authService.getCurrentUser()?.email
                )}
            }
        }
    }

    /**
     * Sign in with Google
     * The only authentication method available
     */
    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            authService.signInWithGoogle(context)
                .onSuccess {
                    Log.d(TAG, "Authentication successful")
                    _state.update { it.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        userEmail = authService.getCurrentUser()?.email
                    )}
                }
                .onFailure { exception ->
                    Log.e(TAG, "Authentication failed: ${exception.message}")

                    val error = when {
                        exception.message?.contains("Google Sign-In failed", ignoreCase = true) == true ->
                            AppError.Authentication.GoogleSignInFailed(exception)
                        exception.message?.contains("cancelled", ignoreCase = true) == true ->
                            AppError.Authentication.GoogleSignInFailed(exception)
                        exception.message?.contains("network", ignoreCase = true) == true ->
                            AppError.Network.NoConnection(exception)
                        else -> AppError.Unknown(exception.message ?: "Authentication error", exception)
                    }

                    _state.update { it.copy(
                        isLoading = false,
                        error = error
                    )}
                }
        }
    }

    /**
     * Sign out from all services
     */
    fun signOut() {
        viewModelScope.launch {
            Log.d(TAG, "Signing out")
            authService.signOut()
            _state.update { AuthState(
                hasExplicitlyLoggedOut = true
            )}
        }
    }

    /**
     * Clear logout flag after navigation
     */
    fun clearLogoutFlag() {
        _state.update { it.copy(hasExplicitlyLoggedOut = false) }
    }
}