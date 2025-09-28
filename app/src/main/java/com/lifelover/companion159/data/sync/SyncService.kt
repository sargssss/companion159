package com.lifelover.companion159.data.sync

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.entities.InventoryCategory
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
            Log.d(TAG, "🔄 Starting sync...")
            _syncStatus.value = SyncStatus.SYNCING

            // Перевірка автентифікації
            val userId = authService.getUserId()
            if (userId == null) {
                Log.w(TAG, "❌ User not authenticated")
                _syncStatus.value = SyncStatus.ERROR
                return@withContext Result.failure(Exception("Користувач не автентифікований"))
            }

            Log.d(TAG, "✅ User authenticated: $userId")

            // ЕТАП 1: PUSH - Відправити локальні зміни на сервер
            val localItemsNeedingSync = localDao.getItemsNeedingSync()
            Log.d(TAG, "📤 Local items needing sync: ${localItemsNeedingSync.size}")

            for (localItem in localItemsNeedingSync) {
                Log.d(TAG, "Processing local item: ${localItem.name}, supabaseId: ${localItem.supabaseId}, isDeleted: ${localItem.isDeleted}")

                when {
                    // Новий запис (немає supabaseId) + не видалений -> СТВОРИТИ на сервері
                    localItem.supabaseId == null && !localItem.isDeleted -> {
                        Log.d(TAG, "➕ CREATING new item on server: ${localItem.name}")
                        val newSupabaseId = remoteRepository.createItem(localItem)
                        if (newSupabaseId != null) {
                            // Зберігаємо отриманий Supabase ID в локальному записі
                            localDao.setSupabaseId(localItem.id, newSupabaseId)
                            Log.d(TAG, "✅ Created and linked: ${localItem.name} -> $newSupabaseId")
                        } else {
                            Log.e(TAG, "❌ Failed to create: ${localItem.name}")
                        }
                    }

                    // Існуючий запис (є supabaseId) + видалений -> ВИДАЛИТИ на сервері
                    localItem.supabaseId != null && localItem.isDeleted -> {
                        Log.d(TAG, "🗑️ DELETING on server: ${localItem.name}")
                        val deleted = remoteRepository.deleteItem(localItem.supabaseId)
                        if (deleted) {
                            localDao.markAsSynced(localItem.id)
                            Log.d(TAG, "✅ Deleted on server: ${localItem.name}")
                        } else {
                            Log.e(TAG, "❌ Failed to delete: ${localItem.name}")
                        }
                    }

                    // Існуючий запис (є supabaseId) + оновлений -> ОНОВИТИ на сервері
                    localItem.supabaseId != null && !localItem.isDeleted -> {
                        Log.d(TAG, "📝 UPDATING on server: ${localItem.name}")
                        val updated = remoteRepository.updateItem(localItem.supabaseId, localItem)
                        if (updated) {
                            localDao.markAsSynced(localItem.id)
                            Log.d(TAG, "✅ Updated on server: ${localItem.name}")
                        } else {
                            Log.e(TAG, "❌ Failed to update: ${localItem.name}")
                        }
                    }

                    // Новий запис який видалений локально -> просто позначити як синхронізований
                    localItem.supabaseId == null && localItem.isDeleted -> {
                        Log.d(TAG, "🚮 Marking deleted new item as synced: ${localItem.name}")
                        localDao.markAsSynced(localItem.id)
                    }
                }
            }

            // ЕТАП 2: PULL - Завантажити дані з сервера та оновити локальну базу
            Log.d(TAG, "📥 Fetching items from server...")
            val remoteItems = remoteRepository.getAllItems()
            Log.d(TAG, "📊 Fetched ${remoteItems.size} items from server")

            // Отримуємо всі локальні записи для порівняння
            val allLocalItems = localDao.getAllItems()
            val localItemsBySupabaseId = allLocalItems
                .filter { it.supabaseId != null }
                .associateBy { it.supabaseId!! }

            Log.d(TAG, "📊 Local items with supabaseId: ${localItemsBySupabaseId.size}")

            // Обробляємо кожен елемент з сервера
            for (remoteItem in remoteItems) {
                if (remoteItem.id == null) {
                    Log.w(TAG, "⚠️ Remote item has no ID, skipping")
                    continue
                }

                val existingLocalItem = localItemsBySupabaseId[remoteItem.id]

                when {
                    // ВИПАДОК 1: Новий елемент з сервера - створюємо локально
                    existingLocalItem == null -> {
                        if (!remoteItem.isDeleted) {
                            Log.d(TAG, "⬇️ CREATING new local item from server: ${remoteItem.name}")
                            val newEntity = remoteItem.toEntity()
                            localDao.insertItem(newEntity)
                            Log.d(TAG, "✅ Created locally: ${remoteItem.name}")
                        } else {
                            Log.d(TAG, "🚫 Skipping deleted item from server: ${remoteItem.name}")
                        }
                    }

                    // ВИПАДОК 2: Існуючий елемент БЕЗ локальних змін - оновлюємо з сервера
                    !existingLocalItem.needsSync -> {
                        if (remoteItem.isDeleted && !existingLocalItem.isDeleted) {
                            Log.d(TAG, "🗑️ MARKING as deleted (from server): ${remoteItem.name}")
                            localDao.softDeleteItem(existingLocalItem.id)
                        } else if (!remoteItem.isDeleted) {
                            // Перевіряємо, чи потрібне оновлення
                            val needsUpdate = existingLocalItem.name != remoteItem.name ||
                                    existingLocalItem.quantity != remoteItem.quantity ||
                                    existingLocalItem.category.name.lowercase() != remoteItem.category.lowercase()

                            if (needsUpdate) {
                                Log.d(TAG, "📝 UPDATING from server: ${remoteItem.name}")
                                localDao.updateFromServer(
                                    supabaseId = remoteItem.id,
                                    name = remoteItem.name,
                                    quantity = remoteItem.quantity,
                                    category = InventoryCategory.valueOf(remoteItem.category.uppercase()),
                                    isDeleted = remoteItem.isDeleted
                                )
                                Log.d(TAG, "✅ Updated from server: ${remoteItem.name}")
                            } else {
                                Log.d(TAG, "📋 No changes needed for: ${remoteItem.name}")
                                // Просто оновлюємо час синхронізації
                                localDao.markAsSynced(existingLocalItem.id)
                            }
                        }
                    }

                    // ВИПАДОК 3: Існуючий елемент З локальними змінами - залишаємо локальні зміни
                    else -> {
                        Log.d(TAG, "⚡ Keeping local changes for: ${existingLocalItem.name} (server: ${remoteItem.name})")
                        // Локальні зміни мають пріоритет - нічого не робимо
                    }
                }
            }

            _syncStatus.value = SyncStatus.SUCCESS
            _lastSyncTime.value = System.currentTimeMillis()
            Log.d(TAG, "✅ Sync completed successfully")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Sync failed", e)
            _syncStatus.value = SyncStatus.ERROR
            Result.failure(e)
        }
    }

    suspend fun hasUnsyncedChanges(): Boolean = withContext(Dispatchers.IO) {
        val count = localDao.getItemsNeedingSync().size
        Log.d(TAG, "📊 Unsynced items count: $count")
        count > 0
    }

    fun resetSyncStatus() {
        _syncStatus.value = SyncStatus.IDLE
    }
}