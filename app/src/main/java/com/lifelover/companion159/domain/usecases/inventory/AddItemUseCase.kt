package com.lifelover.companion159.domain.usecases.inventory

import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.InventoryResult
import com.lifelover.companion159.domain.models.StorageCategory
import javax.inject.Inject

/**
 * Add new inventory item
 * Determines storage category based on display context
 */
class AddItemUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend operator fun invoke(
        name: String,
        availableQuantity: Int,
        neededQuantity: Int,
        displayCategory: DisplayCategory
    ): InventoryResult {
        // Validation
        if (name.isBlank()) {
            return InventoryResult.ValidationError("name", "Назва не може бути порожньою")
        }

        if (availableQuantity < 0 || neededQuantity < 0) {
            return InventoryResult.ValidationError("quantity", "Кількість не може бути від'ємною")
        }

        return try {
            // Determine storage category based on DisplayCategory
            val storageCategory = when (displayCategory) {
                DisplayCategory.AMMUNITION -> StorageCategory.AMMUNITION
                DisplayCategory.AVAILABILITY,
                DisplayCategory.NEEDS -> StorageCategory.EQUIPMENT
            }

            val item = InventoryItem(
                id = 0,
                itemName = name.trim(),
                availableQuantity = availableQuantity,
                neededQuantity = neededQuantity,
                category = storageCategory,
                crewName = "" // Will be set by repository
            )

            repository.addItem(item)
            InventoryResult.Success

        } catch (e: Exception) {
            InventoryResult.Error("Помилка додавання: ${e.message}", e)
        }
    }
}