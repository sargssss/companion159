package com.lifelover.companion159.data.sync

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.remote.models.CrewInventoryItem
import com.lifelover.companion159.data.remote.repository.SupabaseInventoryRepository
import com.lifelover.companion159.data.repository.PositionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
    OFFLINE
}

@Singleton
class SyncService @Inject constructor(
    private val localDao: InventoryDao,
    private val remoteRepository: SupabaseInventoryRepository,
    private val authService: SupabaseAuthService,
    private val positionRepository: PositionRepository
) {
    companion object {
        private const val TAG = "SyncService"
        private val DEFAULT_CATEGORY = InventoryCategory.EQUIPMENT  // "інвентар"
    }

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    suspend fun hasOfflineItems(): Boolean = withContext(Dispatchers.IO) {
        try {
            val offlineItems = localDao.getOfflineItems()
            val hasItems = offlineItems.isNotEmpty()
            Log.d(TAG, "📦 Offline items count: ${offlineItems.size}")
            hasItems
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking offline items", e)
            false
        }
    }

    suspend fun performSync(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Starting sync...")
            _syncStatus.value = SyncStatus.SYNCING

            val userId = authService.getUserIdForSync()
            if (userId == null) {
                Log.w(TAG, "⚠️ No user found - cannot sync")
                _syncStatus.value = SyncStatus.ERROR
                return@withContext Result.failure(Exception("No user available for sync"))
            }

            val crewName = positionRepository.getPosition()
            if (crewName.isNullOrBlank()) {
                Log.w(TAG, "⚠️ No crew name (position) set - cannot sync")
                _syncStatus.value = SyncStatus.ERROR
                return@withContext Result.failure(Exception("No crew name set"))
            }

            val currentUser = authService.getCurrentUser()
            if (currentUser != null) {
                Log.d(TAG, "✅ Syncing for CURRENT user: ${currentUser.email} ($userId)")
            } else {
                Log.d(TAG, "🔄 Syncing for LAST logged user: $userId")
            }

            // STEP 1: Assign userId to offline items
            val offlineItemsCount = localDao.assignUserIdToOfflineItems(userId)
            if (offlineItemsCount > 0) {
                Log.d(TAG, "📝 Assigned userId to $offlineItemsCount offline items")
            }

            // STEP 2: PUSH - Upload local changes to server
            val localItemsNeedingSync = localDao.getItemsNeedingSync(userId)
            Log.d(TAG, "📤 Local items needing sync: ${localItemsNeedingSync.size}")

            for (localItem in localItemsNeedingSync) {
                if (localItem.userId == null) {
                    Log.w(TAG, "⚠️ Skipping item without userId: ${localItem.id}")
                    continue
                }

                if (localItem.userId != userId) {
                    Log.w(TAG, "⚠️ Skipping item with different userId: ${localItem.id}")
                    continue
                }

                // Ensure crew name is set
                val itemWithCrew = if (localItem.crewName.isBlank()) {
                    localItem.copy(crewName = crewName)
                } else {
                    localItem
                }

                when {
                    localItem.supabaseId == null && localItem.isActive -> {
                        Log.d(TAG, "➕ Creating new item: ${localItem.itemName}")
                        val newSupabaseId = remoteRepository.createItem(itemWithCrew)
                        if (newSupabaseId != null) {
                            localDao.setSupabaseId(localItem.id, newSupabaseId)
                            Log.d(TAG, "✅ Created and linked: ${localItem.itemName}")
                        } else {
                            Log.e(TAG, "❌ Failed to create: ${localItem.itemName}")
                        }
                    }

                    localItem.supabaseId != null && !localItem.isActive -> {
                        Log.d(TAG, "🗑️ Deleting on server: ${localItem.itemName}")
                        val deleted = remoteRepository.deleteItem(localItem.supabaseId)
                        if (deleted) {
                            localDao.markAsSynced(localItem.id)
                            Log.d(TAG, "✅ Deleted on server: ${localItem.itemName}")
                        } else {
                            Log.e(TAG, "❌ Failed to delete: ${localItem.itemName}")
                        }
                    }

                    localItem.supabaseId != null && localItem.isActive -> {
                        Log.d(TAG, "✏️ Updating on server: ${localItem.itemName}")
                        val updated = remoteRepository.updateItem(localItem.supabaseId, itemWithCrew)
                        if (updated) {
                            localDao.markAsSynced(localItem.id)
                            Log.d(TAG, "✅ Updated on server: ${localItem.itemName}")
                        } else {
                            Log.e(TAG, "❌ Failed to update: ${localItem.itemName}")
                        }
                    }

                    localItem.supabaseId == null && !localItem.isActive -> {
                        Log.d(TAG, "🗑️ Marking deleted new item as synced: ${localItem.itemName}")
                        localDao.markAsSynced(localItem.id)
                    }
                }
            }

            // STEP 3: PULL - Download items from server
            Log.d(TAG, "📥 Fetching items from server...")
            val remoteItems = remoteRepository.getAllItems(crewName)
            Log.d(TAG, "📥 Fetched ${remoteItems.size} items from server")

            val allLocalItems = localDao.getAllItems(userId)
            val localItemsBySupabaseId = allLocalItems
                .filter { it.supabaseId != null }
                .associateBy { it.supabaseId!! }

            for (remoteItem in remoteItems) {
                if (remoteItem.id == null) continue

                if (remoteItem.crewName != crewName) {
                    Log.w(TAG, "⚠️ Skipping remote item with different crew: ${remoteItem.id}")
                    continue
                }

                val existingLocalItem = localItemsBySupabaseId[remoteItem.id]

                when {
                    existingLocalItem == null -> {
                        if (remoteItem.isActive) {
                            Log.d(TAG, "⬇️ Creating new local item from server: ${remoteItem.itemName}")

                            // FIXED: Use default category
                            val newEntity = remoteItem.toEntity(
                                localCategory = DEFAULT_CATEGORY,
                                userId = userId
                            )
                            localDao.insertItem(newEntity)
                        }
                    }

                    !existingLocalItem.needsSync -> {
                        if (remoteItem.isActive && !existingLocalItem.isActive) {
                            Log.d(TAG, "♻️ Restoring from server: ${remoteItem.itemName}")
                            localDao.updateFromServer(
                                supabaseId = remoteItem.id,
                                userId = userId,
                                name = remoteItem.itemName,
                                quantity = remoteItem.availableQuantity,
                                category = existingLocalItem.category,
                                crewName = remoteItem.crewName,
                                isActive = true
                            )
                        } else if (!remoteItem.isActive && existingLocalItem.isActive) {
                            Log.d(TAG, "🗑️ Marking as deleted from server: ${remoteItem.itemName}")
                            localDao.softDeleteItem(existingLocalItem.id)
                        } else if (remoteItem.isActive) {
                            val needsUpdate = existingLocalItem.itemName != remoteItem.itemName ||
                                    existingLocalItem.availableQuantity != remoteItem.availableQuantity ||
                                    existingLocalItem.crewName != remoteItem.crewName

                            if (needsUpdate) {
                                Log.d(TAG, "🔄 Updating from server: ${remoteItem.itemName}")
                                localDao.updateFromServer(
                                    supabaseId = remoteItem.id,
                                    userId = userId,
                                    name = remoteItem.itemName,
                                    quantity = remoteItem.availableQuantity,
                                    category = existingLocalItem.category,
                                    crewName = remoteItem.crewName,
                                    isActive = remoteItem.isActive
                                )
                            } else {
                                localDao.markAsSynced(existingLocalItem.id)
                            }
                        }
                    }

                    else -> {
                        Log.d(TAG, "📝 Keeping local changes for: ${existingLocalItem.itemName}")
                    }
                }
            }

            _syncStatus.value = SyncStatus.SUCCESS
            _lastSyncTime.value = System.currentTimeMillis()
            Log.d(TAG, "✅ Sync completed successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Sync failed", e)
            _syncStatus.value = SyncStatus.ERROR
            Result.failure(e)
        }
    }

    suspend fun hasUnsyncedChanges(): Boolean = withContext(Dispatchers.IO) {
        val userId = authService.getUserIdForSync() ?: return@withContext false
        val count = localDao.getItemsNeedingSync(userId).size
        Log.d(TAG, "📊 Unsynced items count: $count")
        count > 0
    }

    fun resetSyncStatus() {
        _syncStatus.value = SyncStatus.IDLE
    }
}

