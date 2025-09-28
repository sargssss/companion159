package com.lifelover.companion159.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lifelover.companion159.network.NetworkMonitor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncService: SyncService,
    private val networkMonitor: NetworkMonitor
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "sync_inventory_work"
        const val RETRY_POLICY_DELAY = 30_000L // 30 seconds
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Check if device has internet connection
            if (!networkMonitor.isOnline) {
                return@withContext Result.failure()
            }

            // Perform synchronization
            val syncResult = syncService.performSync()

            syncResult.fold(
                onSuccess = {
                    Result.success()
                },
                onFailure = { error ->
                    // Log error for debugging
                    println("Sync failed: ${error.message}")

                    // Retry on network errors, fail on auth errors
                    when {
                        error.message?.contains("network", ignoreCase = true) == true ||
                                error.message?.contains("timeout", ignoreCase = true) == true -> {
                            Result.retry()
                        }
                        else -> Result.failure()
                    }
                }
            )
        } catch (e: Exception) {
            println("Sync worker exception: ${e.message}")
            Result.retry()
        }
    }
}