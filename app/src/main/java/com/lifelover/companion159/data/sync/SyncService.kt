package com.lifelover.companion159.data.sync

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.data.mappers.InventoryMapper
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.remote.models.CrewInventoryItem
import com.lifelover.companion159.data.remote.repository.SupabaseInventoryRepository
import com.lifelover.companion159.data.repository.PositionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sync status states
 */
enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
    OFFLINE
}

/**
 * Service for synchronizing inventory data between local database and Supabase
 *
 * Sync flow:
 * 1. Assign userId to offline items
 * 2. PUSH: Upload local changes to server
 * 3. PULL: Download server changes to local
 *
 * Business rules:
 * - Category is stored locally only (not synced to server)
 * - Server's item_category field is nullable and not enforced
 * - Local category is preserved during sync operations
 */
@Singleton
class SyncService @Inject constructor(
    private val localDao: InventoryDao,
    private val remoteRepository: SupabaseInventoryRepository,
    private val authService: SupabaseAuthService,
    private val positionRepository: PositionRepository
) {
    companion object {
        private const val TAG = "SyncService"
        private val DEFAULT_CATEGORY = InventoryCategory.EQUIPMENT
    }

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    /**
     * Check if there are offline items (items without userId)
     */
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

    /**
     * Main sync operation
     * Synchronizes local changes with server and pulls server updates
     */
    suspend fun performSync(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Starting sync...")
            _syncStatus.value = SyncStatus.SYNCING

            // Get user ID
            val userId = authService.getUserIdForSync()
            if (userId == null) {
                Log.w(TAG, "⚠️ No user found - cannot sync")
                _syncStatus.value = SyncStatus.ERROR
                return@withContext Result.failure(Exception("No user available for sync"))
            }

            // Get crew name (position)
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
                    // CREATE: New item not yet on server
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

                    // DELETE: Item marked as deleted on server
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

                    // UPDATE: Existing active item
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

                    // SKIP: Deleted new item (never synced)
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
                    // CREATE: New item from server
                    existingLocalItem == null -> {
                        if (remoteItem.isActive) {
                            Log.d(TAG, "⬇️ Creating new local item from server: ${remoteItem.itemName}")
                            val newEntity = InventoryMapper.fromApi(
                                api = remoteItem,
                                userId = userId,
                                existingCategory = DEFAULT_CATEGORY
                            )
                            localDao.insertItem(newEntity)
                        }
                    }

                    // UPDATE: Existing item without local changes
                    !existingLocalItem.needsSync -> {
                        if (remoteItem.isActive && !existingLocalItem.isActive) {
                            Log.d(TAG, "♻️ Restoring from server: ${remoteItem.itemName}")
                            localDao.updateFromServer(
                                supabaseId = remoteItem.id,
                                userId = userId,
                                name = remoteItem.itemName,
                                quantity = remoteItem.availableQuantity,
                                neededQuantity = remoteItem.neededQuantity,
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
                                    existingLocalItem.neededQuantity != remoteItem.neededQuantity ||
                                    existingLocalItem.crewName != remoteItem.crewName

                            if (needsUpdate) {
                                Log.d(TAG, "🔄 Updating from server: ${remoteItem.itemName}")
                                localDao.updateFromServer(
                                    supabaseId = remoteItem.id,
                                    userId = userId,
                                    name = remoteItem.itemName,
                                    quantity = remoteItem.availableQuantity,
                                    neededQuantity = remoteItem.neededQuantity,
                                    category = existingLocalItem.category,
                                    crewName = remoteItem.crewName,
                                    isActive = remoteItem.isActive
                                )
                            } else {
                                localDao.markAsSynced(existingLocalItem.id)
                            }
                        }
                    }

                    // SKIP: Item has local changes, keep them
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

    /**
     * Check if there are unsynced changes
     */
    suspend fun hasUnsyncedChanges(): Boolean = withContext(Dispatchers.IO) {
        val userId = authService.getUserIdForSync() ?: return@withContext false
        val count = localDao.getItemsNeedingSync(userId).size
        Log.d(TAG, "📊 Unsynced items count: $count")
        count > 0
    }

    /**
     * Reset sync status to idle
     */
    fun resetSyncStatus() {
        _syncStatus.value = SyncStatus.IDLE
    }
}

/**
 * Convert local entity to server model
 * Maps all quantities for sync
 */
fun InventoryItemEntity.toCrewInventoryItem(): CrewInventoryItem {
    return CrewInventoryItem(
        id = supabaseId,
        tenantId = 0,
        crewName = crewName,
        crewType = null,
        itemName = itemName,
        itemCategory = null,
        unit = "шт",
        availableQuantity = availableQuantity,
        neededQuantity = neededQuantity,
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