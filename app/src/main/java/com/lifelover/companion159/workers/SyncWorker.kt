package com.lifelover.companion159.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.remote.sync.SyncQueueProcessor
import com.lifelover.companion159.data.repository.PositionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Worker for processing sync queue
 *
 * Runs periodically to upload pending changes
 * Also runs immediately when queue has items
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncQueueProcessor: SyncQueueProcessor,
    private val authService: SupabaseAuthService,
    private val positionRepository: PositionRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
        private const val WORK_NAME = "inventory_sync_work"

        /**
         * Schedule periodic sync work
         * Runs every 15 minutes when device has network
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )

            Log.d(TAG, "✅ Periodic sync scheduled (every 15 minutes)")
        }

        /**
         * Trigger immediate one-time sync
         * Called after database changes
         */
        fun triggerImmediateSync(context: Context) {
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
                .build()

            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d(TAG, "✅ Immediate sync triggered")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "🔄 Sync worker started")

            // Check auth
            if (!authService.isUserAuthenticated()) {
                Log.d(TAG, "⏭️ User not authenticated, skipping")
                return Result.success()
            }

            // Check position
            val crewName = positionRepository.getPosition()
            if (crewName == null) {
                Log.d(TAG, "⏭️ Position not set, skipping")
                return Result.success()
            }

            val userId = authService.getUserId()

            // Process queue
            val result = syncQueueProcessor.processQueue(userId, crewName)

            result.fold(
                onSuccess = { processedCount ->
                    Log.d(TAG, "✅ Sync worker completed: $processedCount operations processed")
                    Result.success()
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Sync worker failed", error)

                    // Retry with exponential backoff
                    if (runAttemptCount < 3) {
                        Result.retry()
                    } else {
                        Result.failure()
                    }
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Sync worker failed", e)

            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}