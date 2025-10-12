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


sealed class SyncResult {
    object Success : SyncResult()
    data class Error(val message: String) : SyncResult()
    object NetworkError : SyncResult()
}
interface InventoryRepository {
    fun getAvailabilityItems(): Flow<List<InventoryItem>>
    fun getAmmunitionItems(): Flow<List<InventoryItem>>
    fun getNeedsItems(): Flow<List<InventoryItem>>
    fun getItemsByCategory(category: InventoryCategory): Flow<List<InventoryItem>>
    suspend fun addItem(item: InventoryItem)
    suspend fun updateItem(item: InventoryItem)
    suspend fun updateNeededQuantity(itemId: Long, quantity: Int)  // NEW
    suspend fun deleteItem(id: Long)
    suspend fun syncWithServer(): SyncResult
    suspend fun hasUnsyncedChanges(): Boolean
}

@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val localDao: InventoryDao,
    private val autoSyncManager: AutoSyncManager,
    private val networkMonitor: NetworkMonitor,
    private val authService: SupabaseAuthService,
    private val positionRepository: PositionRepository
) : InventoryRepository {

    companion object {
        private const val TAG = "InventoryRepository"
    }

    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun getAvailabilityItems(): Flow<List<InventoryItem>> {
        val userId = authService.getUserId()
        return localDao.getAvailabilityItems(userId)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    override fun getAmmunitionItems(): Flow<List<InventoryItem>> {
        val userId = authService.getUserId()
        return localDao.getAmmunitionItems(userId)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    override fun getNeedsItems(): Flow<List<InventoryItem>> {
        val userId = authService.getUserId()
        return localDao.getNeedsItems(userId)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    override fun getItemsByCategory(category: InventoryCategory): Flow<List<InventoryItem>> {
        val userId = authService.getUserId()
        return if (userId != null) {
            localDao.getItemsByCategory(category, userId)
                .map { entities -> entities.map { it.toDomainModel() } }
        } else {
            localDao.getItemsByCategoryOffline(category)
                .map { entities -> entities.map { it.toDomainModel() } }
        }
    }

    override suspend fun addItem(item: InventoryItem) {
        val userId = authService.getUserId()
        val crewName = positionRepository.getPosition() ?: "Default"

        val entity = item.toEntity().copy(
            id = 0,
            userId = userId,
            crewName = crewName,
            supabaseId = null,
            needsSync = userId != null,
            lastModified = Date(),
            isActive = true
        )

        val insertedId = localDao.insertItem(entity)
        Log.d(TAG, "New item created with local ID: $insertedId")

        if (userId != null) {
            triggerBackgroundSync()
        }
    }

    override suspend fun updateItem(item: InventoryItem) {
        val userId = authService.getUserId()
        val crewName = positionRepository.getPosition() ?: "Default"

        val existingItem = localDao.getItemById(item.id)
        if (existingItem == null) {
            throw IllegalArgumentException("Item with ID ${item.id} does not exist")
        }

        if (existingItem.userId != null && existingItem.userId != userId) {
            throw IllegalArgumentException("Cannot update item of another user")
        }

        val updatedRows = localDao.updateItemWithNeeds(
            id = item.id,
            name = item.itemName.trim(),
            availableQuantity = item.availableQuantity,
            neededQuantity = item.neededQuantity,
            category = item.category,
            crewName = crewName
        )

        if (updatedRows > 0) {
            Log.d(TAG, "✅ Database updated: $updatedRows rows affected")
        } else {
            Log.e(TAG, "❌ Database update failed: 0 rows affected")
        }

        if (userId != null) {
            Log.d(TAG, "🔄 Triggering background sync...")
            triggerBackgroundSync()
        }
    }

    override suspend fun updateNeededQuantity(itemId: Long, quantity: Int) {
        val userId = authService.getUserId()

        val existingItem = localDao.getItemById(itemId)
        if (existingItem == null) {
            Log.e(TAG, "❌ Item with ID $itemId does NOT exist")
            return
        }

        if (existingItem.userId != null && existingItem.userId != userId) {
            Log.e(TAG, "❌ Cannot update: belongs to different user")
            return
        }

        val updatedRows = localDao.updateNeededQuantity(itemId, quantity)

        if (updatedRows > 0) {
            Log.d(TAG, "✅ Needed quantity updated locally: $itemId -> $quantity")
        } else {
            Log.e(TAG, "❌ Failed to update needed quantity")
        }

        // Trigger sync if online
        if (userId != null) {
            triggerBackgroundSync()
        }
    }

    suspend fun updateItemQuantity(itemId: Long, newQuantity: Int) {
        val userId = authService.getUserId()

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
            Log.d(TAG, "Quantity updated locally")
        }

        if (userId != null) {
            triggerBackgroundSync()
        }
    }

    override suspend fun deleteItem(id: Long) {
        val userId = authService.getUserId()

        val existingItem = localDao.getItemById(id)
        if (existingItem == null) {
            Log.w(TAG, "Item with ID $id does NOT exist")
            return
        }

        if (existingItem.userId != null && existingItem.userId != userId) {
            Log.e(TAG, "Cannot delete item: belongs to different user")
            return
        }

        val deletedRows = localDao.softDeleteItem(id)

        if (deletedRows > 0) {
            Log.d(TAG, "Item marked as deleted locally")
        }

        if (userId != null) {
            triggerBackgroundSync()
        }
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