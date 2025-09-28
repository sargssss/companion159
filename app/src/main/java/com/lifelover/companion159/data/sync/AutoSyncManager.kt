package com.lifelover.companion159.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
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
    private val authService: SupabaseAuthService
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

    /**
     * Initialize auto-sync monitoring
     */
    fun initialize() {
        if (isInitialized) return
        isInitialized = true

        Log.d(TAG, "Initializing AutoSyncManager")

        scope.launch {
            try {
                // Combine network status and auth status
                combine(
                    networkMonitor.isOnlineFlow,
                    authService.isAuthenticated
                ) { isOnline, isAuthenticated ->
                    isOnline to isAuthenticated
                }.collectLatest { (isOnline, isAuthenticated) ->
                    Log.d(TAG, "State changed - Online: $isOnline, Authenticated: $isAuthenticated")
                    handleStateChange(isOnline, isAuthenticated)
                }
            } catch (e: Exception) {
                Log.e(TAG, "AutoSyncManager initialization error", e)
            }
        }
    }

    /**
     * Handle changes in network or authentication state
     */
    private suspend fun handleStateChange(isOnline: Boolean, isAuthenticated: Boolean) {
        try {
            when {
                // Online and authenticated - check for pending sync
                isOnline && isAuthenticated -> {
                    Log.d(TAG, "Device online and authenticated, checking for unsynced changes")
                    if (syncService.hasUnsyncedChanges()) {
                        Log.d(TAG, "Found unsynced changes, triggering sync")
                        triggerImmediateSync()
                    }
                    schedulePeriodicSync()
                }
                // Offline or not authenticated - cancel periodic sync
                else -> {
                    Log.d(TAG, "Device offline or not authenticated, cancelling periodic sync")
                    cancelPeriodicSync()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling state change", e)
        }
    }

    /**
     * Trigger immediate synchronization using simple worker
     */
    fun triggerImmediateSync() {
        try {
            Log.d(TAG, "Triggering immediate sync")
            val workManager = WorkManager.getInstance(context)

            // Використовуємо простий Worker замість Hilt Worker
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

            Log.d(TAG, "Immediate sync work enqueued")
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering immediate sync", e)
        }
    }

    /**
     * Schedule periodic background synchronization
     */
    private fun schedulePeriodicSync() {
        try {
            Log.d(TAG, "Scheduling periodic sync")
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

            Log.d(TAG, "Periodic sync work scheduled")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling periodic sync", e)
        }
    }

    /**
     * Cancel periodic synchronization
     */
    private fun cancelPeriodicSync() {
        try {
            Log.d(TAG, "Cancelling periodic sync")
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling periodic sync", e)
        }
    }

    /**
     * Cancel all sync operations
     */
    fun cancelAllSync() {
        try {
            Log.d(TAG, "Cancelling all sync operations")
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag(IMMEDIATE_SYNC_TAG)
            workManager.cancelAllWorkByTag(PERIODIC_SYNC_TAG)
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling all sync", e)
        }
    }

    /**
     * Create constraints for sync work
     */
    private fun createSyncConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false)
            .setRequiresCharging(false)
            .setRequiresDeviceIdle(false)
            .build()
    }

    /**
     * Get sync work info for monitoring
     */
    fun getSyncWorkInfo() = try {
        val workManager = WorkManager.getInstance(context)
        workManager.getWorkInfosByTagLiveData(IMMEDIATE_SYNC_TAG)
    } catch (e: Exception) {
        Log.e(TAG, "Error getting sync work info", e)
        null
    }
}