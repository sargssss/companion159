package com.lifelover.companion159.data.repository

import com.lifelover.companion159.data.ui.toRoomCategory
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.domain.models.toDomainModel
import com.lifelover.companion159.domain.models.toEntity
import com.lifelover.companion159.data.ui.InventoryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface LocalInventoryRepository {
    fun getItemsByCategory(category: InventoryType): Flow<List<InventoryItem>>
    suspend fun addItem(item: InventoryItem): Long
    suspend fun updateItem(item: InventoryItem)
    suspend fun deleteItem(id: Long)
    suspend fun getItemCount(category: InventoryType): Int
}

@Singleton
class LocalInventoryRepositoryImpl @Inject constructor(
    private val dao: InventoryDao
) : LocalInventoryRepository {

    override fun getItemsByCategory(category: InventoryType): Flow<List<InventoryItem>> {
        return dao.getItemsByCategory(category.toRoomCategory())
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
    }

    override suspend fun addItem(item: InventoryItem): Long {
        val entity = item.toEntity()
        return dao.insertItem(entity)
    }

    override suspend fun updateItem(item: InventoryItem) {
        // Отримуємо існуючий entity щоб зберегти supabaseId
        val existingEntity = dao.getItemById(item.id)
        if (existingEntity != null) {
            val updatedEntity = item.toEntity().copy(
                id = item.id,
                supabaseId = existingEntity.supabaseId, // Зберігаємо supabaseId
                lastSynced = existingEntity.lastSynced
            )
            dao.updateItem(
                id = updatedEntity.id,
                name = updatedEntity.name,
                quantity = updatedEntity.quantity,
                category = updatedEntity.category
            )        } else {
            // Якщо не знайдено, створюємо новий
            dao.insertItem(item.toEntity())
        }
    }

    override suspend fun deleteItem(id: Long) {
        dao.softDeleteItem(id)
    }

    override suspend fun getItemCount(category: InventoryType): Int {
        return dao.getItemCount(category.toRoomCategory())
    }
}