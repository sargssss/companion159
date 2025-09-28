package com.lifelover.companion159.data.sync

import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.remote.repository.SupabaseInventoryRepository
import com.lifelover.companion159.data.remote.repository.toEntity
import com.lifelover.companion159.data.remote.repository.toSupabaseModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
    OFFLINE
}

@Singleton
class SyncService @Inject constructor(
    private val localDao: InventoryDao,
    private val remoteRepository: SupabaseInventoryRepository,
    private val authService: SupabaseAuthService
) {

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    // Основна функція синхронізації
    suspend fun performSync(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _syncStatus.value = SyncStatus.SYNCING

            // Перевірка автентифікації
            if (authService.getUserId() == null) {
                _syncStatus.value = SyncStatus.ERROR
                return@withContext Result.failure(Exception("Користувач не автентифікований"))
            }

            // 1. Завантажити локальні зміни
            val localItemsNeedingSync = localDao.getItemsNeedingSync()

            // 2. Відправити локальні зміни на сервер
            if (localItemsNeedingSync.isNotEmpty()) {
                val supabaseItems = localItemsNeedingSync.map { it.toSupabaseModel() }
                val syncSuccess = remoteRepository.syncItems(supabaseItems)

                if (!syncSuccess) {
                    _syncStatus.value = SyncStatus.ERROR
                    return@withContext Result.failure(Exception("Помилка синхронізації"))
                }

                // Оновити локальні записи (позначити як синхронізовані)
                localItemsNeedingSync.forEach { item ->
                    val updatedItem = item.copy(
                        needsSync = false,
                        lastSynced = Date()
                    )
                    localDao.updateItem(updatedItem)
                }
            }

            // 3. Завантажити дані з сервера
            val remoteItems = remoteRepository.getAllItems()

            // 4. Оновити локальну базу даними з сервера
            remoteItems.forEach { remoteItem ->
                val existingItem = remoteItem.id?.let {
                    localDao.getItemByServerId(it)
                }

                if (existingItem == null) {
                    // Новий елемент з сервера
                    localDao.insertItem(remoteItem.toEntity())
                } else if (existingItem.needsSync.not()) {
                    // Оновити існуючий елемент, якщо він не має локальних змін
                    val updatedEntity = remoteItem.toEntity().copy(
                        id = existingItem.id
                    )
                    localDao.updateItem(updatedEntity)
                }
            }

            // 5. Видалити локальні елементи, яких немає на сервері
            val allLocalItems = localDao.getAllItems()
            val remoteIds = remoteItems.mapNotNull { it.id }.toSet()

            allLocalItems.forEach { localItem ->
                if (localItem.serverId != null &&
                    localItem.serverId !in remoteIds &&
                    !localItem.needsSync) {
                    localDao.deleteItemPermanently(localItem.id)
                }
            }

            _syncStatus.value = SyncStatus.SUCCESS
            _lastSyncTime.value = System.currentTimeMillis()
            Result.success(Unit)

        } catch (e: Exception) {
            e.printStackTrace()
            _syncStatus.value = SyncStatus.ERROR
            Result.failure(e)
        }
    }

    // Синхронізація одного елемента
    suspend fun syncSingleItem(localId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val item = localDao.getItemById(localId)
                ?: return@withContext Result.failure(Exception("Елемент не знайдено"))

            val supabaseItem = item.toSupabaseModel()

            val result = if (item.serverId == null) {
                // Створити новий елемент на сервері
                remoteRepository.createItem(supabaseItem)
            } else {
                // Оновити існуючий елемент
                val updates = mapOf(
                    "name" to item.name,
                    "quantity" to item.quantity,
                    "category" to item.category.name.lowercase(),
                    "is_deleted" to item.isDeleted
                )
                if (remoteRepository.updateItem(item.serverId, updates)) {
                    supabaseItem.copy(id = item.serverId)
                } else {
                    null
                }
            }

            result?.let { remoteItem ->
                // Оновити локальний елемент з server ID
                val updatedItem = item.copy(
                    serverId = remoteItem.id,
                    needsSync = false,
                    lastSynced = Date()
                )
                localDao.updateItem(updatedItem)
                Result.success(Unit)
            } ?: Result.failure(Exception("Помилка синхронізації елемента"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Перевірка наявності несинхронізованих змін
    suspend fun hasUnsyncedChanges(): Boolean = withContext(Dispatchers.IO) {
        localDao.getItemsNeedingSync().isNotEmpty()
    }

    fun resetSyncStatus() {
        _syncStatus.value = SyncStatus.IDLE
    }
}