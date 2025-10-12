package com.lifelover.companion159.data.repository

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.mappers.InventoryMapper
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.domain.models.InventoryItem
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository interface for inventory operations
 * All operations require authenticated user
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
 * Requires user authentication to access data
 */
@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val localDao: InventoryDao,
    private val positionRepository: PositionRepository,
    private val authService: SupabaseAuthService
) : InventoryRepository {

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

    override fun getAvailabilityItems(): Flow<List<InventoryItem>> {
        return flow {
            val userId = requireUserId()
            localDao.getAvailabilityItems(userId).collect { entities ->
                emit(entities.map { InventoryMapper.toDomain(it) })
            }
        }
    }

    override fun getAmmunitionItems(): Flow<List<InventoryItem>> {
        return flow {
            val userId = requireUserId()
            localDao.getAmmunitionItems(userId).collect { entities ->
                emit(entities.map { InventoryMapper.toDomain(it) })
            }
        }
    }

    override fun getNeedsItems(): Flow<List<InventoryItem>> {
        return flow {
            val userId = requireUserId()
            localDao.getNeedsItems(userId).collect { entities ->
                emit(entities.map { InventoryMapper.toDomain(it) })
            }
        }
    }

    override suspend fun getAllItemsOnce(): List<InventoryItem> {
        val userId = requireUserId()
        return localDao.getAllItems(userId).map { InventoryMapper.toDomain(it) }
    }

    override suspend fun addItem(item: InventoryItem) {
        val userId = requireUserId()
        val crewName = positionRepository.getPosition()
            ?: throw IllegalStateException("Position must be set before adding items")

        val entity = InventoryMapper.toEntity(
            domain = item.copy(crewName = crewName),
            userId = userId
        )

        val insertedId = localDao.insertItem(entity)
        Log.d(TAG, "✅ Item created with ID: $insertedId for user: $userId")
    }

    override suspend fun updateItem(item: InventoryItem) {
        val userId = requireUserId()
        val crewName = positionRepository.getPosition()
            ?: throw IllegalStateException("Position must be set")

        val existingItem = localDao.getItemById(item.id)
        if (existingItem == null) {
            throw IllegalArgumentException("Item with ID ${item.id} does not exist")
        }

        // Ensure user owns this item
        if (existingItem.userId != userId) {
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
    }

    override suspend fun updateItemQuantity(itemId: Long, quantity: Int) {
        val userId = requireUserId()

        val existingItem = localDao.getItemById(itemId)
        if (existingItem == null) {
            Log.e(TAG, "❌ Item with ID $itemId does NOT exist")
            return
        }

        // Ensure user owns this item
        if (existingItem.userId != userId) {
            throw SecurityException("Cannot update item of another user")
        }

        val updatedRows = localDao.updateQuantity(itemId, quantity)

        if (updatedRows > 0) {
            Log.d(TAG, "✅ Available quantity updated: $itemId -> $quantity")
        } else {
            Log.e(TAG, "❌ Failed to update available quantity")
        }
    }

    override suspend fun updateNeededQuantity(itemId: Long, quantity: Int) {
        val userId = requireUserId()

        val existingItem = localDao.getItemById(itemId)
        if (existingItem == null) {
            Log.e(TAG, "❌ Item $itemId not found")
            return
        }

        // Ensure user owns this item
        if (existingItem.userId != userId) {
            throw SecurityException("Cannot update item of another user")
        }

        localDao.updateNeededQuantity(itemId, quantity)
        Log.d(TAG, "✅ Needed quantity updated: $itemId -> $quantity")
    }

    override suspend fun deleteItem(id: Long) {
        val userId = requireUserId()

        val existingItem = localDao.getItemById(id)
        if (existingItem == null) {
            Log.w(TAG, "⚠️ Item $id not found")
            return
        }

        // Ensure user owns this item
        if (existingItem.userId != userId) {
            throw SecurityException("Cannot delete item of another user")
        }

        localDao.softDeleteItem(id)
        Log.d(TAG, "✅ Item deleted: $id")
    }
}