package com.lifelover.companion159.data.sync

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.remote.repository.SupabaseInventoryRepository
import com.lifelover.companion159.data.remote.repository.toEntity
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
    private val authService: SupabaseAuthService
) {
    companion object {
        private const val TAG = "SyncService"
    }

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    suspend fun performSync(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting sync...")
            _syncStatus.value = SyncStatus.SYNCING

            val userId = authService.getUserId()
            if (userId == null) {
                Log.w(TAG, "User not authenticated")
                _syncStatus.value = SyncStatus.ERROR
                return@withContext Result.failure(Exception("User not authenticated"))
            }

            Log.d(TAG, "User authenticated: $userId")

            // Assign userId to all offline items
            val offlineItemsCount = localDao.assignUserIdToOfflineItems(userId)
            if (offlineItemsCount > 0) {
                Log.d(TAG, "Assigned userId to $offlineItemsCount offline items")
            }

            // PUSH: Upload local changes to server
            val localItemsNeedingSync = localDao.getItemsNeedingSync(userId)
            Log.d(TAG, "Local items needing sync: ${localItemsNeedingSync.size}")

            for (localItem in localItemsNeedingSync) {
                // Skip items without userId (should not happen after assignment above)
                if (localItem.userId == null) {
                    Log.w(TAG, "Skipping item without userId: ${localItem.id}")
                    continue
                }

                // Skip items that don't belong to current user
                if (localItem.userId != userId) {
                    Log.w(TAG, "Skipping item with different userId: ${localItem.id}")
                    continue
                }

                when {
                    localItem.supabaseId == null && !localItem.isDeleted -> {
                        Log.d(TAG, "Creating new item on server: ${localItem.name}")
                        val newSupabaseId = remoteRepository.createItem(localItem)
                        if (newSupabaseId != null) {
                            localDao.setSupabaseId(localItem.id, newSupabaseId)
                            Log.d(TAG, "Created and linked: ${localItem.name}")
                        }
                    }

                    localItem.supabaseId != null && localItem.isDeleted -> {
                        Log.d(TAG, "Deleting on server: ${localItem.name}")
                        val deleted = remoteRepository.deleteItem(localItem.supabaseId)
                        if (deleted) {
                            localDao.markAsSynced(localItem.id)
                            Log.d(TAG, "Deleted on server: ${localItem.name}")
                        }
                    }

                    localItem.supabaseId != null && !localItem.isDeleted -> {
                        Log.d(TAG, "Updating on server: ${localItem.name}")
                        val updated = remoteRepository.updateItem(localItem.supabaseId, localItem)
                        if (updated) {
                            localDao.markAsSynced(localItem.id)
                            Log.d(TAG, "Updated on server: ${localItem.name}")
                        }
                    }

                    localItem.supabaseId == null && localItem.isDeleted -> {
                        Log.d(TAG, "Marking deleted new item as synced: ${localItem.name}")
                        localDao.markAsSynced(localItem.id)
                    }
                }
            }

            // PULL: Download data from server
            Log.d(TAG, "Fetching items from server...")
            val remoteItems = remoteRepository.getAllItems()
            Log.d(TAG, "Fetched ${remoteItems.size} items from server")

            val allLocalItems = localDao.getAllItems(userId)
            val localItemsBySupabaseId = allLocalItems
                .filter { it.supabaseId != null }
                .associateBy { it.supabaseId!! }

            for (remoteItem in remoteItems) {
                if (remoteItem.id == null) continue

                if (remoteItem.userId != userId) {
                    Log.w(TAG, "Skipping remote item with different userId: ${remoteItem.id}")
                    continue
                }

                val existingLocalItem = localItemsBySupabaseId[remoteItem.id]

                when {
                    existingLocalItem == null -> {
                        if (!remoteItem.isDeleted) {
                            Log.d(TAG, "Creating new local item from server: ${remoteItem.name}")
                            val newEntity = remoteItem.toEntity().copy(userId = userId)
                            localDao.insertItem(newEntity)
                        }
                    }

                    !existingLocalItem.needsSync -> {
                        if (remoteItem.isDeleted && !existingLocalItem.isDeleted) {
                            Log.d(TAG, "Marking as deleted from server: ${remoteItem.name}")
                            localDao.softDeleteItem(existingLocalItem.id)
                        } else if (!remoteItem.isDeleted) {
                            val needsUpdate = existingLocalItem.name != remoteItem.name ||
                                    existingLocalItem.quantity != remoteItem.quantity ||
                                    existingLocalItem.category.name.lowercase() != remoteItem.category.lowercase()

                            if (needsUpdate) {
                                Log.d(TAG, "Updating from server: ${remoteItem.name}")
                                localDao.updateFromServer(
                                    supabaseId = remoteItem.id,
                                    userId = userId,
                                    name = remoteItem.name,
                                    quantity = remoteItem.quantity,
                                    category = InventoryCategory.valueOf(remoteItem.category.uppercase()),
                                    isDeleted = remoteItem.isDeleted
                                )
                            } else {
                                localDao.markAsSynced(existingLocalItem.id)
                            }
                        }
                    }

                    else -> {
                        Log.d(TAG, "Keeping local changes for: ${existingLocalItem.name}")
                    }
                }
            }

            _syncStatus.value = SyncStatus.SUCCESS
            _lastSyncTime.value = System.currentTimeMillis()
            Log.d(TAG, "Sync completed successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            _syncStatus.value = SyncStatus.ERROR
            Result.failure(e)
        }
    }

    suspend fun hasUnsyncedChanges(): Boolean = withContext(Dispatchers.IO) {
        val userId = authService.getUserId() ?: return@withContext false
        val count = localDao.getItemsNeedingSync(userId).size
        Log.d(TAG, "Unsynced items count: $count")
        count > 0
    }

    fun resetSyncStatus() {
        _syncStatus.value = SyncStatus.IDLE
    }
}