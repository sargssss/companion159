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

            // PUSH: Upload local changes to server
            val localItemsNeedingSync = localDao.getItemsNeedingSync(userId)
            Log.d(TAG, "Local items needing sync: ${localItemsNeedingSync.size}")

            for (localItem in localItemsNeedingSync) {
                Log.d(TAG, "Processing local item: ${localItem.name}, userId: ${localItem.userId}, supabaseId: ${localItem.supabaseId}, isDeleted: ${localItem.isDeleted}")

                // Update userId for old items that don't have it
                if (localItem.userId == null) {
                    Log.d(TAG, "Updating userId for old item: ${localItem.name}")
                    val updatedEntity = localItem.copy(userId = userId, needsSync = true)
                    localDao.insertItem(updatedEntity)
                    continue
                }

                // Skip items that don't belong to current user
                if (localItem.userId != userId) {
                    Log.w(TAG, "Skipping item with different userId: ${localItem.id}")
                    continue
                }

                when {
                    // New item without supabaseId and not deleted -> CREATE on server
                    localItem.supabaseId == null && !localItem.isDeleted -> {
                        Log.d(TAG, "Creating new item on server: ${localItem.name}")
                        val newSupabaseId = remoteRepository.createItem(localItem)
                        if (newSupabaseId != null) {
                            localDao.setSupabaseId(localItem.id, newSupabaseId)
                            Log.d(TAG, "Created and linked: ${localItem.name} -> $newSupabaseId")
                        } else {
                            Log.e(TAG, "Failed to create: ${localItem.name}")
                        }
                    }

                    // Existing item with supabaseId and deleted -> DELETE on server
                    localItem.supabaseId != null && localItem.isDeleted -> {
                        Log.d(TAG, "Deleting on server: ${localItem.name}")
                        val deleted = remoteRepository.deleteItem(localItem.supabaseId)
                        if (deleted) {
                            localDao.markAsSynced(localItem.id)
                            Log.d(TAG, "Deleted on server: ${localItem.name}")
                        } else {
                            Log.e(TAG, "Failed to delete: ${localItem.name}")
                        }
                    }

                    // Existing item with supabaseId and not deleted -> UPDATE on server
                    localItem.supabaseId != null && !localItem.isDeleted -> {
                        Log.d(TAG, "Updating on server: ${localItem.name}")
                        val updated = remoteRepository.updateItem(localItem.supabaseId, localItem)
                        if (updated) {
                            localDao.markAsSynced(localItem.id)
                            Log.d(TAG, "Updated on server: ${localItem.name}")
                        } else {
                            Log.e(TAG, "Failed to update: ${localItem.name}")
                        }
                    }

                    // New item that was deleted locally -> just mark as synced
                    localItem.supabaseId == null && localItem.isDeleted -> {
                        Log.d(TAG, "Marking deleted new item as synced: ${localItem.name}")
                        localDao.markAsSynced(localItem.id)
                    }
                }
            }

            // PULL: Download data from server and update local database
            Log.d(TAG, "Fetching items from server...")
            val remoteItems = remoteRepository.getAllItems()
            Log.d(TAG, "Fetched ${remoteItems.size} items from server")

            // Get all local items for current user
            val allLocalItems = localDao.getAllItems(userId)
            val localItemsBySupabaseId = allLocalItems
                .filter { it.supabaseId != null }
                .associateBy { it.supabaseId!! }

            Log.d(TAG, "Local items with supabaseId: ${localItemsBySupabaseId.size}")

            // Process each item from server
            for (remoteItem in remoteItems) {
                if (remoteItem.id == null) {
                    Log.w(TAG, "Remote item has no ID, skipping")
                    continue
                }

                // Skip items that don't belong to current user
                if (remoteItem.userId != userId) {
                    Log.w(TAG, "Skipping remote item with different userId: ${remoteItem.id}")
                    continue
                }

                val existingLocalItem = localItemsBySupabaseId[remoteItem.id]

                when {
                    // New item from server - create locally
                    existingLocalItem == null -> {
                        if (!remoteItem.isDeleted) {
                            Log.d(TAG, "Creating new local item from server: ${remoteItem.name}")
                            val newEntity = remoteItem.toEntity().copy(userId = userId)
                            localDao.insertItem(newEntity)
                            Log.d(TAG, "Created locally: ${remoteItem.name}")
                        } else {
                            Log.d(TAG, "Skipping deleted item from server: ${remoteItem.name}")
                        }
                    }

                    // Existing item WITHOUT local changes - update from server
                    !existingLocalItem.needsSync -> {
                        if (remoteItem.isDeleted && !existingLocalItem.isDeleted) {
                            Log.d(TAG, "Marking as deleted from server: ${remoteItem.name}")
                            localDao.softDeleteItem(existingLocalItem.id)
                        } else if (!remoteItem.isDeleted) {
                            // Check if update is needed
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
                                Log.d(TAG, "Updated from server: ${remoteItem.name}")
                            } else {
                                Log.d(TAG, "No changes needed for: ${remoteItem.name}")
                                localDao.markAsSynced(existingLocalItem.id)
                            }
                        }
                    }

                    // Existing item WITH local changes - keep local changes
                    else -> {
                        Log.d(TAG, "Keeping local changes for: ${existingLocalItem.name} (server: ${remoteItem.name})")
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