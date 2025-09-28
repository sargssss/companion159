package com.lifelover.companion159.data.repository

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.sync.AutoSyncManager
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.toDomainModel
import com.lifelover.companion159.domain.models.toEntity
import com.lifelover.companion159.network.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val localDao: InventoryDao,
    private val autoSyncManager: AutoSyncManager,
    private val networkMonitor: NetworkMonitor
) : InventoryRepository {

    companion object {
        private const val TAG = "InventoryRepository"
    }

    // Фоновий scope для синхронізації
    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun getItemsByCategory(category: InventoryCategory): Flow<List<InventoryItem>> {
        return localDao.getItemsByCategory(category)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    // ТІЛЬКИ для створення НОВИХ записів
    override suspend fun addItem(item: InventoryItem) {
        Log.d(TAG, "➕ CREATING new item: ${item.name}")

        val entity = item.toEntity().copy(
            id = 0, // Room згенерує новий ID
            supabaseId = null, // Новий запис - немає Supabase ID
            needsSync = true,
            lastModified = Date(),
            isDeleted = false
        )

        val insertedId = localDao.insertItem(entity)
        Log.d(TAG, "✅ NEW item created with local ID: $insertedId")

        // Синхронізація в фоні - НЕ блокує UI
        triggerBackgroundSync()
    }

    // ТІЛЬКИ для оновлення ІСНУЮЧИХ записів
    override suspend fun updateItem(item: InventoryItem) {
        Log.d(TAG, "📝 UPDATING existing item with ID: ${item.id}, name: ${item.name}")

        // Перевіряємо, чи існує запис
        val existingItem = localDao.getItemById(item.id)
        if (existingItem == null) {
            Log.e(TAG, "❌ Item with ID ${item.id} does NOT exist, cannot update")
            throw IllegalArgumentException("Item with ID ${item.id} does not exist")
        }

        // МИТТЄВО оновлюємо локально (UI відразу бачить зміни)
        val updatedRows = localDao.updateItem(
            id = item.id,
            name = item.name.trim(),
            quantity = item.quantity,
            category = item.category
        )

        if (updatedRows > 0) {
            Log.d(TAG, "✅ EXISTING item updated locally (rows: $updatedRows)")
        } else {
            Log.w(TAG, "⚠️ No rows updated for item ID: ${item.id}")
        }

        // Синхронізація в фоні - НЕ блокує UI
        triggerBackgroundSync()
    }

    // ОПТИМІСТИЧНИЙ метод для оновлення кількості - НЕ блокує UI
    suspend fun updateItemQuantity(itemId: Long, newQuantity: Int) {
        Log.d(TAG, "🔢 OPTIMISTIC quantity update for item ID: $itemId to $newQuantity")

        val existingItem = localDao.getItemById(itemId)
        if (existingItem == null) {
            Log.e(TAG, "❌ Item with ID $itemId does NOT exist")
            return
        }

        // МИТТЄВО оновлюємо локально - UI відразу бачить зміни
        val updatedRows = localDao.updateQuantity(itemId, newQuantity)

        if (updatedRows > 0) {
            Log.d(TAG, "✅ Quantity updated locally, triggering background sync")
        } else {
            Log.w(TAG, "⚠️ No rows updated for item ID: $itemId")
        }

        // Синхронізація в фоні - НЕ чекаємо завершення
        triggerBackgroundSync()
    }

    // ОПТИМІСТИЧНИЙ метод для оновлення імені - НЕ блокує UI
    suspend fun updateItemName(itemId: Long, newName: String) {
        Log.d(TAG, "📝 OPTIMISTIC name update for item ID: $itemId to '$newName'")

        val existingItem = localDao.getItemById(itemId)
        if (existingItem == null) {
            Log.e(TAG, "❌ Item with ID $itemId does NOT exist")
            return
        }

        // МИТТЄВО оновлюємо локально - UI відразу бачить зміни
        val updatedRows = localDao.updateName(itemId, newName.trim())

        if (updatedRows > 0) {
            Log.d(TAG, "✅ Name updated locally, triggering background sync")
        } else {
            Log.w(TAG, "⚠️ No rows updated for item ID: $itemId")
        }

        // Синхронізація в фоні - НЕ чекаємо завершення
        triggerBackgroundSync()
    }

    // ТІЛЬКИ для видалення ІСНУЮЧИХ записів
    override suspend fun deleteItem(id: Long) {
        Log.d(TAG, "🗑️ DELETING existing item with ID: $id")

        // Спочатку отримуємо інформацію про запис для логування
        val existingItem = localDao.getItemById(id)
        if (existingItem == null) {
            Log.w(TAG, "⚠️ Item with ID $id does NOT exist, cannot delete")
            return
        }

        Log.d(TAG, "🗑️ Deleting item: ${existingItem.name}, supabaseId: ${existingItem.supabaseId}")

        // МИТТЄВО видаляємо локально - UI відразу бачить зміни
        val deletedRows = localDao.softDeleteItem(id)

        if (deletedRows > 0) {
            Log.d(TAG, "✅ Item marked as deleted locally, triggering background sync")
        } else {
            Log.w(TAG, "⚠️ No rows updated for item ID: $id")
        }

        // Синхронізація в фоні - НЕ чекаємо завершення
        triggerBackgroundSync()
    }

    override suspend fun syncWithServer(): SyncResult {
        Log.d(TAG, "🔄 Manual sync requested")
        return try {
            autoSyncManager.triggerImmediateSync()
            SyncResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Manual sync failed", e)
            when {
                !networkMonitor.isOnline -> SyncResult.NetworkError
                else -> SyncResult.Error(e.message ?: "Unknown sync error")
            }
        }
    }

    override suspend fun hasUnsyncedChanges(): Boolean {
        val hasChanges = localDao.getItemsNeedingSync().isNotEmpty()
        Log.d(TAG, "📊 Has unsynced changes: $hasChanges")
        return hasChanges
    }

    /**
     * КЛЮЧОВЕ ВИПРАВЛЕННЯ: Фонова синхронізація без блокування UI
     */
    private fun triggerBackgroundSync() {
        Log.d(TAG, "🔄 Triggering background sync (non-blocking)")

        if (networkMonitor.isOnline) {
            // Запускаємо синхронізацію в фоновому scope - НЕ блокує UI
            backgroundScope.launch {
                try {
                    Log.d(TAG, "📡 Starting background sync...")
                    autoSyncManager.triggerImmediateSync()
                    Log.d(TAG, "✅ Background sync completed")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Background sync failed", e)
                    // При помилці синхронізації UI залишається працездатним
                }
            }
        } else {
            Log.d(TAG, "📴 Device is offline, sync will happen when online")
        }
    }
}