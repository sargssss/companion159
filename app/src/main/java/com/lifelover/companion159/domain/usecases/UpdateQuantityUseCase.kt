package com.lifelover.companion159.domain.usecases

import android.util.Log
import com.lifelover.companion159.data.remote.sync.SyncQueueManager
import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.AppError
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.domain.validation.InputValidator
import javax.inject.Inject

class UpdateQuantityUseCase @Inject constructor(
    private val repository: InventoryRepository,
    private val syncQueueManager: SyncQueueManager
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

                    // Step 3: Update in repository (pure data operation)
                    Log.d(TAG, "Updating in repository...")
                    repository.updateQuantity(
                        itemId = itemId,
                        quantity = validatedQuantity,
                        quantityType = quantityType
                    )
                    Log.d(TAG, "✅ Quantity updated in database")

                    // Step 4: Get supabaseId for sync
                    val supabaseId = repository.getSupabaseId(itemId)

                    // Step 5: Enqueue for sync (business logic)
                    Log.d(TAG, "Enqueueing for sync...")
                    syncQueueManager.enqueueUpdate(
                        localItemId = itemId,
                        supabaseId = supabaseId
                    )
                    Log.d(TAG, "✅ Item enqueued for sync")

                    Log.d(TAG, "✅ Quantity update completed successfully")
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