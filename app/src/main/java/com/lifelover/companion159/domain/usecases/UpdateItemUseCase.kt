package com.lifelover.companion159.domain.usecases

import android.util.Log
import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.AppError
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.toStorageCategory
import com.lifelover.companion159.domain.validation.InputValidator
import javax.inject.Inject

/**
 * Use case for updating full inventory item
 *
 * Business rules:
 * - All fields must be validated
 * - Item must exist
 * - User can only update items from their crew
 * - Sync triggered automatically by repository
 *
 * @param repository Inventory repository for data persistence
 */
class UpdateItemUseCase @Inject constructor(
    private val repository: InventoryRepository
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
            Log.d(TAG, "Input: name='$newName', available=$newAvailableQuantity, needed=$newNeededQuantity")

            // Step 1: Validate input
            val validationResult = InputValidator.validateNewItem(
                name = newName,
                availableQuantity = newAvailableQuantity,
                neededQuantity = newNeededQuantity
            )

            validationResult.fold(
                onSuccess = { validated ->
                    Log.d(TAG, "✅ Validation passed")

                    // Step 2: Map category
                    val storageCategory = displayCategory.toStorageCategory()
                    Log.d(TAG, "Mapped: ${displayCategory.name} → ${storageCategory.name}")

                    // Step 3: Create updated domain model
                    val item = InventoryItem(
                        id = itemId,
                        itemName = validated.name,
                        availableQuantity = validated.availableQuantity,
                        neededQuantity = validated.neededQuantity,
                        category = storageCategory,
                        crewName = "" // Validated by repository
                    )

                    // Step 4: Update in repository
                    // Repository will automatically trigger sync via callback
                    repository.updateFullItem(item)
                    Log.d(TAG, "✅ Item updated in database")
                    Log.d(TAG, "✅ Sync triggered automatically by repository")

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