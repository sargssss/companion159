package com.lifelover.companion159.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREF_NAME = "user_preferences"
        private const val KEY_LAST_USER_ID = "last_user_id"
        private const val KEY_LAST_USER_EMAIL = "last_user_email"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /**
     * Зберігає останнього авторизованого користувача
     */
    fun saveLastUser(userId: String, email: String?) {
        prefs.edit()
            .putString(KEY_LAST_USER_ID, userId)
            .putString(KEY_LAST_USER_EMAIL, email)
            .apply()
    }

    /**
     * Отримує ID останнього авторизованого користувача
     */
    fun getLastUserId(): String? {
        return prefs.getString(KEY_LAST_USER_ID, null)
    }

    /**
     * Отримує email останнього авторизованого користувача
     */
    fun getLastUserEmail(): String? {
        return prefs.getString(KEY_LAST_USER_EMAIL, null)
    }

    /**
     * Перевіряє чи є збережений користувач
     */
    fun hasLastUser(): Boolean {
        return getLastUserId() != null
    }
}