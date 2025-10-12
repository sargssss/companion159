package com.lifelover.companion159.domain.usecases.inventory

import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.InventoryResult
import javax.inject.Inject

/**
 * Delete inventory item (soft delete)
 */
class DeleteItemUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend operator fun invoke(itemId: Long): InventoryResult {
        return try {
            repository.deleteItem(itemId)
            InventoryResult.Success
        } catch (e: Exception) {
            InventoryResult.Error("Помилка видалення: ${e.message}", e)
        }
    }
}