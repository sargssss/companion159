package com.lifelover.companion159.data.repository

import com.lifelover.companion159.data.SyncPreferences
import com.lifelover.companion159.data.room.InventoryCategory
import com.lifelover.companion159.data.room.InventoryDao
import com.lifelover.companion159.data.ui.InventoryItem
import com.lifelover.companion159.data.ui.toDomainModel
import com.lifelover.companion159.data.ui.toEntity
import com.lifelover.companion159.network.InventoryApiService
import com.lifelover.companion159.network.NetworkMonitor
import com.lifelover.companion159.network.dto.toApiModel
import com.lifelover.companion159.network.dto.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date

class InventoryRepositoryImpl(
    private val dao: InventoryDao,
    private val apiService: InventoryApiService,
    private val networkMonitor: NetworkMonitor,
    private val syncPreferences: SyncPreferences
) : InventoryRepository {

    override fun getItemsByCategory(category: InventoryCategory): Flow<List<InventoryItem>> {
        return dao.getItemsByCategory(category)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    override suspend fun addItem(item: InventoryItem) {
        val entity = item.toEntity(needsSync = true)
        dao.insertItem(entity)

        if (networkMonitor.isOnline) {
            trySync()
        }
    }

    override suspend fun updateItem(item: InventoryItem) {
        val entity = item.toEntity(needsSync = true)
        dao.updateItem(entity)

        if (networkMonitor.isOnline) {
            trySync()
        }
    }

    override suspend fun deleteItem(id: Long) {
        dao.softDeleteItem(id)

        if (networkMonitor.isOnline) {
            trySync()
        }
    }

    override suspend fun syncWithServer(): SyncResult {
        if (!networkMonitor.isOnline) {
            return SyncResult.NetworkError
        }

        return try {
            // 1. Відправити локальні зміни на сервер
            val unsyncedItems = dao.getItemsNeedingSync()
            for (item in unsyncedItems) {
                when {
                    item.isDeleted -> {
                        item.serverId?.let { serverId ->
                            apiService.deleteItem(serverId)
                            dao.markAsSynced(item.id)
                        }
                    }
                    item.serverId == null -> {
                        // Новий item
                        val apiItem = apiService.createItem(item.toApiModel())
                        dao.updateItem(item.copy(
                            serverId = apiItem.id,
                            lastSynced = Date(),
                            needsSync = false
                        ))
                    }
                    else -> {
                        // Оновлення існуючого
                        apiService.updateItem(item.serverId!!, item.toApiModel())
                        dao.markAsSynced(item.id)
                    }
                }
            }

            // 2. Отримати зміни з сервера
            val lastSyncTime = getLastSyncTimestamp()
            val serverResponse = apiService.getUpdates(lastSyncTime)

            // 3. Застосувати серверні зміни
            for (apiItem in serverResponse.items) {
                val existingItem = dao.getItemByServerId(apiItem.id)
                if (existingItem == null) {
                    dao.insertItem(apiItem.toEntity())
                } else {
                    // Conflict resolution: server wins for now
                    dao.updateItem(apiItem.toEntity().copy(id = existingItem.id))
                }
            }

            // 4. Видалити items які видалені на сервері
            for (deletedId in serverResponse.deletedIds) {
                dao.getItemByServerId(deletedId)?.let { item ->
                    dao.softDeleteItem(item.id)
                }
            }

            // 5. Очистити видалені items
            dao.cleanupDeletedItems()

            saveLastSyncTimestamp(serverResponse.timestamp)
            SyncResult.Success

        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Sync failed")
        }
    }

    override suspend fun hasUnsyncedChanges(): Boolean {
        return dao.getItemsNeedingSync().isNotEmpty()
    }

    private suspend fun trySync() {
        // Background sync без блокування UI
        // Тут можна додати WorkManager для надійності
        try {
            syncWithServer()
        } catch (e: Exception) {
            // Логуємо помилку, але не кидаємо її далі
            android.util.Log.e("Repository", "Background sync failed", e)
        }
    }

    private fun getLastSyncTimestamp(): String {
        return syncPreferences.getLastSyncTimestamp()
    }

    private fun saveLastSyncTimestamp(timestamp: String) {
        syncPreferences.saveLastSyncTimestamp(timestamp)
    }
}