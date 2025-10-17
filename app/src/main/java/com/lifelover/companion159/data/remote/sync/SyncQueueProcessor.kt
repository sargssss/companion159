package com.lifelover.companion159.data.remote.sync

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.dao.SyncDao
import com.lifelover.companion159.data.local.dao.SyncQueueDao
import com.lifelover.companion159.data.local.entities.SyncOperationType
import com.lifelover.companion159.data.local.entities.SyncQueueEntity
import com.lifelover.companion159.data.remote.api.SupabaseInventoryApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Processes sync queue operations
 *
 * Handles:
 * - INSERT → upload to Supabase
 * - UPDATE → update in Supabase
 * - DELETE → soft delete in Supabase
 *
 * Simplified approach:
 * - Always fetches fresh data from local DB
 * - No serialization/deserialization needed
 */
@Singleton
class SyncQueueProcessor @Inject constructor(
    private val syncQueueDao: SyncQueueDao,
    private val inventoryDao: InventoryDao,
    private val syncDao: SyncDao,
    private val supabaseApi: SupabaseInventoryApi,
    private val mapper: SyncMapper
) {
    companion object {
        private const val TAG = "SyncQueueProcessor"
        private const val MAX_RETRIES = 3
    }

    /**
     * Process all pending operations in queue
     * Returns number of successfully processed operations
     */
    suspend fun processQueue(userId: String?, crewName: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "========================================")
            Log.d(TAG, "Processing sync queue for crew: $crewName")

            val pendingOps = syncQueueDao.getAllPending()
            Log.d(TAG, "Found ${pendingOps.size} pending operations")

            if (pendingOps.isEmpty()) {
                Log.d(TAG, "Queue is empty, nothing to process")
                return@withContext Result.success(0)
            }

            var successCount = 0

            pendingOps.forEach { operation ->
                try {
                    Log.d(TAG, "----------------------------------------")
                    Log.d(TAG, "Processing: ${operation.operationType} (id=${operation.id})")

                    // Mark as processing
                    syncQueueDao.markAsProcessing(operation.id)

                    // Process based on type
                    val result = when (operation.operationType) {
                        SyncOperationType.INSERT -> processInsert(operation, userId, crewName)
                        SyncOperationType.UPDATE -> processUpdate(operation, userId, crewName)
                        SyncOperationType.DELETE -> processDelete(operation)
                        else -> {
                            Log.e(TAG, "Unknown operation type: ${operation.operationType}")
                            false
                        }
                    }

                    if (result) {
                        // Success - remove from queue
                        syncQueueDao.remove(operation.id)
                        successCount++
                        Log.d(TAG, "✅ Operation completed and removed from queue")
                    } else {
                        // Failed - mark as failed with retry
                        if (operation.retryCount >= MAX_RETRIES) {
                            syncQueueDao.markAsFailed(operation.id, "Max retries reached")
                            Log.e(TAG, "❌ Max retries reached, marking as failed")
                        } else {
                            syncQueueDao.markAsFailed(operation.id, "Processing failed, will retry")
                            Log.w(TAG, "⚠️ Operation failed, retry ${operation.retryCount + 1}/$MAX_RETRIES")
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing operation ${operation.id}", e)
                    syncQueueDao.markAsFailed(operation.id, e.message ?: "Unknown error")
                }
            }

            Log.d(TAG, "✅ Queue processed: $successCount/${pendingOps.size} operations successful")
            Log.d(TAG, "========================================")

            Result.success(successCount)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Queue processing failed", e)
            Result.failure(e)
        }
    }

    /**
     * Process INSERT operation
     * Fetches item from DB and uploads to Supabase
     */
    private suspend fun processInsert(
        operation: SyncQueueEntity,
        userId: String?,
        crewName: String
    ): Boolean {
        try {
            // Fetch item from DB
            val localItem = inventoryDao.getItemById(operation.localItemId)
            if (localItem == null) {
                Log.w(TAG, "Local item not found: ${operation.localItemId}")
                return true // Remove from queue - item doesn't exist anymore
            }

            // Convert to DTO
            val dto = mapper.entityToDto(localItem)

            Log.d(TAG, "Inserting to Supabase: ${localItem.itemName}")

            // Insert to Supabase
            val result = supabaseApi.insertItem(dto)

            return result.fold(
                onSuccess = { insertedDto ->
                    val supabaseId = insertedDto.id
                    if (supabaseId != null) {
                        // Update local item with Supabase ID
                        syncDao.updateSupabaseId(localItem.id, supabaseId)
                        Log.d(TAG, "✅ INSERT successful, supabaseId=$supabaseId")
                        true
                    } else {
                        Log.e(TAG, "❌ INSERT returned null ID")
                        false
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ INSERT failed: ${error.message}")
                    false
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ INSERT processing failed", e)
            return false
        }
    }

    /**
     * Process UPDATE operation
     * Fetches item from DB and updates in Supabase
     */
    private suspend fun processUpdate(
        operation: SyncQueueEntity,
        userId: String?,
        crewName: String
    ): Boolean {
        try {
            // Fetch item from DB
            val localItem = inventoryDao.getItemById(operation.localItemId)
            if (localItem == null) {
                Log.w(TAG, "Local item not found: ${operation.localItemId}")
                return true // Remove from queue - item doesn't exist anymore
            }

            // Need Supabase ID to update
            if (localItem.supabaseId == null) {
                Log.w(TAG, "Item has no Supabase ID, treating as INSERT")
                return processInsert(operation, userId, crewName)
            }

            // Convert to DTO
            val dto = mapper.entityToDto(localItem)

            Log.d(TAG, "Updating in Supabase: ${localItem.itemName} (id=${localItem.supabaseId})")

            // Update in Supabase
            val result = supabaseApi.updateItem(dto)

            return result.fold(
                onSuccess = {
                    // Mark as synced
                    syncDao.markAsSynced(localItem.id)
                    Log.d(TAG, "✅ UPDATE successful")
                    true
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ UPDATE failed: ${error.message}")
                    false
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ UPDATE processing failed", e)
            return false
        }
    }

    /**
     * Process DELETE operation
     */
    private suspend fun processDelete(operation: SyncQueueEntity): Boolean {
        try {
            val supabaseId = operation.supabaseId

            if (supabaseId == null) {
                Log.w(TAG, "DELETE operation has no Supabase ID, skipping")
                return true // Remove from queue - nothing to delete on server
            }

            Log.d(TAG, "Deleting from Supabase: supabaseId=$supabaseId")

            // Delete from Supabase
            val result = supabaseApi.deleteItem(supabaseId)

            return result.fold(
                onSuccess = {
                    Log.d(TAG, "✅ DELETE successful")
                    true
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ DELETE failed: ${error.message}")
                    false
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ DELETE processing failed", e)
            return false
        }
    }
}