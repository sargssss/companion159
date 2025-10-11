package com.lifelover.companion159.data.repository

import com.lifelover.companion159.data.types.toRoomCategory
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.domain.models.toDomainModel
import com.lifelover.companion159.domain.models.toEntity
import com.lifelover.companion159.data.types.InventoryType
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface LocalInventoryRepository {
    fun getItemsByCategory(category: InventoryType): Flow<List<InventoryItem>>
    suspend fun addItem(item: InventoryItem): Long
    suspend fun updateItem(item: InventoryItem)
    suspend fun deleteItem(id: Long)
}

@Singleton
class LocalInventoryRepositoryImpl @Inject constructor(
    private val dao: InventoryDao,
    private val authService: SupabaseAuthService
) : LocalInventoryRepository {

    override fun getItemsByCategory(category: InventoryType): Flow<List<InventoryItem>> {
        val userId = authService.getUserId() ?: return flowOf(emptyList())

        return dao.getItemsByCategory(category.toRoomCategory(), userId)
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
    }

    override suspend fun addItem(item: InventoryItem): Long {
        val userId = authService.getUserId() ?: return -1

        val entity = item.toEntity().copy(userId = userId)
        return dao.insertItem(entity)
    }

    override suspend fun updateItem(item: InventoryItem) {
        val existingEntity = dao.getItemById(item.id)
        if (existingEntity != null) {
            val updatedRows = dao.updateItem(
                id = item.id,
                name = item.itemName,  // FIXED
                quantity = item.availableQuantity,  // FIXED
                category = item.category,
                crewName = item.crewName  // FIXED
            )
        } else {
            val userId = authService.getUserId() ?: return
            val newEntity = item.toEntity().copy(userId = userId)
            dao.insertItem(newEntity)
        }
    }

    override suspend fun deleteItem(id: Long) {
        dao.softDeleteItem(id)
    }
}