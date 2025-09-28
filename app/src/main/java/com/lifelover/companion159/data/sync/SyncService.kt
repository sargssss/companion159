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
            Log.d(TAG, "🔄 Starting intelligent sync...")
            _syncStatus.value = SyncStatus.SYNCING

            // Перевірка автентифікації
            val userId = authService.getUserId()
            if (userId == null) {
                Log.w(TAG, "❌ User not authenticated")
                _syncStatus.value = SyncStatus.ERROR
                return@withContext Result.failure(Exception("Користувач не автентифікований"))
            }

            Log.d(TAG, "✅ User authenticated: $userId")

            // 1. PUSH: Відправити локальні зміни на сервер
            val localItemsNeedingSync = localDao.getItemsNeedingSync()
            Log.d(TAG, "📤 Items needing sync: ${localItemsNeedingSync.size}")

            for (localItem in localItemsNeedingSync) {
                when {
                    // ВИПАДОК 1: Новий запис (немає supabaseId) -> СТВОРИТИ
                    localItem.supabaseId == null && !localItem.isDeleted -> {
                        Log.d(TAG, "➕ CREATING new item: ${localItem.name}")
                        val newSupabaseId = remoteRepository.createItem(localItem)
                        if (newSupabaseId != null) {
                            // КРИТИЧНО: Зберегти отриманий Supabase ID локально
                            localDao.setSupabaseId(localItem.id, newSupabaseId)
                            Log.d(TAG, "✅ Created and saved Supabase ID: $newSupabaseId for ${localItem.name}")
                        } else {
                            Log.e(TAG, "❌ Failed to create item: ${localItem.name}")
                        }
                    }

                    // ВИПАДОК 2: Існуючий запис (є supabaseId) + видалений -> ВИДАЛИТИ
                    localItem.supabaseId != null && localItem.isDeleted -> {
                        Log.d(TAG, "🗑️ DELETING item: ${localItem.name}, supabaseId: ${localItem.supabaseId}")
                        val deleted = remoteRepository.deleteItem(localItem.supabaseId)
                        if (deleted) {
                            localDao.markAsSynced(localItem.id)
                            Log.d(TAG, "✅ Deleted item: ${localItem.name}")
                        } else {
                            Log.e(TAG, "❌ Failed to delete item: ${localItem.name}")
                        }
                    }

                    // ВИПАДОК 3: Існуючий запис (є supabaseId) + оновлений -> ОНОВИТИ
                    localItem.supabaseId != null && !localItem.isDeleted -> {
                        Log.d(TAG, "📝 UPDATING item: ${localItem.name}, supabaseId: ${localItem.supabaseId}")
                        val updated = remoteRepository.updateItem(localItem.supabaseId, localItem)
                        if (updated) {
                            localDao.markAsSynced(localItem.id)
                            Log.d(TAG, "✅ Updated item: ${localItem.name}")
                        } else {
                            Log.e(TAG, "❌ Failed to update item: ${localItem.name}")
                        }
                    }

                    // ВИПАДОК 4: Новий запис який видалений локально -> просто позначити як синхронізований
                    localItem.supabaseId == null && localItem.isDeleted -> {
                        Log.d(TAG, "🚮 Marking deleted new item as synced: ${localItem.name}")
                        localDao.markAsSynced(localItem.id)
                    }
                }
            }

            // 2. PULL: Завантажити дані з сервера та оновити локальну базу
            Log.d(TAG, "📥 Fetching items from server")
            val remoteItems = remoteRepository.getAllItems()
            Log.d(TAG, "📊 Fetched ${remoteItems.size} items from server")

            // Обробити кожен елемент з сервера
            for (remoteItem in remoteItems) {
                if (remoteItem.id == null) {
                    Log.w(TAG, "⚠️ Remote item has no ID, skipping")
                    continue
                }

                val existingLocalItem = localDao.getItemBySupabaseId(remoteItem.id)

                when {
                    // ВИПАДОК 1: Новий елемент з сервера - СТВОРЮЄМО новий запис
                    existingLocalItem == null -> {
                        if (!remoteItem.isDeleted) {
                            Log.d(TAG, "⬇️ CREATING new local item from server: ${remoteItem.name}")
                            val newEntity = remoteItem.toEntity().copy(
                                id = 0 // Room згенерує новий локальний ID
                            )
                            localDao.insertItem(newEntity)
                            Log.d(TAG, "✅ New item created locally: ${remoteItem.name}")
                        } else {
                            Log.d(TAG, "🚫 Skipping deleted item from server: ${remoteItem.name}")
                        }
                    }

                    // ВИПАДОК 2: Існуючий елемент БЕЗ локальних змін - ОНОВЛЮЄМО по ID
                    !existingLocalItem.needsSync -> {
                        if (remoteItem.isDeleted && !existingLocalItem.isDeleted) {
                            Log.d(TAG, "🗑️ DELETING local item (from server): ${remoteItem.name}")
                            // КЛЮЧОВЕ: Видаляємо ІСНУЮЧИЙ запис по ID
                            localDao.softDeleteItem(existingLocalItem.id)
                            Log.d(TAG, "✅ Local item marked as deleted: ${remoteItem.name}")

                        } else if (!remoteItem.isDeleted) {
                            Log.d(TAG, "📝 UPDATING local item (from server): ${remoteItem.name}")
                            // КЛЮЧОВЕ: Оновлюємо ІСНУЮЧИЙ запис по ID
                            localDao.updateItem(
                                id = existingLocalItem.id, // Використовуємо існуючий локальний ID
                                name = remoteItem.name,
                                quantity = remoteItem.quantity,
                                category = InventoryCategory.valueOf(remoteItem.category.uppercase())
                            )
                            // Позначаємо як синхронізований
                            localDao.markAsSynced(existingLocalItem.id)
                            Log.d(TAG, "✅ Local item updated from server: ${remoteItem.name}")
                        }
                    }

                    // ВИПАДОК 3: Існуючий елемент З локальними змінами - НЕ ЧІПАЄМО
                    else -> {
                        Log.d(TAG, "⚡ Keeping local changes for item: ${existingLocalItem.name}")
                        // Нічого не робимо - локальні зміни мають пріоритет
                    }
                }
            }

            _syncStatus.value = SyncStatus.SUCCESS
            _lastSyncTime.value = System.currentTimeMillis()
            Log.d(TAG, "✅ Intelligent sync completed successfully")
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