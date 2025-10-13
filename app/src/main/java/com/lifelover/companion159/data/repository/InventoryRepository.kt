package com.lifelover.companion159.data.repository

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.entities.toDomain
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.toEntity
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for inventory operations
 * Simplified: no interface, uses extension functions instead of Mapper
 *
 * All operations require authenticated user
 * Works with local database only (offline-first)
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

    /**
     * Get current authenticated userId or throw exception
     */
    private fun requireUserId(): String {
        return authService.getUserId()
            ?: throw IllegalStateException("User must be authenticated to access inventory")
    }

    // ============================================================
    // QUERY METHODS
    // ============================================================

    /**
     * Get items for AVAILABILITY screen
     * Shows EQUIPMENT with availableQuantity > 0
     */
    fun getAvailabilityItems(): Flow<List<InventoryItem>> {
        return flow {
            val userId = requireUserId()
            dao.getAvailabilityItems(userId).collect { entities ->
                emit(entities.map { it.toDomain() })
            }
        }
    }

    /**
     * Get items for AMMUNITION screen
     * Shows AMMUNITION items
     */
    fun getAmmunitionItems(): Flow<List<InventoryItem>> {
        return flow {
            val userId = requireUserId()
            dao.getAmmunitionItems(userId).collect { entities ->
                emit(entities.map { it.toDomain() })
            }
        }
    }

    /**
     * Get items for NEEDS screen
     * Shows all items with neededQuantity > 0
     */
    fun getNeedsItems(): Flow<List<InventoryItem>> {
        return flow {
            val userId = requireUserId()
            dao.getNeedsItems(userId).collect { entities ->
                emit(entities.map { it.toDomain() })
            }
        }
    }

    /**
     * Get all items (for export/analysis)
     */
    suspend fun getAllItemsOnce(): List<InventoryItem> {
        val userId = requireUserId()
        return dao.getAllItems(userId).map { it.toDomain() }
    }

    // ============================================================
    // CREATE / UPDATE / DELETE
    // ============================================================

    /**
     * Add new item
     */
    suspend fun addItem(item: InventoryItem) {
        val userId = requireUserId()
        val crewName = positionRepository.getPosition()
            ?: throw IllegalStateException("Position must be set before adding items")

        val entity = item.copy(crewName = crewName).toEntity(userId = userId)

        val insertedId = dao.insertItem(entity)
        Log.d(TAG, "✅ Item created with ID: $insertedId for user: $userId")
    }

    /**
     * Update full item (name + both quantities)
     */
    suspend fun updateItem(item: InventoryItem) {
        val userId = requireUserId()
        val crewName = positionRepository.getPosition()
            ?: throw IllegalStateException("Position must be set")

        val existingItem = dao.getItemById(item.id)
        if (existingItem == null) {
            throw IllegalArgumentException("Item with ID ${item.id} does not exist")
        }

        // Ensure user owns this item
        if (existingItem.userId != userId) {
            throw SecurityException("Cannot update item of another user")
        }

        val updatedRows = dao.updateItemWithNeeds(
            id = item.id,
            name = item.itemName.trim(),
            availableQuantity = item.availableQuantity,
            neededQuantity = item.neededQuantity,
            category = item.category,
            crewName = crewName
        )

        if (updatedRows > 0) {
            Log.d(TAG, "✅ Item updated: $updatedRows rows")
        }
    }

    /**
     * Update available quantity only
     */
    suspend fun updateItemQuantity(itemId: Long, quantity: Int) {
        val userId = requireUserId()

        val existingItem = dao.getItemById(itemId)
        if (existingItem == null) {
            Log.e(TAG, "❌ Item with ID $itemId does NOT exist")
            return
        }

        // Ensure user owns this item
        if (existingItem.userId != userId) {
            throw SecurityException("Cannot update item of another user")
        }

        val updatedRows = dao.updateQuantity(itemId, quantity)

        if (updatedRows > 0) {
            Log.d(TAG, "✅ Available quantity updated: $itemId -> $quantity")
        } else {
            Log.e(TAG, "❌ Failed to update available quantity")
        }
    }

    /**
     * Update needed quantity only
     */
    suspend fun updateNeededQuantity(itemId: Long, quantity: Int) {
        val userId = requireUserId()

        val existingItem = dao.getItemById(itemId)
        if (existingItem == null) {
            Log.e(TAG, "❌ Item $itemId not found")
            return
        }

        // Ensure user owns this item
        if (existingItem.userId != userId) {
            throw SecurityException("Cannot update item of another user")
        }

        dao.updateNeededQuantity(itemId, quantity)
        Log.d(TAG, "✅ Needed quantity updated: $itemId -> $quantity")
    }

    /**
     * Delete item (soft delete)
     */
    suspend fun deleteItem(id: Long) {
        val userId = requireUserId()

        val existingItem = dao.getItemById(id)
        if (existingItem == null) {
            Log.w(TAG, "⚠️ Item $id not found")
            return
        }

        // Ensure user owns this item
        if (existingItem.userId != userId) {
            throw SecurityException("Cannot delete item of another user")
        }

        dao.softDeleteItem(id)
        Log.d(TAG, "✅ Item deleted: $id")
    }
}