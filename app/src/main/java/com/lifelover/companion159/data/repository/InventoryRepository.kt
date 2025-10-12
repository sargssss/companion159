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
    // NEW: Methods for display categories
    fun getAvailabilityItems(): Flow<List<InventoryItem>>
    fun getAmmunitionItems(): Flow<List<InventoryItem>>
    fun getNeedsItems(): Flow<List<InventoryItem>>

    // OLD: Keep for internal use
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

    // NEW: Display category methods
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

    // OLD: Keep for backward compatibility
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

        Log.d(TAG, "Creating new item: ${item.itemName}, userId: $userId, crew: $crewName")

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

        Log.d(TAG, "═══════════════════════════════════")
        Log.d(TAG, "🔄 UPDATING ITEM")
        Log.d(TAG, "═══════════════════════════════════")
        Log.d(TAG, "Item ID: ${item.id}")
        Log.d(TAG, "Item name: ${item.itemName}")
        Log.d(TAG, "Available quantity: ${item.availableQuantity}")
        Log.d(TAG, "Needed quantity: ${item.neededQuantity}")
        Log.d(TAG, "Category: ${item.category}")
        Log.d(TAG, "Crew name: $crewName")
        Log.d(TAG, "User ID: $userId")

        val existingItem = localDao.getItemById(item.id)
        if (existingItem == null) {
            Log.e(TAG, "❌ Item with ID ${item.id} does NOT exist in database")
            throw IllegalArgumentException("Item with ID ${item.id} does not exist")
        }

        Log.d(TAG, "✅ Existing item found in database")
        Log.d(TAG, "   Old name: ${existingItem.itemName}")
        Log.d(TAG, "   Old available: ${existingItem.availableQuantity}")
        Log.d(TAG, "   Old needed: ${existingItem.neededQuantity}")

        if (existingItem.userId != null && existingItem.userId != userId) {
            Log.e(TAG, "❌ Cannot update item: belongs to different user")
            throw IllegalArgumentException("Cannot update item of another user")
        }

        Log.d(TAG, "📝 Executing DAO update...")
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

        Log.d(TAG, "═══════════════════════════════════")
    }

    // NEW: Update needed quantity
    override suspend fun updateNeededQuantity(itemId: Long, quantity: Int) {
        val userId = authService.getUserId()

        Log.d(TAG, "📝 Updating needed quantity for item ID: $itemId to $quantity")

        val existingItem = localDao.getItemById(itemId)
        if (existingItem == null) {
            Log.e(TAG, "❌ Item with ID $itemId does NOT exist")
            return
        }

        if (existingItem.userId != null && existingItem.userId != userId) {
            Log.e(TAG, "❌ Cannot update: belongs to different user")
            return
        }

        // IMPORTANT: Update the value
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

        Log.d(TAG, "Quantity update for item ID: $itemId to $newQuantity")

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

        Log.d(TAG, "Deleting item with ID: $id")

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