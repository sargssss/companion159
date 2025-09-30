package com.lifelover.companion159.data.repository

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.sync.AutoSyncManager
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.toDomainModel
import com.lifelover.companion159.domain.models.toEntity
import com.lifelover.companion159.network.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton


interface InventoryRepository {
    fun getItemsByCategory(category: InventoryCategory): Flow<List<InventoryItem>>
    suspend fun addItem(item: InventoryItem)
    suspend fun updateItem(item: InventoryItem)
    suspend fun deleteItem(id: Long)
    suspend fun syncWithServer(): SyncResult
    suspend fun hasUnsyncedChanges(): Boolean
}

sealed class SyncResult {
    object Success : SyncResult()
    data class Error(val message: String) : SyncResult()
    object NetworkError : SyncResult()
}

@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val localDao: InventoryDao,
    private val autoSyncManager: AutoSyncManager,
    private val networkMonitor: NetworkMonitor,
    private val authService: SupabaseAuthService
) : InventoryRepository {

    companion object {
        private const val TAG = "InventoryRepository"
    }

    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun getItemsByCategory(category: InventoryCategory): Flow<List<InventoryItem>> {
        val userId = authService.getUserId() ?: return flowOf(emptyList())

        return localDao.getItemsByCategory(category, userId)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    override suspend fun addItem(item: InventoryItem) {
        val userId = authService.getUserId()
        if (userId == null) {
            Log.e(TAG, "Cannot add item: user not authenticated")
            return
        }

        Log.d(TAG, "Creating new item: ${item.name}")

        val entity = item.toEntity().copy(
            id = 0,
            userId = userId,
            supabaseId = null,
            needsSync = true,
            lastModified = Date(),
            isDeleted = false
        )

        val insertedId = localDao.insertItem(entity)
        Log.d(TAG, "New item created with local ID: $insertedId")

        triggerBackgroundSync()
    }

    override suspend fun updateItem(item: InventoryItem) {
        val userId = authService.getUserId()
        if (userId == null) {
            Log.e(TAG, "Cannot update item: user not authenticated")
            return
        }

        Log.d(TAG, "Updating existing item with ID: ${item.id}, name: ${item.name}")

        val existingItem = localDao.getItemById(item.id)
        if (existingItem == null) {
            Log.e(TAG, "Item with ID ${item.id} does NOT exist, cannot update")
            throw IllegalArgumentException("Item with ID ${item.id} does not exist")
        }

        if (existingItem.userId != null && existingItem.userId != userId) {
            Log.e(TAG, "Cannot update item: belongs to different user")
            throw IllegalArgumentException("Cannot update item of another user")
        }

        val updatedRows = localDao.updateItem(
            id = item.id,
            name = item.name.trim(),
            quantity = item.quantity,
            category = item.category
        )

        if (updatedRows > 0) {
            Log.d(TAG, "EXISTING item updated locally (rows: $updatedRows)")
        } else {
            Log.w(TAG, "No rows updated for item ID: ${item.id}")
        }

        triggerBackgroundSync()
    }

    suspend fun updateItemQuantity(itemId: Long, newQuantity: Int) {
        val userId = authService.getUserId()
        if (userId == null) {
            Log.e(TAG, "Cannot update quantity: user not authenticated")
            return
        }

        Log.d(TAG, "OPTIMISTIC quantity update for item ID: $itemId to $newQuantity")

        val existingItem = localDao.getItemById(itemId)
        if (existingItem == null) {
            Log.e(TAG, "Item with ID $itemId does NOT exist")
            return
        }

        if (existingItem.userId != null && existingItem.userId != userId) {
            Log.e(TAG, "Cannot update quantity: belongs to different user")
            return
        }

        val updatedRows = localDao.updateQuantity(itemId, newQuantity)

        if (updatedRows > 0) {
            Log.d(TAG, "Quantity updated locally, triggering background sync")
        } else {
            Log.w(TAG, "No rows updated for item ID: $itemId")
        }

        triggerBackgroundSync()
    }

    override suspend fun deleteItem(id: Long) {
        val userId = authService.getUserId()
        if (userId == null) {
            Log.e(TAG, "Cannot delete item: user not authenticated")
            return
        }

        Log.d(TAG, "DELETING existing item with ID: $id")

        val existingItem = localDao.getItemById(id)
        if (existingItem == null) {
            Log.w(TAG, "Item with ID $id does NOT exist, cannot delete")
            return
        }

        if (existingItem.userId != null && existingItem.userId != userId) {
            Log.e(TAG, "Cannot delete item: belongs to different user")
            return
        }

        Log.d(TAG, "Deleting item: ${existingItem.name}, supabaseId: ${existingItem.supabaseId}")

        val deletedRows = localDao.softDeleteItem(id)

        if (deletedRows > 0) {
            Log.d(TAG, "Item marked as deleted locally, triggering background sync")
        } else {
            Log.w(TAG, "No rows updated for item ID: $id")
        }

        triggerBackgroundSync()
    }

    override suspend fun syncWithServer(): SyncResult {
        Log.d(TAG, "Manual sync requested")
        return try {
            autoSyncManager.triggerImmediateSync()
            SyncResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Manual sync failed", e)
            when {
                !networkMonitor.isOnline -> SyncResult.NetworkError
                else -> SyncResult.Error(e.message ?: "Unknown sync error")
            }
        }
    }

    override suspend fun hasUnsyncedChanges(): Boolean {
        val userId = authService.getUserId() ?: return false
        val hasChanges = localDao.getItemsNeedingSync(userId).isNotEmpty()
        Log.d(TAG, "Has unsynced changes: $hasChanges")
        return hasChanges
    }

    private fun triggerBackgroundSync() {
        Log.d(TAG, "Triggering background sync (non-blocking)")

        if (networkMonitor.isOnline) {
            backgroundScope.launch {
                try {
                    Log.d(TAG, "Starting background sync...")
                    autoSyncManager.triggerImmediateSync()
                    Log.d(TAG, "Background sync completed")
                } catch (e: Exception) {
                    Log.e(TAG, "Background sync failed", e)
                }
            }
        } else {
            Log.d(TAG, "Device is offline, sync will happen when online")
        }
    }
}