package com.lifelover.companion159.data.remote.sync

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.dao.SyncQueueDao
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.data.local.entities.SyncOperationType
import com.lifelover.companion159.data.local.entities.SyncQueueEntity
import com.lifelover.companion159.data.remote.api.SupabaseInventoryApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for uploading local changes to Supabase
 *
 * Responsibilities:
 * - Upload items with needsSync = true
 * - Process offline queue operations
 * - Handle batch uploads for efficiency
 * - Update local records after successful sync
 */
@Singleton
class UploadSyncService @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val syncQueueDao: SyncQueueDao,
    private val supabaseApi: SupabaseInventoryApi,
    private val mapper: SyncMapper
) {
    companion object {
        private const val TAG = "UploadSyncService"
        private const val BATCH_SIZE = 50
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Upload all pending changes to Supabase
     *
     * Strategy:
     * 1. Process offline queue first (FIFO order)
     * 2. Upload items with needsSync = true
     * 3. Update local DB after successful uploads
     *
     * @param userId Current user ID
     * @return Number of successfully uploaded items
     */
    suspend fun uploadPendingChanges(userId: String?): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Starting upload sync...")

            var uploadedCount = 0

            // Step 1: Process offline queue
            uploadedCount += processOfflineQueue()

            // Step 2: Upload items marked for sync
            uploadedCount += uploadNeedsSyncItems(userId)

            Log.d(TAG, "✅ Upload sync completed: $uploadedCount items")
            Result.success(uploadedCount)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload sync failed", e)
            Result.failure(e)
        }
    }

    /**
     * Process operations from offline queue
     * FIFO order: oldest operations first
     */
    private suspend fun processOfflineQueue(): Int {
        val pendingOperations = syncQueueDao.getAllPending()
        if (pendingOperations.isEmpty()) {
            Log.d(TAG, "No pending queue operations")
            return 0
        }

        Log.d(TAG, "Processing ${pendingOperations.size} queue operations")
        var successCount = 0

        pendingOperations.forEach { operation ->
            try {
                syncQueueDao.markAsProcessing(operation.id)

                val success = when (operation.operationType) {
                    SyncOperationType.INSERT -> processInsertOperation(operation)
                    SyncOperationType.UPDATE -> processUpdateOperation(operation)
                    SyncOperationType.DELETE -> processDeleteOperation(operation)
                    else -> false
                }

                if (success) {
                    syncQueueDao.remove(operation.id)
                    successCount++
                } else {
                    syncQueueDao.markAsFailed(operation.id, "Operation failed")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Queue operation failed: ${operation.id}", e)
                syncQueueDao.markAsFailed(operation.id, e.message ?: "Unknown error")
            }
        }

        Log.d(TAG, "Processed $successCount/${pendingOperations.size} queue operations")
        return successCount
    }

    /**
     * Process INSERT operation from queue
     * Saves server ID after successful insert
     */
    private suspend fun processInsertOperation(operation: SyncQueueEntity): Boolean {
        val localItem = inventoryDao.getItemById(operation.localItemId) ?: return false

        // Check if already has supabaseId (already synced)
        if (localItem.supabaseId != null) {
            Log.d(TAG, "Item ${localItem.id} already has supabaseId, skipping insert")
            return true
        }

        val dto = mapper.entityToDto(localItem)
        val result = supabaseApi.insertItem(dto)

        return result.fold(
            onSuccess = { insertedItem ->
                val serverId = insertedItem.id
                if (serverId != null) {
                    // Update local item with server ID
                    inventoryDao.updateSupabaseId(
                        localId = localItem.id,
                        supabaseId = serverId
                    )
                    Log.d(TAG, "✅ Queue INSERT: Updated item ${localItem.id} with server ID $serverId")
                    true
                } else {
                    Log.e(TAG, "❌ Queue INSERT: Server didn't return ID")
                    false
                }
            },
            onFailure = { error ->
                Log.e(TAG, "❌ Queue INSERT failed", error)
                false
            }
        )
    }

    /**
     * Process UPDATE operation from queue
     * Marks item as synced after successful update
     */
    private suspend fun processUpdateOperation(operation: SyncQueueEntity): Boolean {
        val localItem = inventoryDao.getItemById(operation.localItemId) ?: return false

        // Check if item has supabaseId
        if (localItem.supabaseId == null) {
            Log.w(TAG, "Item ${localItem.id} has no supabaseId, cannot update on server")
            // Should insert instead
            return processInsertOperation(operation)
        }

        val dto = mapper.entityToDto(localItem)
        val result = supabaseApi.updateItem(dto)

        return result.fold(
            onSuccess = {
                // Mark as synced
                inventoryDao.markAsSynced(localId = localItem.id)
                Log.d(TAG, "✅ Queue UPDATE: Item ${localItem.id} synced")
                true
            },
            onFailure = { error ->
                Log.e(TAG, "❌ Queue UPDATE failed", error)
                false
            }
        )
    }

    /**
     * Process DELETE operation from queue
     */
    private suspend fun processDeleteOperation(operation: SyncQueueEntity): Boolean {
        val supabaseId = operation.supabaseId ?: return false

        val result = supabaseApi.deleteItem(supabaseId)
        return result.isSuccess
    }

    /**
     * Upload items with needsSync = true
     * Processes in batches for efficiency
     */
    private suspend fun uploadNeedsSyncItems(userId: String?): Int {
        // Get all items needing sync
        val allItems = inventoryDao.getAllItems(userId)
        val needsSyncItems = allItems.filter { it.needsSync && it.isActive }

        if (needsSyncItems.isEmpty()) {
            Log.d(TAG, "No items need sync")
            return 0
        }

        Log.d(TAG, "Uploading ${needsSyncItems.size} items needing sync")
        var uploadedCount = 0

        // Split into items with/without server IDs
        val (itemsWithId, itemsWithoutId) = needsSyncItems.partition { it.supabaseId != null }

        // Batch insert new items
        if (itemsWithoutId.isNotEmpty()) {
            uploadedCount += batchInsertItems(itemsWithoutId)
        }

        // Update existing items
        if (itemsWithId.isNotEmpty()) {
            uploadedCount += batchUpdateItems(itemsWithId)
        }

        return uploadedCount
    }

    /**
     * Batch insert items without server IDs
     * Updates local items with server IDs after successful insert
     */
    private suspend fun batchInsertItems(items: List<InventoryItemEntity>): Int {
        if (items.isEmpty()) return 0

        Log.d(TAG, "Batch inserting ${items.size} new items")
        val dtos = items.map { mapper.entityToDto(it) }
        val result = supabaseApi.batchInsert(dtos)

        return result.fold(
            onSuccess = { insertedItems ->
                Log.d(TAG, "✅ Server returned ${insertedItems.size} inserted items")

                // Update local items with server IDs
                insertedItems.forEachIndexed { index, dto ->
                    try {
                        val localItem = items.getOrNull(index)
                        val serverId = dto.id

                        if (localItem != null && serverId != null) {
                            // Update supabaseId in local DB
                            inventoryDao.updateSupabaseId(
                                localId = localItem.id,
                                supabaseId = serverId
                            )
                            Log.d(TAG, "✅ Updated local item ${localItem.id} with server ID: $serverId")
                        } else {
                            Log.w(TAG, "⚠️ Cannot update item: localItem=$localItem, serverId=$serverId")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to update supabaseId for item at index $index", e)
                    }
                }

                insertedItems.size
            },
            onFailure = { error ->
                Log.e(TAG, "❌ Batch insert failed", error)
                0
            }
        )
    }
    /**
     * Batch update items with server IDs
     */
    private suspend fun batchUpdateItems(items: List<InventoryItemEntity>): Int {
        val dtos = items.map { mapper.entityToDto(it) }
        val result = supabaseApi.batchUpdate(dtos)

        return result.fold(
            onSuccess = { count ->
                // Mark items as synced
                // TODO: Update needsSync for successful items
                count
            },
            onFailure = { 0 }
        )
    }
}