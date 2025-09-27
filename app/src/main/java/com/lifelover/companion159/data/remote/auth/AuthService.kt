package com.lifelover.companion159.data.remote.auth

import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor() {

    private val auth = SupabaseClient.client.auth

    // ═══════════════════════════════════════════════════════════════════
    // Стан аутентифікації
    // ═══════════════════════════════════════════════════════════════════

    val isLoggedIn: Flow<Boolean> = auth.sessionStatus.map {
        it.isAuthenticated
    }

    val currentUser: Flow<UserInfo?> = auth.sessionStatus.map {
        it.session?.user
    }

    fun getCurrentUserId(): String? {
        return auth.currentUserOrNull()?.id
    }

    // ═══════════════════════════════════════════════════════════════════
    // Методи аутентифікації
    // ═══════════════════════════════════════════════════════════════════

    suspend fun signUp(email: String, password: String) {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    suspend fun resetPassword(email: String) {
        auth.resetPasswordForEmail(email)
    }

    // ═══════════════════════════════════════════════════════════════════
    // Перевірки
    // ═══════════════════════════════════════════════════════════════════

    fun isAuthenticated(): Boolean {
        return auth.currentUserOrNull() != null
    }

    suspend fun refreshSession() {
        auth.refreshCurrentSession()
    }
}