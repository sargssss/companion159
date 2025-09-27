package com.lifelover.companion159.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SyncPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "inventory_sync_prefs",
        Context.MODE_PRIVATE
    )

    fun getLastSyncTimestamp(): String {
        return prefs.getString("last_sync_timestamp", "1970-01-01T00:00:00.000Z")
            ?: "1970-01-01T00:00:00.000Z"
    }

    fun saveLastSyncTimestamp(timestamp: String) {
        prefs.edit {
            putString("last_sync_timestamp", timestamp)
        }
    }
}