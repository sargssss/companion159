package com.lifelover.companion159.domain.usecases.inventory

import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.domain.models.InventoryItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Get items for specific display category
 */
class GetItemsUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    operator fun invoke(category: DisplayCategory): Flow<List<InventoryItem>> {
        return when (category) {
            DisplayCategory.AVAILABILITY -> repository.getAvailabilityItems()
            DisplayCategory.AMMUNITION -> repository.getAmmunitionItems()
            DisplayCategory.NEEDS -> repository.getNeedsItems()
        }
    }
}