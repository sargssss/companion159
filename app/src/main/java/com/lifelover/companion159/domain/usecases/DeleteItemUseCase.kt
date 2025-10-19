package com.lifelover.companion159.domain.usecases

import android.util.Log
import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.AppError
import com.lifelover.companion159.domain.models.DisplayCategory
import javax.inject.Inject

/**
 * Use case for deleting item from specific category
 *
 * Business rules:
 * - Set relevant quantity to 0 (don't soft delete item)
 * - Item remains in DB with other quantity intact
 * - If both quantities become 0, item can be soft deleted later
 * - Sync triggered automatically by repository
 *
 * Why not soft delete:
 * Item has TWO independent quantities (available + needed)
 * Deleting from Availability doesn't mean delete from Needs
 * Example: availableQuantity=5, neededQuantity=3
 * - Delete from Availability → availableQuantity=0, neededQuantity=3 (still visible in Needs)
 * - Delete from Needs → availableQuantity=5, neededQuantity=0 (still visible in Availability)
 *
 * @param repository Inventory repository for data persistence
 */
class DeleteItemUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    companion object {
        private const val TAG = "DeleteItemUseCase"
    }

    /**
     * Delete item from specific display category
     *
     * Sets the relevant quantity to 0 based on category:
     * - Availability/Ammunition → set availableQuantity=0
     * - Needs → set neededQuantity=0
     *
     * @param itemId Item to delete
     * @param displayCategory Category being deleted from
     * @return Success if quantity was set to 0, Failure on error
     */
    suspend operator fun invoke(
        itemId: Long,
        displayCategory: DisplayCategory
    ): Result<Unit> {
        return try {
            Log.d(TAG, "=== DELETE ITEM USE CASE ===")
            Log.d(TAG, "Item ID: $itemId")
            Log.d(TAG, "Display category: ${displayCategory.name}")

            // Determine which quantity to zero based on category
            val quantityType = displayCategory.quantityType
            Log.d(TAG, "Setting ${quantityType.name} to 0")

            // Set relevant quantity to 0
            repository.updateQuantity(
                itemId = itemId,
                quantity = 0,
                quantityType = quantityType
            )

            Log.d(TAG, "✅ Item quantity set to 0 from database")
            Log.d(TAG, "✅ Sync triggered automatically by repository")
            Log.d(TAG, "📝 Item remains in DB with other quantity intact")

            Log.d(TAG, "============================")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to delete item", e)
            Log.d(TAG, "============================")
            Result.failure(
                if (e is AppError) e
                else AppError.Unknown("Failed to delete item: ${e.message}", e)
            )
        }
    }
}