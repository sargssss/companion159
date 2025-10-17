package com.lifelover.companion159.domain.usecases

import android.util.Log
import com.lifelover.companion159.data.remote.sync.SyncQueueManager
import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.AppError
import com.lifelover.companion159.domain.models.InventoryItem
import javax.inject.Inject

class DeleteItemUseCase @Inject constructor(
    private val repository: InventoryRepository,
    private val syncQueueManager: SyncQueueManager
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

            // Step 1: Get supabaseId before deletion
            val supabaseId = repository.getSupabaseId(item.id)

            // Step 2: Delete from repository (soft delete - pure data operation)
            Log.d(TAG, "Deleting from repository...")
            repository.softDeleteItem(item.id)
            Log.d(TAG, "✅ Item soft deleted from database")

            // Step 3: Enqueue for sync (business logic)
            Log.d(TAG, "Enqueueing for sync...")
            syncQueueManager.enqueueDelete(
                localItemId = item.id,
                supabaseId = supabaseId
            )
            Log.d(TAG, "✅ Delete enqueued for sync")

            Log.d(TAG, "✅ Delete completed successfully")
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