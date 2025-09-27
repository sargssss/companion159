package com.lifelover.companion159.data.repository


import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.remote.api.InventoryApiService
import com.lifelover.companion159.data.remote.auth.AuthService
import com.lifelover.companion159.data.mappers.*
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.network.NetworkMonitor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val localDao: InventoryDao,
    private val apiService: InventoryApiService,
    private val authService: AuthService,
    private val networkMonitor: NetworkMonitor
) : InventoryRepository {

    // ═══════════════════════════════════════════════════════════════════
    // Основні CRUD операції з offline-first підходом
    // ═══════════════════════════════════════════════════════════════════

    override fun getItemsByCategory(category: InventoryCategory): Flow<List<InventoryItem>> {
        return combine(
            localDao.getItemsByCategory(category.toRoom()),
            networkMonitor.isOnline
        ) { localItems, isOnline ->
            val domainItems = localItems.map { it.toDomain() }

            // Якщо онлайн і є аутентифікація, спробуємо синхронізувати
            if (isOnline && authService.isAuthenticated()) {
                syncCategoryInBackground(category)
            }

            domainItems
        }
    }

    override suspend fun addItem(item: InventoryItem): Long {
        // 1. Завжди зберігаємо локально спочатку
        val localId = localDao.insertItem(item.toEntityForInsert())

        // 2. Спробуємо синхронізувати з сервером
        if (networkMonitor.isOnline && authService.isAuthenticated()) {
            try {
                val serverItem = apiService.createItem(item.toCreateDto())

                // Оновлюємо локальний запис з server ID
                val syncedEntity = item.copy(
                    id = localId,
                    isSynced = true
                ).toEntityForUpdate().copy(
                    serverId = serverItem.id.toString()
                )

                localDao.updateItem(syncedEntity)

            } catch (e: Exception) {
                // Якщо не вдалося синхронізувати, залишаємо як несинхронізований
                android.util.Log.w("Repository", "Failed to sync new item: ${e.message}")
            }
        }

        return localId
    }

    override suspend fun updateItem(item: InventoryItem) {
        // 1. Завжди оновлюємо локально
        localDao.updateItem(item.toEntityForUpdate())

        // 2. Спробуємо синхронізувати з сервером
        if (networkMonitor.isOnline && authService.isAuthenticated()) {
            try {
                apiService.updateItem(item.id, item.toUpdateDto())

                // Позначаємо як синхронізований
                val syncedEntity = item.toEntityForUpdate().copy(needsSync = false)
                localDao.updateItem(syncedEntity)

            } catch (e: Exception) {
                android.util.Log.w("Repository", "Failed to sync updated item: ${e.message}")
            }
        }
    }

    override suspend fun deleteItem(id: Long) {
        // 1. Soft delete локально
        localDao.softDeleteItem(id)

        // 2. Видаляємо з сервера
        if (networkMonitor.isOnline && authService.isAuthenticated()) {
            try {
                apiService.deleteItem(id)
            } catch (e: Exception) {
                android.util.Log.w("Repository", "Failed to delete item on server: ${e.message}")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Синхронізація
    // ═══════════════════════════════════════════════════════════════════

    override suspend fun syncWithServer(): SyncResult {
        if (!networkMonitor.isOnline) {
            return SyncResult.NetworkError
        }

        if (!authService.isAuthenticated()) {
            return SyncResult.Error("User not authenticated")
        }

        return try {
            // 1. Синхронізуємо локальні зміни на сервер
            syncLocalChangesToServer()

            // 2. Отримуємо оновлення з сервера
            syncServerChangesToLocal()

            SyncResult.Success
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Sync failed")
        }
    }

    private suspend fun syncLocalChangesToServer() {
        // Отримуємо всі несинхронізовані записи
        val unsyncedItems = localDao.getItemsNeedingSync()

        for (entity in unsyncedItems) {
            try {
                when {
                    entity.isDeleted -> {
                        // Видаляємо з сервера
                        entity.serverId?.toLongOrNull()?.let { serverId ->
                            apiService.deleteItem(serverId)
                        }
                        // Видаляємо з локальної бази
                        localDao.deleteItemPermanently(entity.id)
                    }

                    entity.serverId == null -> {
                        // Створюємо новий на сервері
                        val serverItem = apiService.createItem(entity.toDomain().toCreateDto())
                        val updatedEntity = entity.copy(
                            serverId = serverItem.id.toString(),
                            needsSync = false
                        )
                        localDao.updateItem(updatedEntity)
                    }

                    else -> {
                        // Оновлюємо існуючий на сервері
                        val serverId = entity.serverId!!.toLong()
                        apiService.updateItem(serverId, entity.toDomain().toUpdateDto())
                        val updatedEntity = entity.copy(needsSync = false)
                        localDao.updateItem(updatedEntity)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Repository", "Failed to sync item ${entity.id}: ${e.message}")
            }
        }
    }

    private suspend fun syncServerChangesToLocal() {
        // Отримуємо всі дані з сервера
        val serverItems = apiService.getAllItems()

        for (serverItem in serverItems) {
            try {
                val serverId = serverItem.id ?: continue
                val existingEntity = localDao.getItemByServerId(serverId.toString())

                if (existingEntity == null) {
                    // Новий item з сервера - додаємо локально
                    val entity = serverItem.toDomain().toEntityForInsert().copy(
                        serverId = serverId.toString(),
                        needsSync = false
                    )
                    localDao.insertItem(entity)
                } else {
                    // Існуючий item - перевіряємо чи потрібно оновити
                    val serverModified = parseSupabaseTimestamp(serverItem.updatedAt ?: "")
                    if (serverModified.after(existingEntity.lastModified)) {
                        // Серверна версія новіша
                        val updatedEntity = serverItem.toDomain().toEntityForUpdate().copy(
                            id = existingEntity.id,
                            serverId = serverId.toString(),
                            needsSync = false
                        )
                        localDao.updateItem(updatedEntity)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Repository", "Failed to process server item: ${e.message}")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Фонова синхронізація категорії
    // ═══════════════════════════════════════════════════════════════════

    private suspend fun syncCategoryInBackground(category: InventoryCategory) {
        try {
            val serverItems = apiService.getItemsByCategory(category.toApiString())

            for (serverItem in serverItems) {
                val serverId = serverItem.id ?: continue
                val existingEntity = localDao.getItemByServerId(serverId.toString())

                if (existingEntity == null) {
                    // Новий item з сервера
                    val entity = serverItem.toDomain().toEntityForInsert().copy(
                        serverId = serverId.toString(),
                        needsSync = false
                    )
                    localDao.insertItem(entity)
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("Repository", "Background sync failed: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Додаткові методи
    // ═══════════════════════════════════════════════════════════════════

    suspend fun searchItems(query: String, category: InventoryCategory? = null): List<InventoryItem> {
        return if (networkMonitor.isOnline && authService.isAuthenticated()) {
            try {
                val serverItems = apiService.searchItems(query, category?.toApiString())
                serverItems.map { it.toDomain() }
            } catch (e: Exception) {
                // Fallback на локальний пошук
                searchItemsLocally(query, category)
            }
        } else {
            searchItemsLocally(query, category)
        }
    }

    private suspend fun searchItemsLocally(query: String, category: InventoryCategory?): List<InventoryItem> {
        val allItems = if (category != null) {
            localDao.getItemsByCategory(category.toRoom()).first()
        } else {
            localDao.getAllItems()
        }

        return allItems
            .filter { it.name.contains(query, ignoreCase = true) }
            .map { it.toDomain() }
    }

    suspend fun getInventoryStats(): List<InventoryStats> {
        return if (networkMonitor.isOnline && authService.isAuthenticated()) {
            try {
                val serverStats = apiService.getInventoryStats()
                serverStats.map { it.toDomain() }
            } catch (e: Exception) {
                getLocalInventoryStats()
            }
        } else {
            getLocalInventoryStats()
        }
    }

    private suspend fun getLocalInventoryStats(): List<InventoryStats> {
        return InventoryCategory.values().map { category ->
            val items = localDao.getItemsByCategory(category.toRoom()).first()
            InventoryStats(
                category = category,
                totalItems = items.size,
                totalQuantity = items.sumOf { it.quantity }
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Утиліти
    // ═══════════════════════════════════════════════════════════════════

    suspend fun hasUnsyncedChanges(): Boolean {
        return localDao.getItemsNeedingSync().isNotEmpty()
    }

    suspend fun clearAllData() {
        localDao.deleteAllItems()
    }

    suspend fun forceFullSync() {
        if (authService.isAuthenticated()) {
            syncWithServer()
        }
    }
}