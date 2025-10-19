// data/remote/sync/SyncBackgroundService.kt
package com.lifelover.companion159.data.remote.sync

import android.app.Service
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.lifelover.companion159.data.local.UserPreferences
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.repository.PositionRepository
import kotlinx.coroutines.cancel

@AndroidEntryPoint
class SyncBackgroundService : Service() {

    companion object {
        private const val TAG = "SyncBackgroundService"
    }

    @Inject
    lateinit var connectivityMonitor: ConnectivityMonitor

    @Inject
    lateinit var syncOrchestrator: SyncOrchestrator

    @Inject
    lateinit var positionRepository: PositionRepository

    @Inject
    lateinit var syncService: SyncService

    @Inject
    lateinit var authService: SupabaseAuthService

    @Inject
    lateinit var userPreferences: UserPreferences

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 SyncBackgroundService created")

        connectivityMonitor.startMonitoring()

        serviceScope.launch {
            connectivityMonitor.isConnected.collect { isConnected ->
                Log.d(TAG, "Network state: isConnected=$isConnected")

                if (isConnected) {
                    checkAndTriggerSync()
                }
            }
        }
    }

    // data/remote/sync/SyncBackgroundService.kt
    private suspend fun checkAndTriggerSync() {
        try {
            // Try current session first
            var userId = authService.getUserId()

            // If not in current session, use last saved userId
            if (userId.isNullOrBlank()) {
                userId = userPreferences.getLastUserId()
                Log.d(TAG, "Using last saved userId: $userId")
            }

            if (userId.isNullOrBlank()) {
                Log.d(TAG, "⏭️ Skip: no userId found (user never logged in)")
                return
            }

            val crewName = positionRepository.getPosition()
            if (crewName.isNullOrBlank()) {
                Log.d(TAG, "⏭️ Skip: position not set")
                return
            }

            Log.d(TAG, "✅ Checking sync status for userId=$userId, crewName=$crewName")

            val pendingCount = syncService.getPendingSyncCount(userId, crewName)

            Log.d(TAG, "Found $pendingCount pending items")

            if (pendingCount > 0) {
                Log.d(TAG, "🔄 Found $pendingCount items to sync - triggering sync")
                triggerSyncWithUserId(userId, crewName)
            } else {
                Log.d(TAG, "✅ No items to sync")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error checking sync status", e)
        }
    }

    /**
     * Trigger sync with explicit userId
     * Used by background service when app is closed
     */
    private suspend fun triggerSyncWithUserId(userId: String, crewName: String) {
        try {
            Log.d(TAG, "🔄 Starting sync with userId=$userId, crewName=$crewName")

            // Upload pending items
            syncService.uploadPendingItems(userId, crewName)
                .onSuccess { count ->
                    Log.d(TAG, "✅ Uploaded $count items")
                }
                .onFailure { error ->
                    Log.e(TAG, "⚠️ Upload failed: ${error.message}")
                }

            // Download server changes
            syncService.downloadServerItems(
                crewName = crewName,
                userId = userId,
                forceFullSync = false
            )
                .onSuccess { count ->
                    Log.d(TAG, "✅ Merged $count items from server")
                }
                .onFailure { error ->
                    Log.e(TAG, "⚠️ Download failed: ${error.message}")
                }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Sync failed", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started with intent: $intent")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "🛑 SyncBackgroundService destroyed")
        connectivityMonitor.stopMonitoring()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}