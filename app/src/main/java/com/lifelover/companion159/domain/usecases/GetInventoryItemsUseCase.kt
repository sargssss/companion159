package com.lifelover.companion159.domain.usecases

import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.InventoryItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetInventoryItemsUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    operator fun invoke(category: InventoryCategory): Flow<List<InventoryItem>> {
        return repository.getItemsByCategory(category)
    }
}