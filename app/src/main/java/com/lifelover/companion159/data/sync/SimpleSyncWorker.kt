package com.lifelover.companion159.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lifelover.companion159.di.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Простий Worker з доступом до Hilt залежностей через ServiceLocator
 */
class SimpleSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SimpleSyncWorker"
        const val WORK_NAME = "simple_sync_inventory_work"
        const val RETRY_POLICY_DELAY = 30_000L
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "SimpleSyncWorker started")

            // Отримуємо SyncService через ServiceLocator
            val syncService = ServiceLocator.getSyncService(applicationContext)

            // Виконуємо синхронізацію
            val result = syncService.performSync()

            result.fold(
                onSuccess = {
                    Log.d(TAG, "Sync completed successfully")
                    Result.success()
                },
                onFailure = { error ->
                    Log.e(TAG, "Sync failed: ${error.message}")
                    // Повторити при помилці мережі
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
            Log.e(TAG, "Worker exception", e)
            Result.retry()
        }
    }
}