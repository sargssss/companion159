package com.lifelover.companion159.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

/**
 * Background sync worker using WorkManager
 *
 * Performs periodic sync every 30 minutes
 * Also runs on network reconnection
 *
 * Features:
 * - Network constraint
 * - Exponential backoff retry
 *
 * NOTE: This worker is currently disabled until SyncManager is fully integrated
 * TODO: Add back @HiltWorker and inject SyncManager after DI setup
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
        private const val WORK_NAME = "inventory_sync_work"

        // Work tags
        const val TAG_PERIODIC = "sync_periodic"
        const val TAG_RECONNECT = "sync_reconnect"

        /**
         * Schedule periodic sync work
         * Runs every 30 minutes when device has network
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = 30,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
                flexTimeInterval = 15,
                flexTimeIntervalUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag(TAG_PERIODIC)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )

            Log.d(TAG, "✅ Periodic sync scheduled (every 30 minutes)")
        }

        /**
         * Schedule one-time sync on network reconnection
         * Processes offline queue immediately when connection restored
         */
        fun scheduleSyncOnReconnect(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag(TAG_RECONNECT)
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d(TAG, "✅ Reconnect sync scheduled")
        }

        /**
         * Cancel all sync work
         */
        fun cancelAllSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "❌ All sync work cancelled")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "🔄 Sync worker started")

            // TODO: Uncomment after SyncManager is integrated
            /*
            // Check if sync conditions are met
            if (!syncManager.canSync()) {
                Log.d(TAG, "⏭️ Sync conditions not met, skipping")
                return Result.success()
            }

            // Determine if this is a reconnect sync
            val isReconnect = tags.contains(TAG_RECONNECT)

            if (isReconnect) {
                Log.d(TAG, "📶 Reconnect sync")
                syncManager.syncOnReconnect()
            } else {
                Log.d(TAG, "⏰ Periodic sync")
                syncManager.sync()
            }

            // Wait for sync to complete
            kotlinx.coroutines.delay(5000)
            */

            Log.d(TAG, "✅ Sync worker completed (currently disabled)")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Sync worker failed", e)

            // Retry with exponential backoff
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}