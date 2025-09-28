package com.lifelover.companion159.data.repository

import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.sync.AutoSyncManager
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.toDomainModel
import com.lifelover.companion159.domain.models.toEntity
import com.lifelover.companion159.network.NetworkMonitor
import kotlinx.coroutines.flow.*
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

        triggerAutoSyncIfNeeded()
    }

    // ТІЛЬКИ для оновлення ІСНУЮЧИХ записів
    override suspend fun updateItem(item: InventoryItem) {
        Log.d(TAG, "📝 UPDATING existing item with ID: ${item.id}, name: ${item.name}")

        // Перевіряємо, чи існує запис
        val exists = localDao.itemExists(item.id) > 0
        if (!exists) {
            Log.e(TAG, "❌ Item with ID ${item.id} does NOT exist, cannot update")
            throw IllegalArgumentException("Item with ID ${item.id} does not exist")
        }

        // КЛЮЧОВЕ: Оновлюємо ІСНУЮЧИЙ запис по ID, НЕ створюємо новий!
        val updatedRows = localDao.updateItem(
            id = item.id,
            name = item.name,
            quantity = item.quantity,
            category = item.category
        )

        Log.d(TAG, "✅ EXISTING item updated successfully (rows: $updatedRows)")
        triggerAutoSyncIfNeeded()
    }

    // ТІЛЬКИ для оновлення кількості ІСНУЮЧОГО запису
    suspend fun updateItemQuantity(itemId: Long, newQuantity: Int) {
        Log.d(TAG, "🔢 UPDATING quantity for existing item ID: $itemId to $newQuantity")

        val exists = localDao.itemExists(itemId) > 0
        if (!exists) {
            Log.e(TAG, "❌ Item with ID $itemId does NOT exist")
            return
        }

        // КЛЮЧОВЕ: Оновлюємо ІСНУЮЧИЙ запис по ID
        val updatedRows = localDao.updateQuantity(itemId, newQuantity)
        Log.d(TAG, "✅ Quantity updated for existing item (rows: $updatedRows)")

        triggerAutoSyncIfNeeded()
    }

    // ТІЛЬКИ для оновлення імені ІСНУЮЧОГО запису
    suspend fun updateItemName(itemId: Long, newName: String) {
        Log.d(TAG, "📝 UPDATING name for existing item ID: $itemId to '$newName'")

        val exists = localDao.itemExists(itemId) > 0
        if (!exists) {
            Log.e(TAG, "❌ Item with ID $itemId does NOT exist")
            return
        }

        // КЛЮЧОВЕ: Оновлюємо ІСНУЮЧИЙ запис по ID
        val updatedRows = localDao.updateName(itemId, newName.trim())
        Log.d(TAG, "✅ Name updated for existing item (rows: $updatedRows)")

        triggerAutoSyncIfNeeded()
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

        // КЛЮЧОВЕ: Оновлюємо isDeleted = 1 для ІСНУЮЧОГО запису по ID
        val deletedRows = localDao.softDeleteItem(id)
        Log.d(TAG, "✅ Item marked as deleted (rows: $deletedRows)")

        triggerAutoSyncIfNeeded()
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

    private fun triggerAutoSyncIfNeeded() {
        Log.d(TAG, "🔄 Checking if auto-sync is needed. Online: ${networkMonitor.isOnline}")
        if (networkMonitor.isOnline) {
            Log.d(TAG, "📡 Triggering immediate sync")
            autoSyncManager.triggerImmediateSync()
        } else {
            Log.d(TAG, "📴 Device is offline, sync will happen when online")
        }
    }
}