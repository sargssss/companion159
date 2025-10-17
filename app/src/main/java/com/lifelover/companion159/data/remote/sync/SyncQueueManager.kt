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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.encodeToJsonElement
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple and reliable sync queue manager
 *
 * Every database change goes into queue immediately
 * Worker processes queue in background
 */
@Singleton
class SyncQueueManager @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    private val json: Json,
    @ApplicationContext private val context: Context

) {
    companion object {
        private const val TAG = "SyncQueueManager"
    }

    /**
     * Trigger immediate sync worker after enqueue
     */
    private fun triggerWorker() {
        SyncWorker.triggerImmediateSync(context)
    }

    /**
     * Enqueue INSERT operation
     * Called after adding new item to local DB
     */
    suspend fun enqueueInsert(localItemId: Long, itemData: Map<String, Any>) = withContext(Dispatchers.IO) {
        try {
            val jsonItemData = buildJsonObject {
                itemData.forEach { (key, value) ->
                    put(key, json.encodeToJsonElement(value))
                }
            }

            val operation = SyncQueueEntity(
                operationType = SyncOperationType.INSERT,
                localItemId = localItemId,
                supabaseId = null,
                itemData = json.encodeToString(jsonItemData),
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
     */
    suspend fun enqueueUpdate(
        localItemId: Long,
        supabaseId: Long?,
        itemData: Map<String, Any>
    ) = withContext(Dispatchers.IO) {
        try {
            val jsonItemData = buildJsonObject {
                itemData.forEach { (key, value) ->
                    put(key, json.encodeToJsonElement(value))
                }
            }

            val operation = SyncQueueEntity(
                operationType = SyncOperationType.UPDATE,
                localItemId = localItemId,
                supabaseId = supabaseId,
                itemData = json.encodeToString(jsonItemData),
                status = SyncQueueStatus.PENDING
            )

            val queueId = syncQueueDao.enqueue(operation)
            Log.d(TAG, "✅ UPDATE queued: localId=$localItemId, supabaseId=$supabaseId, queueId=$queueId")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to enqueue UPDATE", e)
        }
    }

    /**
     * Enqueue DELETE operation
     * Called after soft-deleting item in local DB
     */
    suspend fun enqueueDelete(localItemId: Long, supabaseId: Long?) = withContext(Dispatchers.IO) {
        try {
            val operation = SyncQueueEntity(
                operationType = SyncOperationType.DELETE,
                localItemId = localItemId,
                supabaseId = supabaseId,
                itemData = null,
                status = SyncQueueStatus.PENDING
            )

            val queueId = syncQueueDao.enqueue(operation)
            Log.d(TAG, "✅ DELETE queued: localId=$localItemId, supabaseId=$supabaseId, queueId=$queueId")

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
}
