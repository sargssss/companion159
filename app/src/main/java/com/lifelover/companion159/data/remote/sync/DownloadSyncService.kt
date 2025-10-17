package com.lifelover.companion159.data.remote.sync

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.dao.SyncDao
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.data.remote.api.SupabaseInventoryApi
import com.lifelover.companion159.data.remote.dto.SupabaseInventoryItemDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadSyncService @Inject constructor(
    private val inventoryDao: InventoryDao,  // ← Для insertItem, updateItemWithNeeds
    private val syncDao: SyncDao,
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

    private var lastSyncTimestamp: Date? = null

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

            val result = supabaseApi.fetchItemsByCrewName(crewName, updatedAfter)

            result.fold(
                onSuccess = { remoteItems ->
                    Log.d(TAG, "Downloaded ${remoteItems.size} items")

                    if (remoteItems.isEmpty()) {
                        Log.d(TAG, "No changes from server")
                        return@withContext Result.success(0)
                    }

                    val mergedCount = mergeRemoteItems(remoteItems, userId)

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

    private suspend fun mergeRemoteItems(
        remoteItems: List<SupabaseInventoryItemDto>,
        userId: String?
    ): Int {
        var mergedCount = 0

        remoteItems.forEach { remoteDto ->
            try {
                val supabaseId = remoteDto.id ?: return@forEach

                val localItem = syncDao.getItemBySupabaseId(supabaseId, userId)

                if (localItem == null) {
                    val entity = mapper.dtoToEntity(remoteDto, userId, markAsSynced = true)
                    inventoryDao.insertItem(entity)
                    mergedCount++
                    Log.d(
                        TAG,
                        "✅ Inserted new item from server: ${remoteDto.itemName} (supabaseId=$supabaseId)"
                    )

                } else {
                    val shouldUpdate = resolveConflict(localItem, remoteDto)

                    if (shouldUpdate) {
                        val updatedEntity = mapper.updateEntityFromDto(localItem, remoteDto)

                        inventoryDao.updateItemWithNeeds(
                            id = updatedEntity.id,
                            name = updatedEntity.itemName,
                            availableQuantity = updatedEntity.availableQuantity,
                            neededQuantity = updatedEntity.neededQuantity,
                            category = updatedEntity.category,
                            crewName = updatedEntity.crewName
                        )

                        syncDao.markAsSynced(localId = updatedEntity.id)

                        mergedCount++
                        Log.d(
                            TAG,
                            "✅ Updated from server: ${remoteDto.itemName} (localId=${localItem.id})"
                        )
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

    private fun resolveConflict(
        localItem: InventoryItemEntity,
        remoteDto: SupabaseInventoryItemDto
    ): Boolean {
        val remoteTimestamp = try {
            remoteDto.updatedAt?.let { iso8601Format.parse(it) }
        } catch (e: Exception) {
            null
        } ?: return false

        val localTimestamp = localItem.lastModified

        val remoteIsNewer = remoteTimestamp.after(localTimestamp)

        if (remoteIsNewer) {
            Log.d(TAG, "Conflict: Remote is newer (${remoteDto.itemName})")
            Log.d(TAG, "  Local: ${localTimestamp}, Remote: ${remoteTimestamp}")
        }

        return remoteIsNewer
    }

    suspend fun performInitialSync(crewName: String, userId: String?): Result<Int> {
        Log.d(TAG, "Performing initial full sync")
        return downloadChanges(crewName, userId, forceFullSync = true)
    }

    fun resetLastSyncTimestamp() {
        lastSyncTimestamp = null
        Log.d(TAG, "Last sync timestamp reset")
    }

    fun getLastSyncTimestamp(): Date? = lastSyncTimestamp
}