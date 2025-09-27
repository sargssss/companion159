package com.lifelover.companion159.domain.usecases

import com.lifelover.companion159.data.repository.InventoryRepository
import javax.inject.Inject

class DeleteInventoryItemUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteItem(id)
    }
}