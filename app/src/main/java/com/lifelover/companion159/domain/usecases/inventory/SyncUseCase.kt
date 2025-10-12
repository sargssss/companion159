package com.lifelover.companion159.domain.usecases.inventory

import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.data.repository.SyncResult
import javax.inject.Inject

/**
 * Synchronize inventory with server
 */
class SyncUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    suspend operator fun invoke(): SyncResult {
        return repository.syncWithServer()
    }
}