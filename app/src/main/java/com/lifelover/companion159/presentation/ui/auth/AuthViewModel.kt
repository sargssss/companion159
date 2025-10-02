package com.lifelover.companion159.presentation.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.sync.AutoSyncManager
import com.lifelover.companion159.network.NetworkMonitor
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

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authService: SupabaseAuthService,
    private val networkMonitor: NetworkMonitor,
    private val autoSyncManager: AutoSyncManager // НОВИЙ: Inject AutoSyncManager
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        checkAuthStatus()
        observeNetworkStatus()
    }

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

    private fun observeNetworkStatus() {
        viewModelScope.launch {
            networkMonitor.isOnlineFlow.collect { isOnline ->
                _state.update { it.copy(isOffline = !isOnline) }
            }
        }
    }

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

                    // НОВИЙ: Тригер синхронізації після успішного входу
                    triggerPostLoginSync()
                }
                .onFailure { exception ->
                    _state.update { it.copy(
                        isLoading = false,
                        error = mapError(exception)
                    )}
                }
        }
    }

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

                    // НОВИЙ: Тригер синхронізації після успішного входу через Google
                    triggerPostLoginSync()
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
     * НОВИЙ: Тригер синхронізації після входу
     */
    private fun triggerPostLoginSync() {
        viewModelScope.launch {
            try {
                android.util.Log.d("AuthViewModel", "🎉 User logged in - initializing sync")
                // AutoSyncManager автоматично перевірить офлайн елементи
                // завдяки новій логіці onUserAuthenticated()
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "❌ Failed to trigger post-login sync", e)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authService.signOut()
            _state.update { AuthState(
                hasExplicitlyLoggedOut = true,
                isOffline = _state.value.isOffline
            )}
        }
    }

    fun clearLogoutFlag() {
        _state.update { it.copy(hasExplicitlyLoggedOut = false) }
    }

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