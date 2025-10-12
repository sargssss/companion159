package com.lifelover.companion159.data.repository

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.mappers.InventoryMapper
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.sync.AutoSyncManager
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.network.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    suspend fun getAllItemsOnce(): List<InventoryItem>
    suspend fun addItem(item: InventoryItem)
    suspend fun updateItem(item: InventoryItem)
    suspend fun updateItemQuantity(itemId: Long, quantity: Int)
    suspend fun updateNeededQuantity(itemId: Long, quantity: Int)
    suspend fun deleteItem(id: Long)
    suspend fun syncWithServer(): SyncResult
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
            .map { entities -> entities.map { InventoryMapper.toDomain(it) } }
    }

    override fun getAmmunitionItems(): Flow<List<InventoryItem>> {
        val userId = authService.getUserId()
        return localDao.getAmmunitionItems(userId)
            .map { entities -> entities.map { InventoryMapper.toDomain(it) } }
    }

    override fun getNeedsItems(): Flow<List<InventoryItem>> {
        val userId = authService.getUserId()
        return localDao.getNeedsItems(userId)
            .map { entities -> entities.map { InventoryMapper.toDomain(it) } }
    }

    override suspend fun getAllItemsOnce(): List<InventoryItem> {
        val userId = authService.getUserId()
        return localDao.getAllItems(userId).map { InventoryMapper.toDomain(it) }
    }

    override suspend fun addItem(item: InventoryItem) {
        val userId = authService.getUserId()
        val crewName = positionRepository.getPosition() ?: "Default"

        val entity = InventoryMapper.toEntity(
            domain = item.copy(crewName = crewName),
            userId = userId
        )

        val insertedId = localDao.insertItem(entity)
        Log.d(TAG, "Item created with ID: $insertedId")

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
            throw SecurityException("Cannot update item of another user")
        }

        val updatedRows = localDao.updateItemWithNeeds(
            id = item.id,
            name = item.itemName.trim(),
            availableQuantity = item.availableQuantity,
            neededQuantity = item.neededQuantity,
            category = InventoryMapper.toEntity(item).category,
            crewName = crewName
        )

        if (updatedRows > 0) {
            Log.d(TAG, "✅ Item updated: $updatedRows rows")
        }

        if (userId != null) {
            triggerBackgroundSync()
        }
    }

    /**
     * Update available quantity for item
     * Used by AVAILABILITY and AMMUNITION screens
     */
    override suspend fun updateItemQuantity(itemId: Long, quantity: Int) {
        val userId = authService.getUserId()

        val existingItem = localDao.getItemById(itemId)
        if (existingItem == null) {
            Log.e(TAG, "❌ Item with ID $itemId does NOT exist")
            return
        }

        if (existingItem.userId != null && existingItem.userId != userId) {
            Log.e(TAG, "❌ Cannot update quantity: belongs to different user")
            return
        }

        val updatedRows = localDao.updateQuantity(itemId, quantity)

        if (updatedRows > 0) {
            Log.d(TAG, "✅ Available quantity updated locally: $itemId -> $quantity")
        } else {
            Log.e(TAG, "❌ Failed to update available quantity")
        }

        // Trigger sync if online
        if (userId != null) {
            triggerBackgroundSync()
        }
    }

    override suspend fun updateNeededQuantity(itemId: Long, quantity: Int) {
        val userId = authService.getUserId()

        val existingItem = localDao.getItemById(itemId)
        if (existingItem == null) {
            Log.e(TAG, "Item $itemId not found")
            return
        }

        if (existingItem.userId != null && existingItem.userId != userId) {
            throw SecurityException("Cannot update item of another user")
        }

        localDao.updateNeededQuantity(itemId, quantity)

        if (userId != null) {
            triggerBackgroundSync()
        }
    }

    override suspend fun deleteItem(id: Long) {
        val userId = authService.getUserId()

        val existingItem = localDao.getItemById(id)
        if (existingItem == null) {
            Log.w(TAG, "Item $id not found")
            return
        }

        if (existingItem.userId != null && existingItem.userId != userId) {
            throw SecurityException("Cannot delete item of another user")
        }

        localDao.softDeleteItem(id)

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
                else -> SyncResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun triggerBackgroundSync() {
        if (networkMonitor.isOnline) {
            backgroundScope.launch {
                try {
                    autoSyncManager.triggerImmediateSync()
                } catch (e: Exception) {
                    Log.e(TAG, "Background sync failed", e)
                }
            }
        }
    }
}