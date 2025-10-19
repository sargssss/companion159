package com.lifelover.companion159.data.repository

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.entities.toDomain
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.QuantityType
import com.lifelover.companion159.domain.models.toEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository with direct sync triggering after each operation
 *
 * After every CRUD operation:
 * 1. Update local DB with needsSync=1
 * 2. Trigger immediate sync
 *
 * FIXED: Eliminated Flow wrapping anti-pattern and memory leaks
 */
@OptIn(ExperimentalCoroutinesApi::class)
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

    /**
     * FIXED: Direct map instead of wrapping Flow in flow
     * Benefits:
     * - No anti-pattern Flow wrapping
     * - Single subscription to DAO
     * - Automatic unsubscribe when collector disconnects
     * - Better memory management
     *
     * Flow chain:
     * DAO returns StateFlow<List<Entity>> (cold)
     * → flatMapLatest gets current userId + crewName
     * → map transforms Entity to Domain
     * → Result: Flow<List<InventoryItem>>
     */
    fun getAvailabilityItems(): Flow<List<InventoryItem>> {
        Log.d(TAG, "Creating flow: Availability items")

        return positionRepository.currentPosition
            .flatMapLatest { crewName ->
                // Only subscribe to DAO when crew name changes
                if (crewName == null) {
                    flowOf(emptyList())
                } else {
                    try {
                        val userId = requireUserId()
                        dao.getAvailabilityItems(userId, crewName)
                            .map { entities ->
                                // Transform domain layer once per emission
                                entities.map { it.toDomain() }
                            }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading availability items", e)
                        flowOf(emptyList())
                    }
                }
            }
            .distinctUntilChanged()  // Skip duplicate emissions
            .catch { error ->
                Log.e(TAG, "Error in availability flow", error)
                emit(emptyList())
            }
    }

    /**
     * FIXED: Same pattern as getAvailabilityItems
     * Returns ammunition items (availableQuantity > 0)
     */
    fun getAmmunitionItems(): Flow<List<InventoryItem>> {
        Log.d(TAG, "Creating flow: Ammunition items")

        return positionRepository.currentPosition
            .flatMapLatest { crewName ->
                if (crewName == null) {
                    flowOf(emptyList())
                } else {
                    try {
                        val userId = requireUserId()
                        dao.getAmmunitionItems(userId, crewName)
                            .map { entities ->
                                entities.map { it.toDomain() }
                            }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading ammunition items", e)
                        flowOf(emptyList())
                    }
                }
            }
            .distinctUntilChanged()
            .catch { error ->
                Log.e(TAG, "Error in ammunition flow", error)
                emit(emptyList())
            }
    }

    /**
     * FIXED: Same pattern as getAvailabilityItems
     * Returns needs items (neededQuantity > 0)
     */
    fun getNeedsItems(): Flow<List<InventoryItem>> {
        Log.d(TAG, "Creating flow: Needs items")

        return positionRepository.currentPosition
            .flatMapLatest { crewName ->
                if (crewName == null) {
                    flowOf(emptyList())
                } else {
                    try {
                        val userId = requireUserId()
                        dao.getNeedsItems(userId, crewName)
                            .map { entities ->
                                entities.map { it.toDomain() }
                            }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading needs items", e)
                        flowOf(emptyList())
                    }
                }
            }
            .distinctUntilChanged()
            .catch { error ->
                Log.e(TAG, "Error in needs flow", error)
                emit(emptyList())
            }
    }

    // ==========================================
    // CRUD OPERATIONS
    // ==========================================

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
        val updatedRows = dao.updateLocalItem(
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
     *
     * Execution flow:
     * 1. Validate item exists
     * 2. Validate item belongs to current crew
     * 3. Update DB with needsSync=1
     * 4. Trigger sync callback only after DB update succeeds
     * 5. Return number of updated rows
     *
     * Sync protection:
     * - Sync callback fires AFTER DB update completes
     * - Multiple rapid calls debounced at UI layer (500ms)
     * - Sync layer protected by mutex against concurrent operations
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

        Log.d(TAG, "✅ DB updated: $updatedRows rows affected")

        // CRITICAL: Trigger sync AFTER DB update succeeds
        // This ensures we only sync if DB operation was successful
        if (updatedRows > 0) {
            Log.d(TAG, "🔄 Triggering sync callback")
            onNeedsSyncCallback?.invoke()
        } else {
            Log.w(TAG, "⚠️ No rows updated - skipping sync trigger")
        }

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