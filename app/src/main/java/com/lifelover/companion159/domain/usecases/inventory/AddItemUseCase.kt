package com.lifelover.companion159.domain.usecases.inventory

import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.InventoryResult
import com.lifelover.companion159.domain.models.toStorageCategory
import javax.inject.Inject

/**
 * Add new inventory item
 * Uses extension function to simplify category mapping
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
            // ✅ USE EXTENSION FUNCTION - no duplication!
            val storageCategory = displayCategory.toStorageCategory()

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

        } catch (e: IllegalStateException) {
            InventoryResult.Error("Необхідно увійти в акаунт", e)
        } catch (e: SecurityException) {
            InventoryResult.PermissionError("Недостатньо прав для додавання")
        } catch (e: Exception) {
            InventoryResult.Error("Помилка додавання: ${e.message}", e)
        }
    }
}