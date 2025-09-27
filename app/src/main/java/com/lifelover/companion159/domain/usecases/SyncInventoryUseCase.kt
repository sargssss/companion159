package com.lifelover.companion159.domain.usecases

import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.data.repository.SyncResult
import javax.inject.Inject

class SyncInventoryUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend operator fun invoke(): SyncResult {
        return repository.syncWithServer()
    }
}