// FIXED: Extension function for conversion with default category
fun CrewInventoryItem.toEntity(
    localCategory: InventoryCategory = InventoryCategory.EQUIPMENT,  // Default "інвентар"
    userId: String? = null
): InventoryItemEntity {
    return InventoryItemEntity(
        id = 0,
        itemName = itemName,
        availableQuantity = availableQuantity,
        category = localCategory,
        userId = userId,
        crewName = crewName,
        supabaseId = id,
        createdAt = Date(),
        lastModified = Date(),
        lastSynced = Date(),
        needsSync = false,
        isActive = isActive
    )
}

// FIXED: Extension function for converting entity to server model - explicitly set tenantId
fun InventoryItemEntity.toCrewInventoryItem(): CrewInventoryItem {
    return CrewInventoryItem(
        id = supabaseId,
        tenantId = 0,  // CRITICAL FIX: Always set to 0 (required NOT NULL field in database)
        crewName = crewName,
        crewType = null,
        itemName = itemName,
        itemCategory = null,
        unit = "шт",
        availableQuantity = availableQuantity,
        neededQuantity = 0,
        priority = "medium",
        description = null,
        notes = null,
        lastNeedUpdatedAt = null,
        neededBy = null,
        createdBy = null,
        updatedBy = null,
        metadata = null,
        isActive = isActive
    )
}