package com.lifelover.companion159.domain.usecases

import android.util.Log
import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.QuantityType
import javax.inject.Inject

class DeleteItemUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    companion object {
        private const val TAG = "DeleteItemUseCase"
    }

    suspend operator fun invoke(
        item: InventoryItem,
        displayCategory: DisplayCategory
    ): Result<Unit> {
        return try {
            val quantityType = displayCategory.quantityType

            Log.d(TAG, "Deleting from ${displayCategory.name}: ${item.itemName}")
            Log.d(TAG, "  Before: available=${item.availableQuantity}, needed=${item.neededQuantity}")

            val shouldHardDelete = when (quantityType) {
                QuantityType.AVAILABLE -> item.neededQuantity == 0
                QuantityType.NEEDED -> item.availableQuantity == 0
            }

            if (shouldHardDelete) {
                Log.d(TAG, "  Both quantities will be 0 -> hard delete")
                repository.deleteItem(item.id)
            } else {
                Log.d(TAG, "  Setting ${quantityType.name} quantity to 0")
                repository.updateSingleQuantity(item.id, 0, quantityType)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Delete failed", e)
            Result.failure(e)
        }
    }
}