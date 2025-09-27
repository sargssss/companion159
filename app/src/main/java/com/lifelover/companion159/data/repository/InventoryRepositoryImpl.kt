package com.lifelover.companion159.data.repository

import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.toDomainModel
import com.lifelover.companion159.domain.models.toEntity
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val localDao: InventoryDao
) : InventoryRepository {

    override fun getItemsByCategory(category: InventoryCategory): Flow<List<InventoryItem>> {
        return localDao.getItemsByCategory(category)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    override suspend fun addItem(item: InventoryItem) {
        localDao.insertItem(item.toEntity())
    }

    override suspend fun updateItem(item: InventoryItem) {
        localDao.updateItem(item.toEntity())
    }

    override suspend fun deleteItem(id: Long) {
        localDao.softDeleteItem(id)
    }

    override suspend fun syncWithServer(): SyncResult {
        // Поки що заглушка
        return SyncResult.Success
    }

    override suspend fun hasUnsyncedChanges(): Boolean {
        return false
    }
}