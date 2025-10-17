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

    private suspend fun batchInsertItems(items: List<InventoryItemEntity>): Int {
        if (items.isEmpty()) return 0

        Log.d(TAG, "Batch inserting ${items.size} new items")
        val dtos = items.map { mapper.entityToDto(it) }
        val result = supabaseApi.batchInsert(dtos)

        return result.fold(
            onSuccess = { insertedItems ->
                Log.d(TAG, "✅ Server returned ${insertedItems.size} inserted items")

                var successCount = 0

                insertedItems.forEachIndexed { index, dto ->
                    try {
                        val localItem = items.getOrNull(index)
                        val serverId = dto.id

                        if (localItem != null && serverId != null) {
                            inventoryDao.updateSupabaseId(
                                localId = localItem.id,
                                supabaseId = serverId
                            )

                            // CRITICAL: Mark as synced
                            inventoryDao.markAsSynced(localItem.id)

                            successCount++
                            Log.d(TAG, "✅ Updated local item ${localItem.id} with server ID: $serverId")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to update supabaseId for item at index $index", e)
                    }
                }

                successCount
            },
            onFailure = { error ->
                Log.e(TAG, "❌ Batch insert failed", error)
                0
            }
        )
    }

    // Lines 250-276: Keep the fixed batchUpdateItems method from previous version
    private suspend fun batchUpdateItems(items: List<InventoryItemEntity>): Int {
        if (items.isEmpty()) return 0

        val dtos = items.map { mapper.entityToDto(it) }
        val result = supabaseApi.batchUpdate(dtos)

        return result.fold(
            onSuccess = { successCount ->
                Log.d(TAG, "✅ Batch updated $successCount items")

                items.forEach { item ->
                    try {
                        inventoryDao.markAsSynced(localId = item.id)
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to mark item ${item.id} as synced", e)
                    }
                }

                successCount
            },
            onFailure = { error ->
                Log.e(TAG, "❌ Batch update failed", error)
                0
            }
        )
    }

    suspend fun uploadPendingChanges(
        userId: String?,
        crewName: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "========================================")
            Log.d(TAG, "uploadPendingChanges called")
            Log.d(TAG, "  userId: $userId")
            Log.d(TAG, "  crewName: $crewName")

            var uploadedCount = 0

            Log.d(TAG, "Step 1: Processing offline queue...")
            uploadedCount += processOfflineQueue()

            Log.d(TAG, "Step 2: Uploading items with needsSync=true...")
            uploadedCount += uploadNeedsSyncItems(userId, crewName)

            Log.d(TAG, "✅ Upload completed: $uploadedCount items")
            Log.d(TAG, "========================================")
            Result.success(uploadedCount)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload failed", e)
            Log.d(TAG, "========================================")
            Result.failure(e)
        }
    }

    private suspend fun uploadNeedsSyncItems(userId: String?, crewName: String): Int {
        Log.d(TAG, "uploadNeedsSyncItems called")
        Log.d(TAG, "  userId: $userId")
        Log.d(TAG, "  crewName: $crewName")

        val needsSyncItems = inventoryDao.getItemsNeedingSync(userId, crewName)

        Log.d(TAG, "Found ${needsSyncItems.size} items with needsSync=true")

        needsSyncItems.forEachIndexed { index, item ->
            Log.d(TAG, "  [$index] Item: ${item.itemName}")
            Log.d(TAG, "      id=${item.id}, supabaseId=${item.supabaseId}")
            Log.d(TAG, "      available=${item.availableQuantity}, needed=${item.neededQuantity}")
            Log.d(TAG, "      needsSync=${item.needsSync}, crewName=${item.crewName}")
        }

        if (needsSyncItems.isEmpty()) {
            Log.d(TAG, "No items to upload")
            return 0
        }

        var uploadedCount = 0

        val (itemsWithId, itemsWithoutId) = needsSyncItems.partition { it.supabaseId != null }

        Log.d(TAG, "Items with supabaseId: ${itemsWithId.size}")
        Log.d(TAG, "Items without supabaseId: ${itemsWithoutId.size}")

        if (itemsWithoutId.isNotEmpty()) {
            Log.d(TAG, "Batch inserting ${itemsWithoutId.size} new items...")
            uploadedCount += batchInsertItems(itemsWithoutId)
        }

        if (itemsWithId.isNotEmpty()) {
            Log.d(TAG, "Batch updating ${itemsWithId.size} existing items...")
            uploadedCount += batchUpdateItems(itemsWithId)
        }

        Log.d(TAG, "uploadNeedsSyncItems finished: $uploadedCount items uploaded")
        return uploadedCount
    }
}