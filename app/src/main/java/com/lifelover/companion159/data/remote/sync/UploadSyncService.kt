package com.lifelover.companion159.data.remote.sync

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.dao.SyncDao
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.data.remote.api.SupabaseInventoryApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for uploading local changes to Supabase
 *
 * Strategy:
 * - Upload items with needsSync=true
 * - Batch operations for efficiency
 * - Update local records after successful sync
 */
@Singleton
class UploadSyncService @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val syncDao: SyncDao,
    private val supabaseApi: SupabaseInventoryApi,
    private val mapper: SyncMapper
) {
    companion object {
        private const val TAG = "UploadSyncService"
    }

    /**
     * Upload all items with needsSync=true
     */
    suspend fun uploadPendingChanges(
        userId: String?,
        crewName: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "========================================")
            Log.d(TAG, "uploadPendingChanges called")
            Log.d(TAG, "  userId: $userId")
            Log.d(TAG, "  crewName: $crewName")

            val uploadedCount = uploadNeedsSyncItems(userId, crewName)

            Log.d(TAG, "✅ Upload completed: $uploadedCount items")
            Log.d(TAG, "========================================")
            Result.success(uploadedCount)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload failed", e)
            Log.d(TAG, "========================================")
            Result.failure(e)
        }
    }

    /**
     * Upload items marked with needsSync=true
     */
    private suspend fun uploadNeedsSyncItems(userId: String?, crewName: String): Int {
        Log.d(TAG, "uploadNeedsSyncItems called")

        val needsSyncItems = syncDao.getItemsNeedingSync(userId, crewName)

        Log.d(TAG, "Found ${needsSyncItems.size} items with needsSync=true")

        if (needsSyncItems.isEmpty()) {
            Log.d(TAG, "No items to upload")
            return 0
        }

        // Log items for debugging
        needsSyncItems.forEachIndexed { index, item ->
            Log.d(TAG, "  [$index] ${item.itemName}")
            Log.d(TAG, "      id=${item.id}, supabaseId=${item.supabaseId}")
            Log.d(TAG, "      available=${item.availableQuantity}, needed=${item.neededQuantity}")
            Log.d(TAG, "      isActive=${item.isActive}")
        }

        var uploadedCount = 0

        // Split items: new (no supabaseId) vs existing (has supabaseId)
        val (itemsWithId, itemsWithoutId) = needsSyncItems.partition { it.supabaseId != null }

        Log.d(TAG, "Items with supabaseId: ${itemsWithId.size}")
        Log.d(TAG, "Items without supabaseId: ${itemsWithoutId.size}")

        // Insert new items
        if (itemsWithoutId.isNotEmpty()) {
            Log.d(TAG, "Batch inserting ${itemsWithoutId.size} new items...")
            uploadedCount += batchInsertItems(itemsWithoutId)
        }

        // Update existing items
        if (itemsWithId.isNotEmpty()) {
            Log.d(TAG, "Batch updating ${itemsWithId.size} existing items...")
            uploadedCount += batchUpdateItems(itemsWithId)
        }

        Log.d(TAG, "uploadNeedsSyncItems finished: $uploadedCount items uploaded")
        return uploadedCount
    }

    /**
     * Batch insert new items (without supabaseId)
     */
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
                            syncDao.updateSupabaseId(
                                localId = localItem.id,
                                supabaseId = serverId
                            )

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

    /**
     * Batch update existing items (with supabaseId)
     */
    private suspend fun batchUpdateItems(items: List<InventoryItemEntity>): Int {
        if (items.isEmpty()) return 0

        Log.d(TAG, "batchUpdateItems() - ${items.size} items")

        val dtos = items.map { mapper.entityToDto(it) }
        val result = supabaseApi.batchUpdate(dtos)

        return result.fold(
            onSuccess = { successCount ->
                Log.d(TAG, "✅ Batch updated $successCount items")

                items.forEach { item ->
                    try {
                        syncDao.markAsSynced(localId = item.id)
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
}