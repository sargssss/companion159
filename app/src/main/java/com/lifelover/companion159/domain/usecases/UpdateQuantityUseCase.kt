package com.lifelover.companion159.domain.usecases

import android.util.Log
import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.AppError
import com.lifelover.companion159.domain.models.QuantityType
import com.lifelover.companion159.domain.validation.InputValidator
import javax.inject.Inject

/**
 * Use case for updating single quantity field
 *
 * Business rules:
 * - Quantity must be valid (0-999999)
 * - Sync triggered only after successful DB update
 * - Other quantity field remains unchanged
 * - Validates input BEFORE database update
 *
 * CRITICAL: When user decreases quantity to 0:
 * - Item still exists in DB
 * - Other quantity might be > 0
 * - Full item update sent to server
 * - Item will disappear from UI (filtered by quantity > 0)
 *
 * Race condition protection:
 * - Repository uses mutex in sync layer
 * - Sync only triggers AFTER DB update completes
 * - Multiple rapid calls are debounced at UI layer
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
        quantityType: QuantityType
    ): Result<Unit> {
        return try {
            Log.d(TAG, "=== UPDATE QUANTITY USE CASE ===")
            Log.d(TAG, "Item ID: $itemId")
            Log.d(TAG, "New ${quantityType.name}: $newQuantity")

            // Step 1: Validate quantity BEFORE any DB operation
            val validationResult = InputValidator.validateQuantity(
                quantity = newQuantity,
                fieldName = quantityType.name.lowercase()
            )

            validationResult.fold(
                onSuccess = { validatedQuantity ->
                    Log.d(TAG, "✅ Validation passed: $validatedQuantity")

                    try {
                        // Step 2: Update in repository
                        // Repository will:
                        // - Update DB with needsSync=1
                        // - Trigger sync callback only after update succeeds
                        val updatedRows = repository.updateQuantity(
                            itemId = itemId,
                            quantity = validatedQuantity,
                            quantityType = quantityType
                        )

                        if (updatedRows > 0) {
                            Log.d(TAG, "✅ DB updated: $updatedRows rows")

                            if (validatedQuantity == 0) {
                                Log.d(TAG, "⚠️ Quantity is now 0 - item will disappear from current view")
                                Log.d(TAG, "📝 Item remains in DB with other quantity intact")
                            }

                            Log.d(TAG, "✅ Sync callback triggered by repository")
                            Log.d(TAG, "============================")
                            Result.success(Unit)
                        } else {
                            Log.e(TAG, "❌ DB update returned 0 rows")
                            Log.d(TAG, "============================")
                            Result.failure(
                                AppError.Database.OperationFailed("updateQuantity")
                            )
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "❌ DB operation failed", e)
                        Log.d(TAG, "============================")
                        Result.failure(
                            if (e is AppError) e
                            else AppError.Database.OperationFailed("updateQuantity", e)
                        )
                    }
                },
                onFailure = { error ->
                    Log.w(TAG, "⚠️ Validation failed: ${error.message}")
                    Log.d(TAG, "============================")
                    Result.failure(error)
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Use case failed", e)
            Log.d(TAG, "============================")
            Result.failure(
                if (e is AppError) e
                else AppError.Unknown("Failed to update quantity: ${e.message}", e)
            )
        }
    }
}