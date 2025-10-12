package com.lifelover.companion159.data.repository

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.mappers.InventoryMapper
import com.lifelover.companion159.domain.models.InventoryItem
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for inventory operations
 * Handles all CRUD operations for inventory items
 */
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
}

/**
 * Implementation of inventory repository
 * Works with local database only (offline-first approach)
 */
@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val localDao: InventoryDao,
    private val positionRepository: PositionRepository
) : InventoryRepository {

    companion object {
        private const val TAG = "InventoryRepository"
    }

    override fun getAvailabilityItems(): Flow<List<InventoryItem>> {
        return localDao.getAvailabilityItems(null)
            .map { entities -> entities.map { InventoryMapper.toDomain(it) } }
    }

    override fun getAmmunitionItems(): Flow<List<InventoryItem>> {
        return localDao.getAmmunitionItems(null)
            .map { entities -> entities.map { InventoryMapper.toDomain(it) } }
    }

    override fun getNeedsItems(): Flow<List<InventoryItem>> {
        return localDao.getNeedsItems(null)
            .map { entities -> entities.map { InventoryMapper.toDomain(it) } }
    }

    override suspend fun getAllItemsOnce(): List<InventoryItem> {
        return localDao.getAllItems(null).map { InventoryMapper.toDomain(it) }
    }

    override suspend fun addItem(item: InventoryItem) {
        val crewName = positionRepository.getPosition() ?: "Default"

        val entity = InventoryMapper.toEntity(
            domain = item.copy(crewName = crewName),
            userId = null
        )

        val insertedId = localDao.insertItem(entity)
        Log.d(TAG, "✅ Item created with ID: $insertedId")
    }

    override suspend fun updateItem(item: InventoryItem) {
        val crewName = positionRepository.getPosition() ?: "Default"

        val existingItem = localDao.getItemById(item.id)
        if (existingItem == null) {
            throw IllegalArgumentException("Item with ID ${item.id} does not exist")
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
    }

    /**
     * Update available quantity for item
     * Used by AVAILABILITY and AMMUNITION screens
     */
    override suspend fun updateItemQuantity(itemId: Long, quantity: Int) {
        val existingItem = localDao.getItemById(itemId)
        if (existingItem == null) {
            Log.e(TAG, "❌ Item with ID $itemId does NOT exist")
            return
        }

        val updatedRows = localDao.updateQuantity(itemId, quantity)

        if (updatedRows > 0) {
            Log.d(TAG, "✅ Available quantity updated: $itemId -> $quantity")
        } else {
            Log.e(TAG, "❌ Failed to update available quantity")
        }
    }

    /**
     * Update needed quantity for item
     * Used by NEEDS screen
     */
    override suspend fun updateNeededQuantity(itemId: Long, quantity: Int) {
        val existingItem = localDao.getItemById(itemId)
        if (existingItem == null) {
            Log.e(TAG, "❌ Item $itemId not found")
            return
        }

        localDao.updateNeededQuantity(itemId, quantity)
        Log.d(TAG, "✅ Needed quantity updated: $itemId -> $quantity")
    }

    /**
     * Soft delete item (sets isActive = false)
     */
    override suspend fun deleteItem(id: Long) {
        val existingItem = localDao.getItemById(id)
        if (existingItem == null) {
            Log.w(TAG, "⚠️ Item $id not found")
            return
        }

        localDao.softDeleteItem(id)
        Log.d(TAG, "✅ Item deleted: $id")
    }
}