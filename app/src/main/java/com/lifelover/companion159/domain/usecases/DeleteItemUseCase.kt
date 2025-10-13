package com.lifelover.companion159.domain.usecases

import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.domain.models.InventoryItem
import javax.inject.Inject

/**
 * Use case for smart item deletion
 *
 * Business rules:
 * - If deleting from Availability and neededQuantity > 0: set availableQuantity = 0
 * - If deleting from Ammunition and neededQuantity > 0: set availableQuantity = 0
 * - If deleting from Needs and availableQuantity > 0: set neededQuantity = 0
 * - Only delete completely if both quantities are 0
 */
class DeleteItemUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    /**
     * Delete or zero-out item based on category and quantities
     *
     * @param item Item to delete
     * @param displayCategory Category where deletion was triggered
     * @return Result with success/failure
     */
    suspend operator fun invoke(
        item: InventoryItem,
        displayCategory: DisplayCategory
    ): Result<Unit> {
        return try {
            when (displayCategory) {
                is DisplayCategory.Availability,
                is DisplayCategory.Ammunition -> {
                    // Deleting from Availability/Ammunition
                    if (item.neededQuantity > 0) {
                        // Has needs - zero out available quantity
                        val updatedItem = item.copy(availableQuantity = 0)
                        repository.updateItem(updatedItem)
                    } else {
                        // No needs - delete completely
                        repository.deleteItem(item.id)
                    }
                }

                is DisplayCategory.Needs -> {
                    // Deleting from Needs
                    if (item.availableQuantity > 0) {
                        // Has available - zero out needed quantity
                        val updatedItem = item.copy(neededQuantity = 0)
                        repository.updateItem(updatedItem)
                    } else {
                        // No available - delete completely
                        repository.deleteItem(item.id)
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}