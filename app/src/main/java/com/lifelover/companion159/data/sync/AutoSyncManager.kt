package com.lifelover.companion159.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.lifelover.companion159.data.local.UserPreferences
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.network.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncService: SyncService,
    private val networkMonitor: NetworkMonitor,
    private val authService: SupabaseAuthService,
    private val userPreferences: UserPreferences
) {
    companion object {
        private const val TAG = "AutoSyncManager"
        private const val IMMEDIATE_SYNC_WORK_NAME = "immediate_sync_work"
        private const val PERIODIC_SYNC_WORK_NAME = "periodic_sync_work"
        private const val IMMEDIATE_SYNC_TAG = "immediate_sync"
        private const val PERIODIC_SYNC_TAG = "periodic_sync"
        private const val SYNC_INTERVAL_MINUTES = 15L
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var isInitialized = false
    private var wasAuthenticated = false

    fun initialize() {
        if (isInitialized) return
        isInitialized = true

        Log.d(TAG, "🚀 Initializing AutoSyncManager")

        wasAuthenticated = authService.getCurrentUser() != null

        scope.launch {
            try {
                combine(
                    networkMonitor.isOnlineFlow,
                    authService.isAuthenticated
                ) { isOnline, isAuthenticated ->
                    isOnline to isAuthenticated
                }.collectLatest { (isOnline, isAuthenticated) ->

                    val hasUser = isAuthenticated || userPreferences.hasLastUser()

                    val justAuthenticated = isAuthenticated && !wasAuthenticated
                    if (justAuthenticated) {
                        wasAuthenticated = true
                        onUserAuthenticated()
                    } else if (!isAuthenticated) {
                        wasAuthenticated = false
                    }

                    handleStateChange(isOnline, hasUser)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ AutoSyncManager initialization error", e)
            }
        }
    }

    private suspend fun onUserAuthenticated() {
        try {
            Log.d(TAG, "🔍 Checking for offline items to sync...")

            val hasOfflineItems = syncService.hasOfflineItems()

            if (hasOfflineItems) {
                Log.d(TAG, "📦 Found offline items - triggering immediate sync")
                triggerCriticalSync()
            } else {
                Log.d(TAG, "✨ No offline items found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking offline items", e)
        }
    }

    private suspend fun handleStateChange(isOnline: Boolean, hasUser: Boolean) {
        try {
            when {
                isOnline && hasUser -> {
                    Log.d(TAG, "✅ Device online AND has user (current or last)")

                    if (syncService.hasUnsyncedChanges() || syncService.hasOfflineItems()) {
                        Log.d(TAG, "📤 Found data to sync, triggering sync")
                        triggerCriticalSync()
                    } else {
                        Log.d(TAG, "✨ No data to sync")
                    }

                    schedulePeriodicSync()
                }
                isOnline && !hasUser -> {
                    Log.d(TAG, "⚠️ Device online but NO user (never logged in)")
                    Log.d(TAG, "💡 User can work offline. Data will sync after first login.")
                    cancelPeriodicSync()
                }
                else -> {
                    Log.d(TAG, "📴 Device offline")
                    cancelPeriodicSync()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error handling state change", e)
        }
    }

    fun triggerCriticalSync() {
        try {
            Log.d(TAG, "🔥 Triggering CRITICAL sync with Foreground Service")
            SyncForegroundService.start(context)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start foreground sync, falling back to WorkManager", e)
            triggerImmediateSync()
        }
    }

    fun triggerImmediateSync() {
        try {
            Log.d(TAG, "⚡ Triggering immediate sync via WorkManager")
            val workManager = WorkManager.getInstance(context)

            val immediateWorkRequest = OneTimeWorkRequestBuilder<SimpleSyncWorker>()
                .setConstraints(createSyncConstraints())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    SimpleSyncWorker.RETRY_POLICY_DELAY,
                    TimeUnit.MILLISECONDS
                )
                .addTag(IMMEDIATE_SYNC_TAG)
                .build()

            workManager.enqueueUniqueWork(
                IMMEDIATE_SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                immediateWorkRequest
            )

            Log.d(TAG, "✅ Immediate sync work enqueued")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error triggering immediate sync", e)
        }
    }

    private fun schedulePeriodicSync() {
        try {
            Log.d(TAG, "⏰ Scheduling periodic sync (every $SYNC_INTERVAL_MINUTES minutes)")
            val workManager = WorkManager.getInstance(context)

            val periodicWorkRequest = PeriodicWorkRequestBuilder<SimpleSyncWorker>(
                SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(createSyncConstraints())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    SimpleSyncWorker.RETRY_POLICY_DELAY,
                    TimeUnit.MILLISECONDS
                )
                .addTag(PERIODIC_SYNC_TAG)
                .build()

            workManager.enqueueUniquePeriodicWork(
                PERIODIC_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )

            Log.d(TAG, "✅ Periodic sync work scheduled")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error scheduling periodic sync", e)
        }
    }

    private fun cancelPeriodicSync() {
        try {
            Log.d(TAG, "🛑 Cancelling periodic sync")
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cancelling periodic sync", e)
        }
    }

    private fun createSyncConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(false)
            .setRequiresDeviceIdle(false)
            .setRequiresStorageNotLow(true)
            .build()
    }
}