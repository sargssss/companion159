package com.lifelover.companion159.data.sync

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.remote.repository.SupabaseInventoryRepository
import com.lifelover.companion159.data.remote.repository.toEntity
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
    companion object {
        private const val TAG = "SyncService"
    }

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    suspend fun performSync(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting intelligent sync...")
            _syncStatus.value = SyncStatus.SYNCING

            // Перевірка автентифікації
            val userId = authService.getUserId()
            if (userId == null) {
                Log.w(TAG, "User not authenticated")
                _syncStatus.value = SyncStatus.ERROR
                return@withContext Result.failure(Exception("Користувач не автентифікований"))
            }

            Log.d(TAG, "User authenticated: $userId")

            // 1. Обробити локальні зміни що потребують синхронізації
            val localItemsNeedingSync = localDao.getItemsNeedingSync()
            Log.d(TAG, "Items needing sync: ${localItemsNeedingSync.size}")

            for (localItem in localItemsNeedingSync) {
                when {
                    // ВИПАДОК 1: Новий запис (немає supabaseId) -> СТВОРИТИ
                    localItem.supabaseId == null && !localItem.isDeleted -> {
                        Log.d(TAG, "CREATING new item: ${localItem.name}")
                        val newSupabaseId = remoteRepository.createItem(localItem)
                        if (newSupabaseId != null) {
                            // Зберегти отриманий Supabase ID локально
                            localDao.setSupabaseId(localItem.id, newSupabaseId)
                            Log.d(TAG, "✓ Created and saved Supabase ID: $newSupabaseId for ${localItem.name}")
                        } else {
                            Log.e(TAG, "✗ Failed to create item: ${localItem.name}")
                        }
                    }

                    // ВИПАДОК 2: Існуючий запис (є supabaseId) + видалений -> ВИДАЛИТИ
                    localItem.supabaseId != null && localItem.isDeleted -> {
                        Log.d(TAG, "DELETING item: ${localItem.name}, supabaseId: ${localItem.supabaseId}")
                        val deleted = remoteRepository.deleteItem(localItem.supabaseId)
                        if (deleted) {
                            localDao.markAsSynced(localItem.id)
                            Log.d(TAG, "✓ Deleted item: ${localItem.name}")
                        } else {
                            Log.e(TAG, "✗ Failed to delete item: ${localItem.name}")
                        }
                    }

                    // ВИПАДОК 3: Існуючий запис (є supabaseId) + оновлений -> ОНОВИТИ
                    localItem.supabaseId != null && !localItem.isDeleted -> {
                        Log.d(TAG, "UPDATING item: ${localItem.name}, supabaseId: ${localItem.supabaseId}")
                        val updated = remoteRepository.updateItem(localItem.supabaseId, localItem)
                        if (updated) {
                            localDao.markAsSynced(localItem.id)
                            Log.d(TAG, "✓ Updated item: ${localItem.name}")
                        } else {
                            Log.e(TAG, "✗ Failed to update item: ${localItem.name}")
                        }
                    }

                    // ВИПАДОК 4: Новий запис який видалений локально -> просто позначити як синхронізований
                    localItem.supabaseId == null && localItem.isDeleted -> {
                        Log.d(TAG, "Marking deleted new item as synced: ${localItem.name}")
                        localDao.markAsSynced(localItem.id)
                    }
                }
            }

            // 2. Завантажити дані з сервера та оновити локальну базу
            Log.d(TAG, "Fetching items from server")
            val remoteItems = remoteRepository.getAllItems()
            Log.d(TAG, "Fetched ${remoteItems.size} items from server")

            // Обробити кожен елемент з сервера
            for (remoteItem in remoteItems) {
                if (remoteItem.id == null) continue

                val existingLocalItem = localDao.getItemBySupabaseId(remoteItem.id)

                when {
                    // Новий елемент з сервера
                    existingLocalItem == null -> {
                        if (!remoteItem.isDeleted) {
                            Log.d(TAG, "Inserting new item from server: ${remoteItem.name}")
                            localDao.insertItem(remoteItem.toEntity())
                        }
                    }

                    // Елемент оновлений на сервері і локальний не має змін
                    !existingLocalItem.needsSync -> {
                        if (remoteItem.isDeleted && !existingLocalItem.isDeleted) {
                            Log.d(TAG, "Marking item as deleted from server: ${remoteItem.name}")
                            localDao.softDeleteItem(existingLocalItem.id)
                        } else if (!remoteItem.isDeleted) {
                            Log.d(TAG, "Updating item from server: ${remoteItem.name}")
                            val updatedEntity = remoteItem.toEntity().copy(
                                id = existingLocalItem.id
                            )
                            localDao.updateItem(updatedEntity)
                        }
                    }

                    // Локальний елемент має зміни - пріоритет локальним змінам
                    else -> {
                        Log.d(TAG, "Keeping local changes for item: ${existingLocalItem.name}")
                    }
                }
            }

            _syncStatus.value = SyncStatus.SUCCESS
            _lastSyncTime.value = System.currentTimeMillis()
            Log.d(TAG, "✓ Intelligent sync completed successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "✗ Sync failed", e)
            _syncStatus.value = SyncStatus.ERROR
            Result.failure(e)
        }
    }

    suspend fun hasUnsyncedChanges(): Boolean = withContext(Dispatchers.IO) {
        val count = localDao.getItemsNeedingSync().size
        Log.d(TAG, "Unsynced items count: $count")
        count > 0
    }

    fun resetSyncStatus() {
        _syncStatus.value = SyncStatus.IDLE
    }
}