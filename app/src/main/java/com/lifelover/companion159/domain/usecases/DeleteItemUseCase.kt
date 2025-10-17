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
 * - Item must exist in database
 * - Soft delete (marks as inactive, doesn't physically remove)
 * - User can only delete items from their crew
 * - Delete operation automatically queued for sync
 * - Item removed from UI immediately after soft delete
 *
 * Responsibilities:
 * - Validate item exists
 * - Delegate to repository for soft delete
 * - Handle errors gracefully
 *
 * Note: Repository handles all deletion logic including:
 * - Ownership validation
 * - Soft delete (isActive = false)
 * - Sync queue enqueue
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
     * Delete item (soft delete)
     *
     * Flow:
     * 1. Log delete operation
     * 2. Call repository.deleteItem() (soft delete)
     * 3. Repository enqueues DELETE to sync queue
     *
     * @param item Item to delete
     * @return Result.success(Unit) on success, Result.failure(AppError) on error
     *
     * @throws IllegalStateException if user not authenticated or position not set (from repository)
     * @throws SecurityException if trying to delete other crew's item (from repository)
     */
    suspend operator fun invoke(item: InventoryItem): Result<Unit> {
        return try {
            Log.d(TAG, "=== DELETE ITEM USE CASE ===")
            Log.d(TAG, "Item ID: ${item.id}")
            Log.d(TAG, "Item name: '${item.itemName}'")
            Log.d(TAG, "Crew: ${item.crewName}")

            // Delete from repository (soft delete + sync queue)
            Log.d(TAG, "Deleting from repository...")
            repository.deleteItem(item.id)

            Log.d(TAG, "✅ Item deleted successfully")
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