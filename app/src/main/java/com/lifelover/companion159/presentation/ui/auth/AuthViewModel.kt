package com.lifelover.companion159.presentation.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isAuthenticated: Boolean = false,
    val isOffline: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val userEmail: String? = null,
    val hasExplicitlyLoggedOut: Boolean = false
)

/**
 * ViewModel for authentication
 * Handles login, logout, and auth state
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
     * Sign in with email and password
     */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            authService.signIn(email, password)
                .onSuccess {
                    _state.update { it.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        userEmail = email
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
     * Sign up with email and password
     */
    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            authService.signUp(email, password)
                .onSuccess {
                    _state.update { it.copy(
                        isLoading = false,
                        error = "Реєстрація успішна! Перевірте email для підтвердження."
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
     * Sign in with Google
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
                hasExplicitlyLoggedOut = true,
                isOffline = _state.value.isOffline
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
            exception.message?.contains("Invalid login") == true ->
                "Невірний email або пароль"
            exception.message?.contains("User already registered") == true ->
                "Користувач з таким email вже зареєстрований"
            exception.message?.contains("Password") == true ->
                "Пароль має містити мінімум 6 символів"
            exception.message?.contains("Google Sign-In failed") == true ->
                "Помилка входу через Google"
            else -> exception.message ?: "Невідома помилка"
        }
    }
}