package com.lifelover.companion159.domain.usecases

import android.util.Log
import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.AppError
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.domain.validation.InputValidator
import javax.inject.Inject

/**
 * Use case for updating single quantity field
 *
 * Business rules:
 * - Quantity must be valid (0-999999)
 * - Sync triggered even when quantity becomes 0
 * - Other quantity field remains unchanged
 *
 * CRITICAL: When user decreases quantity to 0:
 * - Item still exists in DB
 * - Other quantity might be > 0
 * - Full item update sent to server
 * - Item will disappear from UI (filtered by quantity > 0)
 *
 * @param repository Inventory repository for data persistence
 */
class UpdateQuantityUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    companion object {
        private const val TAG = "UpdateQuantityUseCase"
    }

    suspend operator fun invoke(
        itemId: Long,
        newQuantity: Int,
        displayCategory: DisplayCategory
    ): Result<Unit> {
        return try {
            Log.d(TAG, "=== UPDATE QUANTITY USE CASE ===")
            Log.d(TAG, "Item ID: $itemId")
            Log.d(TAG, "New quantity: $newQuantity")
            Log.d(TAG, "Quantity type: ${displayCategory.quantityType.name}")

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
                    // CRITICAL: Repository triggers sync even when quantity=0
                    // This ensures server knows about the change
                    repository.updateQuantity(
                        itemId = itemId,
                        quantity = validatedQuantity,
                        quantityType = quantityType
                    )

                    Log.d(TAG, "✅ Quantity updated in database")

                    if (validatedQuantity == 0) {
                        Log.d(TAG, "⚠️ Quantity is now 0 - item will disappear from current view")
                        Log.d(TAG, "   But full item update will be synced to server")
                    }

                    Log.d(TAG, "✅ Sync triggered automatically by repository")

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