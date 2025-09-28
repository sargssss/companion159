package com.lifelover.companion159.data.sync

import android.content.Context
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
    // Lazy initialization of WorkManager to avoid initialization issues
    private val workManager by lazy {
        try {
            WorkManager.getInstance(context)
        } catch (e: IllegalStateException) {
            // WorkManager not initialized yet, will retry when needed
            println("WorkManager not initialized yet: ${e.message}")
            null
        }
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var isInitialized = false

    /**
     * Initialize auto-sync monitoring
     * Starts observing network and auth state changes
     */
    fun initialize() {
        if (isInitialized) return
        isInitialized = true

        scope.launch {
            try {
                // Combine network status and auth status
                combine(
                    networkMonitor.isOnlineFlow,
                    authService.isAuthenticated
                ) { isOnline, isAuthenticated ->
                    isOnline to isAuthenticated
                }.collectLatest { (isOnline, isAuthenticated) ->
                    handleStateChange(isOnline, isAuthenticated)
                }
            } catch (e: Exception) {
                println("AutoSyncManager initialization error: ${e.message}")
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
                    if (syncService.hasUnsyncedChanges()) {
                        triggerImmediateSync()
                    }
                    schedulePeriodicSync()
                }

                // Offline or not authenticated - cancel periodic sync
                else -> {
                    cancelPeriodicSync()
                }
            }
        } catch (e: Exception) {
            println("Error handling state change: ${e.message}")
        }
    }

    /**
     * Trigger immediate synchronization
     * Called when data changes or network comes back online
     */
    fun triggerImmediateSync() {
        try {
            val workManagerInstance = workManager ?: run {
                println("WorkManager not available for immediate sync")
                return
            }

            val immediateWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(createSyncConstraints())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    SyncWorker.RETRY_POLICY_DELAY,
                    TimeUnit.MILLISECONDS
                )
                .addTag(IMMEDIATE_SYNC_TAG)
                .build()

            workManagerInstance.enqueueUniqueWork(
                IMMEDIATE_SYNC_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                immediateWorkRequest
            )
        } catch (e: Exception) {
            println("Error triggering immediate sync: ${e.message}")
        }
    }

    /**
     * Schedule periodic background synchronization
     */
    private fun schedulePeriodicSync() {
        try {
            val workManagerInstance = workManager ?: return

            val periodicWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES
            )
                .setConstraints(createSyncConstraints())
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    SyncWorker.RETRY_POLICY_DELAY,
                    TimeUnit.MILLISECONDS
                )
                .addTag(PERIODIC_SYNC_TAG)
                .build()

            workManagerInstance.enqueueUniquePeriodicWork(
                PERIODIC_SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
        } catch (e: Exception) {
            println("Error scheduling periodic sync: ${e.message}")
        }
    }

    /**
     * Cancel periodic synchronization
     */
    private fun cancelPeriodicSync() {
        try {
            workManager?.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
        } catch (e: Exception) {
            println("Error cancelling periodic sync: ${e.message}")
        }
    }

    /**
     * Cancel all sync operations
     */
    fun cancelAllSync() {
        try {
            workManager?.let { wm ->
                wm.cancelAllWorkByTag(IMMEDIATE_SYNC_TAG)
                wm.cancelAllWorkByTag(PERIODIC_SYNC_TAG)
            }
        } catch (e: Exception) {
            println("Error cancelling all sync: ${e.message}")
        }
    }

    /**
     * Create constraints for sync work
     * Only sync when connected to network
     */
    private fun createSyncConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false) // Allow sync even on low battery
            .setRequiresCharging(false)
            .setRequiresDeviceIdle(false)
            .build()
    }

    /**
     * Get sync work info for monitoring
     */
    fun getSyncWorkInfo() = try {
        workManager?.getWorkInfosByTagLiveData(IMMEDIATE_SYNC_TAG)
    } catch (e: Exception) {
        println("Error getting sync work info: ${e.message}")
        null
    }

    companion object {
        private const val IMMEDIATE_SYNC_WORK_NAME = "immediate_sync_work"
        private const val PERIODIC_SYNC_WORK_NAME = "periodic_sync_work"

        private const val IMMEDIATE_SYNC_TAG = "immediate_sync"
        private const val PERIODIC_SYNC_TAG = "periodic_sync"

        private const val SYNC_INTERVAL_MINUTES = 15L // Sync every 15 minutes
    }
}