package com.lifelover.companion159.data.remote.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor() {

    val isLoggedIn: Flow<Boolean> = flowOf(false)
    val userEmail: Flow<String?> = flowOf(null)

    fun getCurrentUserId(): String? = null

    suspend fun signUp(email: String, password: String) {
        // Заглушка
    }

    suspend fun signIn(email: String, password: String) {
        // Заглушка
    }

    suspend fun signOut() {
        // Заглушка
    }

    suspend fun resetPassword(email: String) {
        // Заглушка
    }

    fun isAuthenticated(): Boolean = false

    suspend fun refreshSession() {
        // Заглушка
    }
}