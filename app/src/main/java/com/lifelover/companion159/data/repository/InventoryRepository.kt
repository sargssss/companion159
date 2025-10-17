package com.lifelover.companion159.data.repository

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.entities.toDomain
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.remote.sync.SyncManager
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.QuantityType
import com.lifelover.companion159.domain.models.toEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepository @Inject constructor(
    private val dao: InventoryDao,
    private val positionRepository: PositionRepository,
    private val authService: SupabaseAuthService,
    private val syncManager: SyncManager
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

    suspend fun addItem(item: InventoryItem) {
        val userId = requireUserId()
        val crewName = requireCrewName()

        val entity = item.copy(crewName = crewName).toEntity(userId = userId)
        val insertedId = dao.insertItem(entity)
        Log.d(TAG, "✅ Item created with ID: $insertedId")

        triggerLightweightUpload()
    }

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
            triggerLightweightUpload()
        }
    }

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

        // Update FULL item to trigger needsSync flag
        val updatedItem = when (quantityType) {
            QuantityType.AVAILABLE -> existingItem.copy(
                availableQuantity = quantity,
                needsSync = true,
                lastModified = Date()
            )
            QuantityType.NEEDED -> existingItem.copy(
                neededQuantity = quantity,
                needsSync = true,
                lastModified = Date()
            )
        }

        val updatedRows = dao.updateItemWithNeeds(
            id = updatedItem.id,
            name = updatedItem.itemName,
            availableQuantity = updatedItem.availableQuantity,
            neededQuantity = updatedItem.neededQuantity,
            category = updatedItem.category,
            crewName = updatedItem.crewName
        )

        if (updatedRows > 0) {
            Log.d(TAG, "========================================")
            Log.d(TAG, "✅ Item quantity updated: ${quantityType.name}=$quantity")
            Log.d(TAG, "   Item ID: $itemId")
            Log.d(TAG, "   Available: ${updatedItem.availableQuantity}")
            Log.d(TAG, "   Needed: ${updatedItem.neededQuantity}")
            Log.d(TAG, "   needsSync: ${updatedItem.needsSync}")
            Log.d(TAG, "   crewName: ${updatedItem.crewName}")
            Log.d(TAG, "   Calling triggerLightweightUpload()...")
            Log.d(TAG, "========================================")

            triggerLightweightUpload()
        } else {
            Log.e(TAG, "❌ No rows updated!")
        }
    }

    private fun triggerLightweightUpload() {
        Log.d(TAG, "triggerLightweightUpload() called")
        val canSync = syncManager.canSync()
        Log.d(TAG, "  canSync: $canSync")

        if (canSync) {
            viewModelScope.launch {
                // Wait for DB transaction to complete
                delay(100)  // 100ms delay

                Log.d(TAG, "  ✅ Calling syncManager.uploadOnlySync()")
                syncManager.uploadOnlySync()
            }
        } else {
            Log.d(TAG, "  ⏭️ Offline - changes queued")
        }
    }

    suspend fun updateItemQuantity(itemId: Long, quantity: Int) {
        updateSingleQuantity(itemId, quantity, QuantityType.AVAILABLE)
    }

    suspend fun updateNeededQuantity(itemId: Long, quantity: Int) {
        updateSingleQuantity(itemId, quantity, QuantityType.NEEDED)
    }

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

        triggerLightweightUpload()
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