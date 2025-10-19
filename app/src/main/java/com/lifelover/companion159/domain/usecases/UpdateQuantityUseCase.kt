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
        quantityType: QuantityType
    ): Result<Unit> {
        return try {
            // Validate quantity
            val validationResult = InputValidator.validateQuantity(
                quantity = newQuantity,
                fieldName = quantityType.name.lowercase()
            )

            validationResult.fold(
                onSuccess = { validatedQuantity ->

                    // Update in repository
                    repository.updateQuantity(
                        itemId = itemId,
                        quantity = validatedQuantity,
                        quantityType = quantityType
                    )

                    if (validatedQuantity == 0) {
                        Log.d(TAG, "⚠️ Quantity is now 0 - item will disappear from current view")
                    }
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update quantity", e)
            Result.failure(
                if (e is AppError) e
                else AppError.Unknown("Failed to update quantity: ${e.message}", e)
            )
        }
    }
}