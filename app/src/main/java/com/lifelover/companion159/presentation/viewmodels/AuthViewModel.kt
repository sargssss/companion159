package com.lifelover.companion159.presentation.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
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
    val error: String? = null,
    val userEmail: String? = null,
    val hasExplicitlyLoggedOut: Boolean = false
)

/**
 * ViewModel for authentication
 * Handles Google-only authentication flow
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: SupabaseAuthService
) : ViewModel() {

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
                    _state.update { it.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        userEmail = authService.getCurrentUser()?.email
                    )}
                }
                .onFailure { exception ->
                    _state.update { it.copy(
                        isLoading = false,
                        error = mapError(exception)
                    )}
                }
        }
    }

    /**
     * Sign out
     */
    fun signOut() {
        viewModelScope.launch {
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

    /**
     * Map exceptions to user-friendly error messages
     */
    private fun mapError(exception: Throwable): String {
        return when {
            exception.message?.contains("Google Sign-In failed") == true ->
                "Помилка входу через Google"
            exception.message?.contains("cancelled") == true ->
                "Вхід скасовано"
            exception.message?.contains("network") == true ->
                "Немає підключення до інтернету"
            else -> exception.message ?: "Невідома помилка"
        }
    }
}