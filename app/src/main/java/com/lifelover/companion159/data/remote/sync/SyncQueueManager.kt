package com.lifelover.companion159.data.remote.sync

import android.content.Context
import android.util.Log
import com.lifelover.companion159.data.local.dao.SyncQueueDao
import com.lifelover.companion159.data.local.entities.SyncOperationType
import com.lifelover.companion159.data.local.entities.SyncQueueEntity
import com.lifelover.companion159.data.local.entities.SyncQueueStatus
import com.lifelover.companion159.workers.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple and reliable sync queue manager
 *
 * Every database change goes into queue immediately
 * Worker processes queue in background
 *
 * Simplified approach:
 * - Only stores item IDs in queue
 * - Fetches actual data from DB when processing
 * - No complex serialization needed
 */
@Singleton
class SyncQueueManager @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "SyncQueueManager"
    }

    /**
     * Enqueue INSERT operation
     * Called after adding new item to local DB
     *
     * @param localItemId ID of newly created item
     */
    suspend fun enqueueInsert(localItemId: Long) = withContext(Dispatchers.IO) {
        try {
            val operation = SyncQueueEntity(
                operationType = SyncOperationType.INSERT,
                localItemId = localItemId,
                supabaseId = null,
                itemData = null, // No data needed - will fetch from DB
                status = SyncQueueStatus.PENDING
            )

            val queueId = syncQueueDao.enqueue(operation)
            Log.d(TAG, "✅ INSERT queued: localId=$localItemId, queueId=$queueId")

            // Trigger worker immediately
            triggerWorker()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to enqueue INSERT", e)
        }
    }

    /**
     * Enqueue UPDATE operation
     * Called after updating existing item in local DB
     *
     * @param localItemId ID of updated item
     * @param supabaseId Supabase ID (if synced before)
     */
    suspend fun enqueueUpdate(
        localItemId: Long,
        supabaseId: Long?
    ) = withContext(Dispatchers.IO) {
        try {
            val operation = SyncQueueEntity(
                operationType = SyncOperationType.UPDATE,
                localItemId = localItemId,
                supabaseId = supabaseId,
                itemData = null, // No data needed - will fetch from DB
                status = SyncQueueStatus.PENDING
            )

            val queueId = syncQueueDao.enqueue(operation)
            Log.d(TAG, "✅ UPDATE queued: localId=$localItemId, supabaseId=$supabaseId, queueId=$queueId")

            // Trigger worker immediately
            triggerWorker()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to enqueue UPDATE", e)
        }
    }

    /**
     * Enqueue DELETE operation
     * Called after soft-deleting item in local DB
     *
     * @param localItemId ID of deleted item
     * @param supabaseId Supabase ID (needed for server deletion)
     */
    suspend fun enqueueDelete(localItemId: Long, supabaseId: Long?) = withContext(Dispatchers.IO) {
        try {
            val operation = SyncQueueEntity(
                operationType = SyncOperationType.DELETE,
                localItemId = localItemId,
                supabaseId = supabaseId,
                itemData = null, // No data needed
                status = SyncQueueStatus.PENDING
            )

            val queueId = syncQueueDao.enqueue(operation)
            Log.d(TAG, "✅ DELETE queued: localId=$localItemId, supabaseId=$supabaseId, queueId=$queueId")

            // Trigger worker immediately
            triggerWorker()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to enqueue DELETE", e)
        }
    }

    /**
     * Get queue size (for UI display)
     */
    suspend fun getQueueSize(): Int = withContext(Dispatchers.IO) {
        syncQueueDao.getQueueSize()
    }

    /**
     * Check if queue is empty
     */
    suspend fun isQueueEmpty(): Boolean = withContext(Dispatchers.IO) {
        syncQueueDao.getQueueSize() == 0
    }

    /**
     * Trigger immediate sync worker after enqueue
     */
    private fun triggerWorker() {
        SyncWorker.triggerImmediateSync(context)
    }
}