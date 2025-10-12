package com.lifelover.companion159.domain.usecases

import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.InventoryItem
import javax.inject.Inject

class AddInventoryItemUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend operator fun invoke(item: InventoryItem) {
        return repository.addItem(item)
    }
}