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

/**
 * Pure data layer - NO business logic
 *
 * Responsibilities:
 * - CRUD operations on local database
 * - Data filtering and querying
 * - User/crew validation for security
 *
 * Does NOT:
 * - Validate input (Use Cases do this)
 * - Manage sync queue (Use Cases do this)
 * - Contain business rules (Use Cases do this)
 */
@Singleton
class InventoryRepository @Inject constructor(
    private val dao: InventoryDao,
    private val positionRepository: PositionRepository,
    private val authService: SupabaseAuthService
) {
    companion object {
        private const val TAG = "InventoryRepository"
    }

    // Helper methods for security
    private fun requireUserId(): String {
        return authService.getUserId()
            ?: throw IllegalStateException("User must be authenticated to access inventory")
    }

    private fun requireCrewName(): String {
        return positionRepository.currentPosition.value
            ?: throw IllegalStateException("Position must be set to access inventory")
    }

    // Query methods (return Flows for reactive UI)
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

    // Pure CRUD operations (no business logic)

    /**
     * Insert item to database
     * Returns generated item ID
     */
    suspend fun insertItem(item: InventoryItem): Long {
        val userId = requireUserId()
        val crewName = requireCrewName()

        val entity = item.copy(crewName = crewName).toEntity(userId = userId)
        val insertedId = dao.insertItem(entity)

        Log.d(TAG, "✅ Item inserted with ID: $insertedId")
        return insertedId
    }

    /**
     * Get item by ID
     */
    suspend fun getItemById(itemId: Long): InventoryItem? {
        val entity = dao.getItemById(itemId)
        return entity?.toDomain()
    }

    /**
     * Get Supabase ID for item
     * Used by Use Cases to enqueue sync operations
     */
    suspend fun getSupabaseId(itemId: Long): Long? {
        return dao.getItemById(itemId)?.supabaseId
    }

    /**
     * Update full item (all fields)
     */
    suspend fun updateFullItem(item: InventoryItem): Int {
        val userId = requireUserId()
        val crewName = requireCrewName()

        val existingItem = dao.getItemById(item.id)
            ?: throw IllegalArgumentException("Item with ID ${item.id} does not exist")

        validateItemOwnership(existingItem, crewName)

        val updatedRows = dao.updateItemWithNeeds(
            id = item.id,
            name = item.itemName.trim(),
            availableQuantity = item.availableQuantity,
            neededQuantity = item.neededQuantity,
            category = item.category,
            crewName = crewName
        )

        Log.d(TAG, "✅ Item updated: $updatedRows rows")
        return updatedRows
    }

    /**
     * Update single quantity field
     */
    suspend fun updateQuantity(
        itemId: Long,
        quantity: Int,
        quantityType: QuantityType
    ): Int {
        val userId = requireUserId()
        val crewName = requireCrewName()

        val existingItem = dao.getItemById(itemId)
            ?: throw IllegalArgumentException("Item with ID $itemId does not exist")

        validateItemOwnership(existingItem, crewName)

        val updatedRows = when (quantityType) {
            QuantityType.AVAILABLE -> dao.updateQuantity(itemId, quantity)
            QuantityType.NEEDED -> dao.updateNeededQuantity(itemId, quantity)
        }

        Log.d(TAG, "✅ Quantity updated: ${quantityType.name}=$quantity")
        return updatedRows
    }

    /**
     * Soft delete item
     */
    suspend fun softDeleteItem(itemId: Long): Int {
        val userId = requireUserId()
        val crewName = requireCrewName()

        val existingItem = dao.getItemById(itemId)
            ?: throw IllegalArgumentException("Item with ID $itemId does not exist")

        validateItemOwnership(existingItem, crewName)

        val deletedRows = dao.softDeleteItem(itemId)
        Log.d(TAG, "✅ Item soft deleted: $deletedRows rows")
        return deletedRows
    }

    /**
     * Validate that item belongs to current crew
     * Security check to prevent cross-crew modifications
     */
    private fun validateItemOwnership(
        item: com.lifelover.companion159.data.local.entities.InventoryItemEntity,
        crewName: String
    ) {
        if (item.crewName != crewName) {
            throw SecurityException("Cannot modify item from different crew")
        }
    }
}