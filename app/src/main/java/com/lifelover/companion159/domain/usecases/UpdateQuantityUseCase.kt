package com.lifelover.companion159.domain.usecases

import android.util.Log
import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.AppError
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.domain.validation.InputValidator
import javax.inject.Inject

/**
 * Use case for updating single quantity (available or needed)
 *
 * Business rules:
 * - Item must exist in database
 * - Quantity must be non-negative (0-999999)
 * - Display category determines which quantity to update:
 *   * Availability/Ammunition → availableQuantity
 *   * Needs → neededQuantity
 * - User can only update items from their crew
 * - Changes automatically queued for sync
 *
 * Responsibilities:
 * - Validate quantity input
 * - Determine quantity type from display category
 * - Delegate to repository for persistence
 *
 * Use this when:
 * - Quick quantity updates via +/- buttons in InventoryItemCard
 * - Only one quantity field needs updating
 *
 * For full item updates (name + quantities), use UpdateItemUseCase instead
 *
 * @param repository Inventory repository for data persistence
 */
class UpdateQuantityUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    companion object {
        private const val TAG = "UpdateQuantityUseCase"
    }

    /**
     * Update single quantity with validation
     *
     * Flow:
     * 1. Validate quantity
     * 2. Determine quantity type from display category
     * 3. Update in repository (triggers sync queue)
     *
     * Quantity type mapping:
     * - Availability → QuantityType.AVAILABLE
     * - Ammunition → QuantityType.AVAILABLE
     * - Needs → QuantityType.NEEDED
     *
     * @param itemId ID of item to update (must exist)
     * @param newQuantity New quantity value (>= 0)
     * @param displayCategory Display category (determines which quantity to update)
     * @return Result.success(Unit) on success, Result.failure(AppError) on validation/persistence error
     *
     * @throws IllegalStateException if user not authenticated or position not set (from repository)
     * @throws SecurityException if trying to modify other crew's item (from repository)
     */
    suspend operator fun invoke(
        itemId: Long,
        newQuantity: Int,
        displayCategory: DisplayCategory
    ): Result<Unit> {
        return try {
            Log.d(TAG, "=== UPDATE QUANTITY USE CASE ===")
            Log.d(TAG, "Item ID: $itemId")
            Log.d(TAG, "New quantity: $newQuantity")
            Log.d(TAG, "Display category: ${displayCategory.name}")

            // Step 1: Validate quantity
            val fieldName = displayCategory.quantityType.name.lowercase()
            val validationResult = InputValidator.validateQuantity(
                quantity = newQuantity,
                fieldName = fieldName
            )

            validationResult.fold(
                onSuccess = { validatedQuantity ->
                    Log.d(TAG, "✅ Validation passed: $validatedQuantity")

                    // Step 2: Get quantity type from display category
                    val quantityType = displayCategory.quantityType
                    Log.d(TAG, "Quantity type: ${quantityType.name}")

                    // Step 3: Update in repository
                    Log.d(TAG, "Updating in repository...")
                    repository.updateSingleQuantity(
                        itemId = itemId,
                        quantity = validatedQuantity,
                        quantityType = quantityType
                    )

                    Log.d(TAG, "✅ Quantity updated successfully")
                    Log.d(TAG, "================================")
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Log.w(TAG, "⚠️ Validation failed: ${error.message}")
                    Log.d(TAG, "================================")
                    Result.failure(error)
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update quantity", e)
            Log.d(TAG, "================================")
            Result.failure(
                if (e is AppError) e
                else AppError.Unknown("Failed to update quantity: ${e.message}", e)
            )
        }
    }
}