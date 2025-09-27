package com.lifelover.companion159.data.repository

import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.data.local.entities.InventoryCategory
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    fun getItemsByCategory(category: InventoryCategory): Flow<List<InventoryItem>>
    suspend fun addItem(item: InventoryItem)
    suspend fun updateItem(item: InventoryItem)
    suspend fun deleteItem(id: Long)
    suspend fun syncWithServer(): SyncResult
    suspend fun hasUnsyncedChanges(): Boolean
}

sealed class SyncResult {
    object Success : SyncResult()
    data class Error(val message: String) : SyncResult()
    object NetworkError : SyncResult()
}