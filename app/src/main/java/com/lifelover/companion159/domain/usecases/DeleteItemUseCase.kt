package com.lifelover.companion159.domain.usecases

import android.util.Log
import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.AppError
import com.lifelover.companion159.domain.models.InventoryItem
import javax.inject.Inject

/**
 * Use case for deleting inventory item
 *
 * Business rules:
 * - Soft delete (isActive = false)
 * - Item remains in database for sync
 * - User can only delete items from their crew
 * - Sync triggered automatically by repository
 *
 * @param repository Inventory repository for data persistence
 */
class DeleteItemUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    companion object {
        private const val TAG = "DeleteItemUseCase"
    }

    suspend operator fun invoke(item: InventoryItem): Result<Unit> {
        return try {
            Log.d(TAG, "=== DELETE ITEM USE CASE ===")
            Log.d(TAG, "Item ID: ${item.id}")
            Log.d(TAG, "Item name: '${item.itemName}'")
            Log.d(TAG, "Crew: ${item.crewName}")

            // Soft delete in repository
            // Repository will automatically trigger sync via callback
            repository.softDeleteItem(item.id)

            Log.d(TAG, "✅ Item soft deleted from database")
            Log.d(TAG, "✅ Sync triggered automatically by repository")

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