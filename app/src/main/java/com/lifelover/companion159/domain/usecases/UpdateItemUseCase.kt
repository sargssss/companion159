package com.lifelover.companion159.domain.usecases

import android.util.Log
import com.lifelover.companion159.data.remote.sync.SyncQueueManager
import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.AppError
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.toStorageCategory
import com.lifelover.companion159.domain.validation.InputValidator
import javax.inject.Inject

class UpdateItemUseCase @Inject constructor(
    private val repository: InventoryRepository,
    private val syncQueueManager: SyncQueueManager
) {
    companion object {
        private const val TAG = "UpdateItemUseCase"
    }

    suspend operator fun invoke(
        itemId: Long,
        newName: String,
        newAvailableQuantity: Int,
        newNeededQuantity: Int,
        displayCategory: DisplayCategory
    ): Result<Unit> {
        return try {
            Log.d(TAG, "=== UPDATE ITEM USE CASE ===")
            Log.d(TAG, "Item ID: $itemId")
            Log.d(TAG, "Input: name='$newName', available=$newAvailableQuantity, needed=$newNeededQuantity, category=${displayCategory.name}")

            // Step 1: Validate input
            val validationResult = InputValidator.validateNewItem(
                name = newName,
                availableQuantity = newAvailableQuantity,
                neededQuantity = newNeededQuantity
            )

            validationResult.fold(
                onSuccess = { validated ->
                    Log.d(TAG, "✅ Validation passed")

                    // Step 2: Map display category to storage category
                    val storageCategory = displayCategory.toStorageCategory()
                    Log.d(TAG, "Mapped: ${displayCategory.name} → ${storageCategory.name}")

                    // Step 3: Create updated domain model
                    val item = InventoryItem(
                        id = itemId,
                        itemName = validated.name,
                        availableQuantity = validated.availableQuantity,
                        neededQuantity = validated.neededQuantity,
                        category = storageCategory,
                        crewName = "" // Will be validated by repository
                    )

                    // Step 4: Update in repository (pure data operation)
                    Log.d(TAG, "Updating in repository...")
                    repository.updateFullItem(item)
                    Log.d(TAG, "✅ Item updated in database")

                    // Step 5: Get supabaseId for sync
                    val supabaseId = repository.getSupabaseId(itemId)

                    // Step 6: Enqueue for sync (business logic)
                    Log.d(TAG, "Enqueueing for sync...")
                    syncQueueManager.enqueueUpdate(
                        localItemId = itemId,
                        supabaseId = supabaseId
                    )
                    Log.d(TAG, "✅ Item enqueued for sync")

                    Log.d(TAG, "✅ Update completed successfully")
                    Log.d(TAG, "============================")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.w(TAG, "⚠️ Validation failed: ${error.message}")
                    Log.d(TAG, "============================")
                    Result.failure(error)
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update item", e)
            Log.d(TAG, "============================")
            Result.failure(
                if (e is AppError) e
                else AppError.Unknown("Failed to update item: ${e.message}", e)
            )
        }
    }
}