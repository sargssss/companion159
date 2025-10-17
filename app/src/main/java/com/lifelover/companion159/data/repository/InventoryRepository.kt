package com.lifelover.companion159.data.repository

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.entities.toDomain
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.remote.sync.SyncQueueManager
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.QuantityType
import com.lifelover.companion159.domain.models.toEntity
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepository @Inject constructor(
    private val dao: InventoryDao,
    private val positionRepository: PositionRepository,
    private val authService: SupabaseAuthService,
    private val syncQueueManager: SyncQueueManager
) {
    companion object {
        private const val TAG = "InventoryRepository"
    }

    private fun requireUserId(): String {
        return authService.getUserId()
            ?: throw IllegalStateException("User must be authenticated to access inventory")
    }

    private fun requireCrewName(): String {
        return positionRepository.currentPosition.value
            ?: throw IllegalStateException("Position must be set to access inventory")
    }

    fun getAvailabilityItems(): Flow<List<InventoryItem>> {
        return flow {
            val userId = requireUserId()
            val crewName = requireCrewName()
            dao.getAvailabilityItems(userId, crewName).collect { entities ->
                emit(entities.map { it.toDomain() })
            }
        }
    }

    fun getAmmunitionItems(): Flow<List<InventoryItem>> {
        return flow {
            val userId = requireUserId()
            val crewName = requireCrewName()
            dao.getAmmunitionItems(userId, crewName).collect { entities ->
                emit(entities.map { it.toDomain() })
            }
        }
    }

    fun getNeedsItems(): Flow<List<InventoryItem>> {
        return flow {
            val userId = requireUserId()
            val crewName = requireCrewName()
            dao.getNeedsItems(userId, crewName).collect { entities ->
                emit(entities.map { it.toDomain() })
            }
        }
    }

    suspend fun getAllItemsOnce(): List<InventoryItem> {
        val userId = requireUserId()
        val crewName = requireCrewName()
        return dao.getAllItems(userId, crewName).map { it.toDomain() }
    }

    /**
     * Add new item
     * Automatically enqueues INSERT operation for sync
     */
    suspend fun addItem(item: InventoryItem) {
        val userId = requireUserId()
        val crewName = requireCrewName()

        val entity = item.copy(crewName = crewName).toEntity(userId = userId)
        val insertedId = dao.insertItem(entity)

        Log.d(TAG, "✅ Item created with ID: $insertedId")

        // Enqueue INSERT operation
        syncQueueManager.enqueueInsert(
            localItemId = insertedId,
            itemData = mapOf(
                "itemName" to item.itemName,
                "availableQuantity" to item.availableQuantity,
                "neededQuantity" to item.neededQuantity,
                "category" to item.category.name,
                "crewName" to crewName
            )
        )
    }

    /**
     * Update item with all fields
     * Automatically enqueues UPDATE operation for sync
     */
    suspend fun updateItem(item: InventoryItem) {
        val userId = requireUserId()
        val crewName = requireCrewName()

        val existingItem = dao.getItemById(item.id)
        if (existingItem == null) {
            throw IllegalArgumentException("Item with ID ${item.id} does not exist")
        }

        validateItemOwnership(existingItem, crewName)

        val updatedRows = dao.updateItemWithNeeds(
            id = item.id,
            name = item.itemName.trim(),
            availableQuantity = item.availableQuantity,
            neededQuantity = item.neededQuantity,
            category = item.category,
            crewName = crewName
        )

        if (updatedRows > 0) {
            Log.d(TAG, "✅ Item updated")

            // Enqueue UPDATE operation
            syncQueueManager.enqueueUpdate(
                localItemId = item.id,
                supabaseId = existingItem.supabaseId,
                itemData = mapOf(
                    "itemName" to item.itemName,
                    "availableQuantity" to item.availableQuantity,
                    "neededQuantity" to item.neededQuantity,
                    "category" to item.category.name,
                    "crewName" to crewName
                )
            )
        }
    }

    /**
     * Update single quantity
     * Automatically enqueues UPDATE operation for sync
     */
    suspend fun updateSingleQuantity(
        itemId: Long,
        quantity: Int,
        quantityType: QuantityType
    ) {
        val userId = requireUserId()
        val crewName = requireCrewName()

        val existingItem = dao.getItemById(itemId)
        if (existingItem == null) {
            Log.w(TAG, "⚠️ Item $itemId not found")
            return
        }

        validateItemOwnership(existingItem, crewName)

        val updatedRows = when (quantityType) {
            QuantityType.AVAILABLE -> {
                dao.updateQuantity(itemId, quantity)
            }
            QuantityType.NEEDED -> {
                dao.updateNeededQuantity(itemId, quantity)
            }
        }

        if (updatedRows > 0) {
            Log.d(TAG, "✅ Item quantity updated: ${quantityType.name}=$quantity")

            // Get updated item for queue
            val updatedItem = dao.getItemById(itemId)!!

            // Enqueue UPDATE operation
            syncQueueManager.enqueueUpdate(
                localItemId = itemId,
                supabaseId = existingItem.supabaseId,
                itemData = mapOf(
                    "itemName" to updatedItem.itemName,
                    "availableQuantity" to updatedItem.availableQuantity,
                    "neededQuantity" to updatedItem.neededQuantity,
                    "category" to updatedItem.category.name,
                    "crewName" to crewName
                )
            )
        }
    }

    suspend fun updateItemQuantity(itemId: Long, quantity: Int) {
        updateSingleQuantity(itemId, quantity, QuantityType.AVAILABLE)
    }

    suspend fun updateNeededQuantity(itemId: Long, quantity: Int) {
        updateSingleQuantity(itemId, quantity, QuantityType.NEEDED)
    }

    /**
     * Soft delete item
     * Automatically enqueues DELETE operation for sync
     */
    suspend fun deleteItem(id: Long) {
        val userId = requireUserId()
        val crewName = requireCrewName()

        val existingItem = dao.getItemById(id)
        if (existingItem == null) {
            Log.w(TAG, "⚠️ Item $id not found")
            return
        }

        validateItemOwnership(existingItem, crewName)

        dao.softDeleteItem(id)
        Log.d(TAG, "✅ Item deleted: $id")

        // Enqueue DELETE operation
        syncQueueManager.enqueueDelete(
            localItemId = id,
            supabaseId = existingItem.supabaseId
        )
    }

    private fun validateItemOwnership(
        item: com.lifelover.companion159.data.local.entities.InventoryItemEntity,
        crewName: String
    ) {
        if (item.crewName != crewName) {
            throw SecurityException("Cannot modify item from different crew")
        }
    }
}