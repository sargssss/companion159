package com.lifelover.companion159.data.sync

import android.content.Context
import android.util.Log
import com.lifelover.companion159.di.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SyncTestHelper {
    private const val TAG = "SyncTestHelper"

    suspend fun printSyncStatus(context: Context) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "")
            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "📊 SYNC STATUS REPORT")
            Log.d(TAG, "═══════════════════════════════════════")

            val authService = ServiceLocator.getSupabaseAuthService(context)
            val syncService = ServiceLocator.getSyncService(context)
            val userPreferences = ServiceLocator.getUserPreferences(context) // НОВИЙ

            // Поточний користувач
            val currentUser = authService.getCurrentUser()
            val isAuthenticated = currentUser != null

            Log.d(TAG, "👤 Current User:")
            Log.d(TAG, "   Authenticated: $isAuthenticated")
            if (isAuthenticated) {
                Log.d(TAG, "   Email: ${currentUser?.email}")
                Log.d(TAG, "   UserId: ${currentUser?.id}")
            }

            // НОВИЙ: Останній користувач
            val lastUserId = userPreferences.getLastUserId()
            val lastUserEmail = userPreferences.getLastUserEmail()
            Log.d(TAG, "")
            Log.d(TAG, "💾 Last Saved User:")
            Log.d(TAG, "   UserId: ${lastUserId ?: "none"}")
            Log.d(TAG, "   Email: ${lastUserEmail ?: "none"}")

            // Статус синхронізації
            Log.d(TAG, "")
            Log.d(TAG, "📦 Sync Status:")
            val hasOffline = syncService.hasOfflineItems()
            Log.d(TAG, "   Offline items: $hasOffline")

            val userIdForSync = authService.getUserIdForSync()
            if (userIdForSync != null) {
                val hasUnsynced = syncService.hasUnsyncedChanges()
                Log.d(TAG, "   Unsynced changes: $hasUnsynced")
                Log.d(TAG, "   Sync UserId: $userIdForSync")
            } else {
                Log.d(TAG, "   Unsynced changes: N/A (no user)")
            }

            Log.d(TAG, "═══════════════════════════════════════")
            Log.d(TAG, "")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to print sync status", e)
        }
    }
}