package com.lifelover.companion159.domain.usecases

import android.util.Log
import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.QuantityType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Use case for loading inventory items filtered by display category
 *
 * Business rules:
 * - Only return items from current user's crew
 * - Filter by quantity based on display category:
 *   * Availability: items with availableQuantity > 0
 *   * Ammunition: items with availableQuantity > 0
 *   * Needs: items with neededQuantity > 0
 * - Return reactive Flow for real-time updates
 * - Empty list if no items match filters
 *
 * Responsibilities:
 * - Select correct repository method based on category
 * - Apply quantity filtering
 * - Provide reactive data stream
 *
 * Flow updates automatically when:
 * - New item added
 * - Item updated
 * - Item deleted
 * - Quantities changed
 *
 * @param repository Inventory repository for data access
 */
class LoadItemsUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    companion object {
        private const val TAG = "LoadItemsUseCase"
    }

    /**
     * Load items filtered by display category
     *
     * Flow:
     * 1. Select repository method based on category
     * 2. Apply quantity filtering (only items with relevant quantity > 0)
     * 3. Return Flow for reactive UI updates
     *
     * Category mapping:
     * - Availability → getAvailabilityItems() → filter availableQuantity > 0
     * - Ammunition → getAmmunitionItems() → filter availableQuantity > 0
     * - Needs → getNeedsItems() → filter neededQuantity > 0
     *
     * @param displayCategory Display category to filter by
     * @return Flow<List<InventoryItem>> - reactive stream of filtered items
     *
     * @throws IllegalStateException if user not authenticated or position not set (from repository)
     */
    operator fun invoke(displayCategory: DisplayCategory): Flow<List<InventoryItem>> {
        Log.d(TAG, "=== LOAD ITEMS USE CASE ===")
        Log.d(TAG, "Display category: ${displayCategory.name}")
        Log.d(TAG, "Quantity type: ${displayCategory.quantityType.name}")

        // Step 1: Get base flow from repository based on category
        val baseFlow: Flow<List<InventoryItem>> = when (displayCategory) {
            is DisplayCategory.Availability -> {
                Log.d(TAG, "Loading: Availability items (non-ammunition with availableQuantity > 0)")
                repository.getAvailabilityItems()
            }
            is DisplayCategory.Ammunition -> {
                Log.d(TAG, "Loading: Ammunition items (with availableQuantity > 0)")
                repository.getAmmunitionItems()
            }
            is DisplayCategory.Needs -> {
                Log.d(TAG, "Loading: Needs items (with neededQuantity > 0)")
                repository.getNeedsItems()
            }
        }

        // Step 2: Apply quantity filtering and logging
        return baseFlow
            .map { items ->
                filterItemsByQuantityType(items, displayCategory.quantityType)
            }
            .onEach { items ->
                Log.d(TAG, "✅ Loaded ${items.size} items for ${displayCategory.name}")
                if (items.isEmpty()) {
                    Log.d(TAG, "⚠️ No items found matching criteria")
                }
                Log.d(TAG, "===========================")
            }
    }

    /**
     * Filter items by quantity type
     *
     * Only returns items where the relevant quantity > 0:
     * - AVAILABLE → availableQuantity > 0
     * - NEEDED → neededQuantity > 0
     *
     * @param items List of items to filter
     * @param quantityType Type of quantity to check
     * @return Filtered list of items
     */
    private fun filterItemsByQuantityType(
        items: List<InventoryItem>,
        quantityType: QuantityType
    ): List<InventoryItem> {
        val filtered = when (quantityType) {
            QuantityType.AVAILABLE -> {
                items.filter { item ->
                    val hasQuantity = item.availableQuantity > 0
                    if (!hasQuantity) {
                        Log.v(TAG, "Filtered out '${item.itemName}': availableQuantity = ${item.availableQuantity}")
                    }
                    hasQuantity
                }
            }
            QuantityType.NEEDED -> {
                items.filter { item ->
                    val hasQuantity = item.neededQuantity > 0
                    if (!hasQuantity) {
                        Log.v(TAG, "Filtered out '${item.itemName}': neededQuantity = ${item.neededQuantity}")
                    }
                    hasQuantity
                }
            }
        }

        Log.d(TAG, "Filtering: ${items.size} → ${filtered.size} items (${quantityType.name} > 0)")
        return filtered
    }
}