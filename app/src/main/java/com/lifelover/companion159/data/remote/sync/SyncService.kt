package com.lifelover.companion159.data.remote.sync

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.dao.SyncDao
import com.lifelover.companion159.data.remote.api.SupabaseInventoryApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates synchronization between local DB and remote server
 *
 * Responsibilities:
 * - Upload pending local changes (needsSync=1)
 * - Download server changes (forceFullSync or since lastSyncTime)
 * - Merge conflict resolution (remote vs local timestamps)
 * - Handle deleted items (isActive=false)
 *
 * Does NOT handle:
 * - Data transformations (delegated to SyncMapper)
 * - Date formatting (delegated to SyncDateUtils)
 * - Connectivity checks (delegated to SyncOrchestrator)
 */
@Singleton
class SyncService @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val syncDao: SyncDao,
    private val api: SupabaseInventoryApi
) {
    companion object {
        private const val TAG = "SyncService"
    }

    private var lastSyncTimestamp: Date? = null

    /**
     * Upload all pending items (needsSync=1) to server
     *
     * For each pending item:
     * - Transform to DTO using SyncMapper
     * - INSERT if new (supabaseId=null)
     * - UPDATE if existing (supabaseId set)
     * - Mark as synced on success
     *
     * @param userId Current authenticated user ID
     * @param crewName User's crew/position for filtering
     * @return Number of items successfully uploaded
     */
    suspend fun uploadPendingItems(
        userId: String?,
        crewName: String
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "========== UPLOAD START ==========")

            val pendingItems = syncDao.getItemsNeedingSync(userId, crewName)
            Log.d(TAG, "📦 Found ${pendingItems.size} items with needsSync=true")

            if (pendingItems.isEmpty()) {
                Log.d(TAG, "✅ Nothing to upload")
                Log.d(TAG, "========== UPLOAD END ==========")
                return@withContext Result.success(0)
            }

            var uploadedCount = 0

            // Process each pending item
            pendingItems.forEach { entity ->
                try {
                    Log.d(TAG, "Processing entity: ${entity.itemName}")

                    // Transform using mapper (all conversion logic centralized)
                    val dto = SyncMapper.entityToDto(entity)

                    // Log DTO for debugging sync issues
                    val json = Json { prettyPrint = true }.encodeToString(dto)
                    Log.d(TAG, "📤 Sending to Supabase:\n$json")

                    // Upload (new vs existing)
                    if (dto.id == null) {
                        // New item - INSERT
                        Log.d(TAG, "➕ INSERT: ${entity.itemName}")
                        val result = api.insertSingleItem(dto)

                        result.fold(
                            onSuccess = { inserted ->
                                syncDao.updateSupabaseId(entity.id, inserted.id!!)
                                syncDao.markAsSynced(entity.id)
                                uploadedCount++
                                Log.d(TAG, "✅ Inserted with ID: ${inserted.id}")
                            },
                            onFailure = { e ->
                                Log.e(TAG, "❌ Insert failed: ${e.message}")
                            }
                        )
                    } else {
                        // Existing item - UPDATE
                        Log.d(TAG, "🔄 UPDATE: ${entity.itemName} (ID: ${dto.id})")
                        val result = api.updateSingleItem(dto)

                        result.fold(
                            onSuccess = {
                                syncDao.markAsSynced(entity.id)
                                uploadedCount++
                                Log.d(TAG, "✅ Updated")
                            },
                            onFailure = { e ->
                                Log.e(TAG, "❌ Update failed: ${e.message}")
                            }
                        )
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error processing item: ${entity.itemName}", e)
                }
            }

            Log.d(TAG, "✅ Upload completed: $uploadedCount/${pendingItems.size} items")
            Log.d(TAG, "========== UPLOAD END ==========")
            Result.success(uploadedCount)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload failed", e)
            Log.d(TAG, "========== UPLOAD END ==========")
            Result.failure(e)
        }
    }

    /**
     * Download all items from server and merge with local DB
     *
     * Sync strategy (timestamp-based incremental):
     * - Full sync (forceFullSync=true): fetch ALL server items, update local completely
     * - Incremental: fetch only items modified since lastSyncTimestamp
     *
     * Merge logic for each remote item:
     * 1. If local doesn't exist → INSERT (new from server)
     * 2. If local exists and remote is newer → UPDATE local with remote
     * 3. If local exists and local is newer → KEEP local (will re-upload on next sync)
     * 4. If remote is deleted (isActive=false) → SOFT DELETE local
     *
     * @param crewName Filter by crew/position
     * @param userId Current user ID
     * @param forceFullSync If true, fetch all items; if false, only modified since lastSync
     * @return Number of items processed (merged + deleted)
     */
    suspend fun downloadServerItems(
        crewName: String,
        userId: String?,
        forceFullSync: Boolean = false
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "========== DOWNLOAD START ==========")

            // Determine filter: full sync or incremental since last sync
            val updatedAfter = if (forceFullSync) {
                null
            } else {
                lastSyncTimestamp?.let { SyncDateUtils.formatForSync(it) }
            }

            Log.d(TAG, "📥 Fetching from server for crew: $crewName")
            Log.d(TAG, "   Updated after: $updatedAfter")
            Log.d(TAG, "   Force full sync: $forceFullSync")

            // Fetch from server
            val remoteItems = api.fetchItemsByCrewName(crewName, updatedAfter)
                .getOrElse { error ->
                    Log.e(TAG, "❌ Fetch failed: ${error.message}")
                    return@withContext Result.failure(error)
                }

            Log.d(TAG, "📦 Got ${remoteItems.size} items from server")

            if (remoteItems.isEmpty()) {
                Log.d(TAG, "✅ No server changes")
                Log.d(TAG, "========== DOWNLOAD END ==========")
                return@withContext Result.success(0)
            }

            var mergedCount = 0
            var deletedCount = 0

            // Process each remote item
            remoteItems.forEach { remoteDto ->
                try {
                    val supabaseId = remoteDto.id ?: return@forEach

                    // Handle deleted items from server
                    if (!remoteDto.isActive) {
                        Log.d(TAG, "🗑️ Processing deleted item: ${remoteDto.itemName}")

                        val localItem = syncDao.getItemBySupabaseId(supabaseId, userId)
                        if (localItem != null && localItem.isActive) {
                            inventoryDao.softDeleteItem(localItem.id)
                            syncDao.markAsSynced(localItem.id)
                            deletedCount++
                            Log.d(TAG, "✅ Soft deleted: ${remoteDto.itemName}")
                        }
                        return@forEach // Skip other processing for deleted items
                    }

                    // Find existing local item
                    val localItem = syncDao.getItemBySupabaseId(supabaseId, userId)

                    if (localItem == null) {
                        // New from server - INSERT
                        val entity = SyncMapper.dtoToNewEntity(remoteDto, userId)
                        inventoryDao.insertItem(entity)
                        mergedCount++
                        Log.d(TAG, "✅ Inserted from server: ${remoteDto.itemName}")

                    } else {
                        // Existing locally - check if remote is newer (MERGE CONFLICT RESOLUTION)
                        if (SyncMapper.isRemoteNewer(localItem.lastModified, remoteDto.updatedAt)) {
                            // Remote is newer - UPDATE local with remote
                            val updatedEntity = SyncMapper.dtoToExistingEntity(localItem, remoteDto)
                            inventoryDao.updateItemWithNeeds(
                                id = updatedEntity.id,
                                name = updatedEntity.itemName,
                                availableQuantity = updatedEntity.availableQuantity,
                                neededQuantity = updatedEntity.neededQuantity,
                                category = updatedEntity.category,
                                crewName = updatedEntity.crewName
                            )
                            syncDao.markAsSynced(updatedEntity.id)
                            mergedCount++
                            Log.d(TAG, "✅ Updated from server (remote is newer): ${remoteDto.itemName}")
                        } else {
                            // Local is newer - KEEP local (will re-upload on next sync)
                            Log.d(TAG, "⏭️ Skipped (local is newer): ${remoteDto.itemName}")
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error merging: ${remoteDto.itemName}", e)
                }
            }

            // Update last sync timestamp for next incremental sync
            lastSyncTimestamp = Date()

            Log.d(TAG, "✅ Download completed: $mergedCount merged, $deletedCount deleted")
            Log.d(TAG, "========== DOWNLOAD END ==========")
            Result.success(mergedCount + deletedCount)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Download failed", e)
            Log.d(TAG, "========== DOWNLOAD END ==========")
            Result.failure(e)
        }
    }

    fun resetLastSyncTimestamp() {
        lastSyncTimestamp = null
    }

    fun getLastSyncTimestamp(): Date? = lastSyncTimestamp
}