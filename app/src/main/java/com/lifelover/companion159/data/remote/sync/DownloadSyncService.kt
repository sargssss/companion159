package com.lifelover.companion159.data.remote.sync

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.data.remote.api.SupabaseInventoryApi
import com.lifelover.companion159.data.remote.dto.SupabaseInventoryItemDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for downloading changes from Supabase
 *
 * Uses polling strategy (no Realtime):
 * - Fetches items updated after last sync timestamp
 * - Merges changes into local DB
 * - Resolves conflicts using "last write wins" strategy
 */
@Singleton
class DownloadSyncService @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val supabaseApi: SupabaseInventoryApi,
    private val mapper: SyncMapper
) {
    companion object {
        private const val TAG = "DownloadSyncService"
        private const val PREF_LAST_SYNC_KEY = "last_download_sync_timestamp"
    }

    private val iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    // TODO: Move to SharedPreferences or Room
    private var lastSyncTimestamp: Date? = null

    /**
     * Download changes from Supabase for specific crew
     *
     * Strategy:
     * 1. Fetch items updated after last sync
     * 2. Merge into local DB with conflict resolution
     * 3. Update last sync timestamp
     *
     * @param crewName Crew name to sync
     * @param userId Current user ID
     * @param forceFullSync If true, fetch all items (ignore lastSync)
     * @return Number of downloaded items
     */
    suspend fun downloadChanges(
        crewName: String,
        userId: String?,
        forceFullSync: Boolean = false
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Starting download sync for crew: $crewName")

            val updatedAfter = if (forceFullSync) {
                null
            } else {
                lastSyncTimestamp?.let { iso8601Format.format(it) }
            }

            Log.d(TAG, "Fetching items updated after: $updatedAfter")

            // Fetch items from Supabase
            val result = supabaseApi.fetchItemsByCrewName(crewName, updatedAfter)

            result.fold(
                onSuccess = { remoteItems ->
                    Log.d(TAG, "Downloaded ${remoteItems.size} items")

                    if (remoteItems.isEmpty()) {
                        Log.d(TAG, "No changes from server")
                        return@withContext Result.success(0)
                    }

                    // Merge items into local DB
                    val mergedCount = mergeRemoteItems(remoteItems, userId)

                    // Update last sync timestamp
                    lastSyncTimestamp = Date()

                    Log.d(TAG, "✅ Download sync completed: $mergedCount items merged")
                    Result.success(mergedCount)
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ Download sync failed", error)
                    Result.failure(error)
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Download sync failed", e)
            Result.failure(e)
        }
    }

    /**
     * Merge remote items into local database
     * Prevents duplicates by checking supabaseId
     *
     * Conflict resolution: Last Write Wins
     */
    private suspend fun mergeRemoteItems(
        remoteItems: List<SupabaseInventoryItemDto>,
        userId: String?
    ): Int {
        var mergedCount = 0

        remoteItems.forEach { remoteDto ->
            try {
                val supabaseId = remoteDto.id ?: return@forEach

                // Find local item by supabaseId
                val localItem = inventoryDao.getItemBySupabaseId(supabaseId, userId)

                if (localItem == null) {
                    // New item from server - insert
                    val entity = mapper.dtoToEntity(remoteDto, userId, markAsSynced = true)
                    inventoryDao.insertItem(entity)
                    mergedCount++
                    Log.d(TAG, "✅ Inserted new item from server: ${remoteDto.itemName} (supabaseId=$supabaseId)")

                } else {
                    // Existing item - resolve conflict
                    val shouldUpdate = resolveConflict(localItem, remoteDto)

                    if (shouldUpdate) {
                        // Server version is newer - update local
                        val updatedEntity = mapper.updateEntityFromDto(localItem, remoteDto)

                        inventoryDao.updateItemWithNeeds(
                            id = updatedEntity.id,
                            name = updatedEntity.itemName,
                            availableQuantity = updatedEntity.availableQuantity,
                            neededQuantity = updatedEntity.neededQuantity,
                            category = updatedEntity.category,
                            crewName = updatedEntity.crewName
                        )

                        // Mark as synced
                        inventoryDao.markAsSynced(localId = updatedEntity.id)

                        mergedCount++
                        Log.d(TAG, "✅ Updated from server: ${remoteDto.itemName} (localId=${localItem.id})")
                    } else {
                        Log.d(TAG, "⏭️ Skipped (local is newer): ${remoteDto.itemName}")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to merge item: ${remoteDto.itemName}", e)
            }
        }

        return mergedCount
    }

    /**
     * Resolve conflict between local and remote item
     *
     * Strategy: Last Write Wins
     * - Compare lastModified (local) with updated_at (remote)
     * - Return true if remote is newer and should be applied
     *
     * @return true if remote should overwrite local, false otherwise
     */
    private fun resolveConflict(
        localItem: InventoryItemEntity,
        remoteDto: SupabaseInventoryItemDto
    ): Boolean {
        // Parse remote timestamp
        val remoteTimestamp = try {
            remoteDto.updatedAt?.let { iso8601Format.parse(it) }
        } catch (e: Exception) {
            null
        } ?: return false

        val localTimestamp = localItem.lastModified

        // Remote wins if timestamp is newer
        val remoteIsNewer = remoteTimestamp.after(localTimestamp)

        if (remoteIsNewer) {
            Log.d(TAG, "Conflict: Remote is newer (${remoteDto.itemName})")
            Log.d(TAG, "  Local: ${localTimestamp}, Remote: ${remoteTimestamp}")
        }

        return remoteIsNewer
    }

    /**
     * Perform initial full sync
     * Downloads all items for crew (ignores lastSync timestamp)
     */
    suspend fun performInitialSync(crewName: String, userId: String?): Result<Int> {
        Log.d(TAG, "Performing initial full sync")
        return downloadChanges(crewName, userId, forceFullSync = true)
    }

    /**
     * Reset last sync timestamp
     * Forces full sync on next download
     */
    fun resetLastSyncTimestamp() {
        lastSyncTimestamp = null
        Log.d(TAG, "Last sync timestamp reset")
    }

    /**
     * Get last sync timestamp
     */
    fun getLastSyncTimestamp(): Date? = lastSyncTimestamp
}