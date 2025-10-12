package com.lifelover.companion159.domain.usecases.inventory

import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.InventoryResult
import javax.inject.Inject

/**
 * Update full item (name and both quantities)
 */
class UpdateItemUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend operator fun invoke(
        itemId: Long,
        newName: String,
        newAvailableQuantity: Int,
        newNeededQuantity: Int
    ): InventoryResult {
        // Validation
        if (newName.isBlank()) {
            return InventoryResult.ValidationError("name", "Назва не може бути порожньою")
        }

        if (newAvailableQuantity < 0 || newNeededQuantity < 0) {
            return InventoryResult.ValidationError("quantity", "Кількість не може бути від'ємною")
        }

        return try {
            // Get existing item to preserve other fields
            val items = repository.getAllItemsOnce()
            val existingItem = items.find { it.id == itemId }
                ?: return InventoryResult.Error("Елемент не знайдено")

            val updatedItem = existingItem.copy(
                itemName = newName.trim(),
                availableQuantity = newAvailableQuantity,
                neededQuantity = newNeededQuantity
            )

            repository.updateItem(updatedItem)
            InventoryResult.Success

        } catch (e: Exception) {
            InventoryResult.Error("Помилка оновлення: ${e.message}", e)
        }
    }
}