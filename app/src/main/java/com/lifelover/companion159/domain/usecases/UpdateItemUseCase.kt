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
 * Use case for updating existing inventory item (full update)
 *
 * Business rules:
 * - Item must exist in database
 * - Name cannot be empty and must be 1-100 characters
 * - Quantities must be non-negative (0-999999)
 * - User can only update items from their crew
 * - Changes automatically queued for sync
 *
 * Responsibilities:
 * - Validate all input fields
 * - Map display category to storage category
 * - Create updated domain model
 * - Delegate to repository for persistence
 *
 * Use this when:
 * - Editing item through AddEditItemScreen
 * - Updating name AND quantities together
 *
 * For single quantity updates, use UpdateQuantityUseCase instead
 *
 * @param repository Inventory repository for data persistence
 */
class UpdateItemUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    companion object {
        private const val TAG = "UpdateItemUseCase"
    }

    /**
     * Update existing item with validation
     *
     * Flow:
     * 1. Validate input (name, quantities)
     * 2. Map display category → storage category
     * 3. Create updated InventoryItem domain model
     * 4. Save to repository (triggers sync queue)
     *
     * @param itemId ID of item to update (must exist)
     * @param newName New item name (will be trimmed)
     * @param newAvailableQuantity New available quantity (>= 0)
     * @param newNeededQuantity New needed quantity (>= 0)
     * @param displayCategory Display category (Availability/Ammunition/Needs)
     * @return Result.success(Unit) on success, Result.failure(AppError) on validation/persistence error
     *
     * @throws IllegalStateException if user not authenticated or position not set (from repository)
     * @throws IllegalArgumentException if item with itemId doesn't exist (from repository)
     * @throws SecurityException if trying to modify other crew's item (from repository)
     */
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
                    Log.d(TAG, "Validated: name='${validated.name}', available=${validated.availableQuantity}, needed=${validated.neededQuantity}")

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

                    // Step 4: Update in repository
                    Log.d(TAG, "Updating in repository...")
                    repository.updateItem(item)

                    Log.d(TAG, "✅ Item updated successfully")
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