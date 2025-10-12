package com.lifelover.companion159.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("companion_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_USER_ID = "last_user_id"
    }

    /**
     * Save last authenticated user ID
     */
    fun setLastUserId(userId: String?) {
        prefs.edit { putString(KEY_LAST_USER_ID, userId) }
    }

    /**
     * Get last authenticated user ID
     */
    fun getLastUserId(): String? {
        return prefs.getString(KEY_LAST_USER_ID, null)
    }
}