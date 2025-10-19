package com.lifelover.companion159.data.remote.sync

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.dao.SyncDao
import com.lifelover.companion159.data.remote.api.SupabaseInventoryApi
import com.lifelover.companion159.data.remote.dto.SupabaseInventoryItemDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single sync service - maksimalno proste
 *
 * Робить 2 речі:
 * 1. Upload pending items to server
 * 2. Download server items to local DB
 */
@Singleton
class SimpleSyncService @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val syncDao: SyncDao,
    private val api: SupabaseInventoryApi
) {
    companion object {
        private const val TAG = "SimpleSyncService"

        // ✅ Helper для форматування дати
        private fun formatDate(date: java.util.Date?): String? {
            if (date == null) return null
            return java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", java.util.Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                .format(date)
        }
    }

    private var lastSyncTimestamp: Date? = null

    /**
     * Upload всіх items з needsSync=true
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

            // Process each item
            pendingItems.forEach { entity ->
                try {
                    // ✅ Log entity ПЕРЕД створенням DTO
                    Log.d(TAG, "Entity from DB:")
                    Log.d(TAG, "  id: ${entity.id}")
                    Log.d(TAG, "  itemName: ${entity.itemName}")
                    Log.d(TAG, "  availableQuantity: ${entity.availableQuantity}")
                    Log.d(TAG, "  neededQuantity: ${entity.neededQuantity}")
                    Log.d(TAG, "  priority: ${entity.priority}")
                    Log.d(TAG, "  category: ${entity.category}")
                    Log.d(TAG, "  isActive: ${entity.isActive}")
                    Log.d(TAG, "  supabaseId: ${entity.supabaseId}")

                    // Create DTO
                    val dto = SupabaseInventoryItemDto(
                        id = entity.supabaseId,
                        tenantId = 9999,
                        crewName = entity.crewName,
                        itemName = entity.itemName,
                        availableQuantity = entity.availableQuantity,
                        neededQuantity = entity.neededQuantity,
                        itemCategory = entity.category.name,
                        unit = "шт.",
                        priority = entity.priority,
                        isActive = entity.isActive,
                        createdAt = entity.createdAt?.time?.let {
                            java.text.SimpleDateFormat(
                                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                                java.util.Locale.US
                            )
                                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                                .format(it)
                        },
                        updatedAt = entity.lastModified?.time?.let {
                            java.text.SimpleDateFormat(
                                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                                java.util.Locale.US
                            )
                                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                                .format(it)
                        }
                    )

                    // Log DTO
                    val json = Json { prettyPrint = true }.encodeToString(dto)
                    Log.d(TAG, "📤 Sending to Supabase:\n$json")

                    // Upload
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
     * Download всіх items з сервера
     */
    suspend fun downloadServerItems(
        crewName: String,
        userId: String?,
        forceFullSync: Boolean = false
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "========== DOWNLOAD START ==========")

            val updatedAfter = if (forceFullSync) {
                null
            } else {
                lastSyncTimestamp?.let {
                    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", java.util.Locale.US)
                        .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                        .format(it)
                }
            }

            Log.d(TAG, "📥 Fetching from server for crew: $crewName")
            Log.d(TAG, "   Updated after: $updatedAfter")

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

            remoteItems.forEach { remoteDto ->
                try {
                    val supabaseId = remoteDto.id ?: return@forEach

                    val localItem = syncDao.getItemBySupabaseId(supabaseId, userId)

                    if (localItem == null) {
                        // New from server
                        val entity =
                            com.lifelover.companion159.data.local.entities.InventoryItemEntity(
                                id = 0,
                                supabaseId = supabaseId,
                                itemName = remoteDto.itemName,
                                availableQuantity = remoteDto.availableQuantity,
                                neededQuantity = remoteDto.neededQuantity,
                                category = com.lifelover.companion159.domain.models.StorageCategory.valueOf(
                                    remoteDto.itemCategory?.let {
                                        if (it == "БК") "AMMUNITION" else "EQUIPMENT"
                                    } ?: "EQUIPMENT"
                                ),
                                userId = userId,
                                crewName = remoteDto.crewName,
                                priority = remoteDto.priority,
                                isActive = remoteDto.isActive,
                                createdAt = Date(),
                                lastModified = Date(),
                                lastSynced = Date(),
                                needsSync = false
                            )

                        inventoryDao.insertItem(entity)
                        mergedCount++
                        Log.d(TAG, "✅ Inserted from server: ${remoteDto.itemName}")

                    } else {
                        // Update if server is newer
                        val remoteDate = remoteDto.updatedAt?.let {
                            try {
                                java.text.SimpleDateFormat(
                                    "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                                    java.util.Locale.US
                                )
                                    .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                                    .parse(it)
                            } catch (e: Exception) {
                                Date()
                            }
                        } ?: Date()

                        if (remoteDate.after(localItem.lastModified)) {
                            inventoryDao.updateItemWithNeeds(
                                id = localItem.id,
                                name = remoteDto.itemName,
                                availableQuantity = remoteDto.availableQuantity,
                                neededQuantity = remoteDto.neededQuantity,
                                category = com.lifelover.companion159.domain.models.StorageCategory.valueOf(
                                    remoteDto.itemCategory?.let {
                                        if (it == "БК") "AMMUNITION" else "EQUIPMENT"
                                    } ?: "EQUIPMENT"
                                ),
                                crewName = remoteDto.crewName
                            )
                            syncDao.markAsSynced(localItem.id)
                            mergedCount++
                            Log.d(TAG, "✅ Updated from server: ${remoteDto.itemName}")
                        } else {
                            Log.d(TAG, "⏭️ Skipped (local is newer): ${remoteDto.itemName}")
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error merging: ${remoteDto.itemName}", e)
                }
            }

            lastSyncTimestamp = Date()

            Log.d(TAG, "✅ Download completed: $mergedCount items merged")
            Log.d(TAG, "========== DOWNLOAD END ==========")
            Result.success(mergedCount)

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