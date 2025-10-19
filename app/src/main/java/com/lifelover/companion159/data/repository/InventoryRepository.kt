package com.lifelover.companion159.data.repository

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.entities.toDomain
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.QuantityType
import com.lifelover.companion159.domain.models.toEntity
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository with direct sync triggering after each operation
 *
 * After every CRUD operation:
 * 1. Update local DB with needsSync=1
 * 2. Trigger immediate sync
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
     * Sync trigger callback
     * Set by SyncManager during initialization
     */
    var onNeedsSyncCallback: (() -> Unit)? = null

    private fun requireUserId(): String {
        return authService.getUserId()
            ?: throw IllegalStateException("User must be authenticated")
    }

    private fun requireCrewName(): String {
        return positionRepository.currentPosition.value
            ?: throw IllegalStateException("Position must be set")
    }

    // Query methods
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

    /**
     * Insert item and trigger sync
     */
    suspend fun insertItem(item: InventoryItem): Long {
        val userId = requireUserId()
        val crewName = requireCrewName()

        val entity = item.copy(crewName = crewName).toEntity(userId = userId)
        val insertedId = dao.insertItem(entity)

        Log.d(TAG, "✅ Item inserted: ID=$insertedId, triggering sync")

        // Trigger sync immediately
        onNeedsSyncCallback?.invoke()

        return insertedId
    }

    /**
     * Update full item and trigger sync
     *
     * CRITICAL: Always updates ALL fields including quantities
     * Even if quantity=0, we must send update to server
     */
    suspend fun updateFullItem(item: InventoryItem): Int {
        val userId = requireUserId()
        val crewName = requireCrewName()

        val existingItem = dao.getItemById(item.id)
            ?: throw IllegalArgumentException("Item ${item.id} not found")

        validateItemOwnership(existingItem, crewName)

        // Update with needsSync=1
        val updatedRows = dao.updateItemWithNeeds(
            id = item.id,
            name = item.itemName.trim(),
            availableQuantity = item.availableQuantity,
            neededQuantity = item.neededQuantity,
            category = item.category,
            crewName = crewName
        )

        Log.d(TAG, "✅ Item updated: $updatedRows rows, triggering sync")
        Log.d(
            TAG,
            "   Quantities: available=${item.availableQuantity}, needed=${item.neededQuantity}"
        )

        onNeedsSyncCallback?.invoke()

        return updatedRows
    }

    /**
     * Update single quantity and trigger sync
     *
     * CRITICAL: When quantity becomes 0, still send update
     * The other quantity might be > 0
     */
    suspend fun updateQuantity(
        itemId: Long,
        quantity: Int,
        quantityType: QuantityType
    ): Int {
        val userId = requireUserId()
        val crewName = requireCrewName()

        val existingItem = dao.getItemById(itemId)
            ?: throw IllegalArgumentException("Item $itemId not found")

        validateItemOwnership(existingItem, crewName)

        // Update quantity with needsSync=1
        val updatedRows = when (quantityType) {
            QuantityType.AVAILABLE -> dao.updateQuantity(itemId, quantity)
            QuantityType.NEEDED -> dao.updateNeededQuantity(itemId, quantity)
        }

        Log.d(TAG, "✅ Quantity updated: ${quantityType.name}=$quantity")
        Log.d(TAG, "   Item: ${existingItem.itemName} (ID=$itemId)")

        // CRITICAL: Always sync even when quantity=0
        // The item still exists and other quantity might be > 0
        onNeedsSyncCallback?.invoke()

        return updatedRows
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