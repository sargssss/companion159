package com.lifelover.companion159.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit
import java.util.Date

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("companion_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_USER_ID = "last_user_id"
        private const val KEY_LAST_LOGIN_TIME = "last_login_time"
        private const val POSITION_RESELECT_INTERVAL_MS = 3 * 24 * 60 * 60 * 1000L
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

    /**
     * Save last login timestamp
     * Called after successful authentication
     * Used to determine if position re-selection is needed
     */
    fun setLastLoginTime(timestamp: Long = System.currentTimeMillis()) {
        prefs.edit { putLong(KEY_LAST_LOGIN_TIME, timestamp) }
    }

    /**
     * Get last login timestamp
     */
    fun getLastLoginTime(): Long {
        return prefs.getLong(KEY_LAST_LOGIN_TIME, 0L)
    }

    /**
     * Check if position re-selection is needed
     * Returns true if:
     * - Never logged in before (timestamp = 0)
     * - More than 3 days passed since last login
     *
     * @return true if position should be shown, false if can skip
     */
    fun shouldShowPositionSelection(): Boolean {
        val lastLogin = getLastLoginTime()

        // First time - always show
        if (lastLogin == 0L) return true

        // Check if 3 days passed
        val now = System.currentTimeMillis()
        val timePassed = now - lastLogin

        return timePassed >= POSITION_RESELECT_INTERVAL_MS
    }
}