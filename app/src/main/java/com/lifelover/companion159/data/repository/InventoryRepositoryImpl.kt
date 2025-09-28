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

    override suspend fun addItem(item: InventoryItem) {
        Log.d(TAG, "Adding new item: ${item.name}")

        val entity = item.toEntity().copy(
            supabaseId = null, // Новий запис - немає Supabase ID
            needsSync = true,
            lastModified = Date()
        )

        val insertedId = localDao.insertItem(entity)
        Log.d(TAG, "Item inserted with local ID: $insertedId")

        triggerAutoSyncIfNeeded()
    }

    override suspend fun updateItem(item: InventoryItem) {
        Log.d(TAG, "Updating item: ${item.name} with local ID: ${item.id}")

        // Перевіряємо, чи існує елемент локально
        val existingEntity = localDao.getItemById(item.id)

        if (existingEntity != null) {
            // Створюємо оновлений entity зі збереженням supabaseId
            val updatedEntity = existingEntity.copy(
                name = item.name,
                quantity = item.quantity,
                lastModified = Date(),
                needsSync = true // Позначаємо для синхронізації
                // supabaseId залишається незмінним!
            )

            localDao.updateItem(updatedEntity)
            Log.d(TAG, "Item updated locally: ${item.name}, supabaseId: ${existingEntity.supabaseId}")
        } else {
            Log.e(TAG, "Item with local ID ${item.id} not found")
            throw IllegalArgumentException("Item with ID ${item.id} does not exist")
        }

        triggerAutoSyncIfNeeded()
    }

    // Спеціальний метод для оновлення тільки кількості
    suspend fun updateItemQuantity(itemId: Long, newQuantity: Int) {
        Log.d(TAG, "Updating quantity for local ID: $itemId to $newQuantity")

        val exists = localDao.itemExists(itemId) > 0
        if (!exists) {
            Log.w(TAG, "Item with local ID $itemId does not exist")
            return
        }

        localDao.updateQuantity(itemId, newQuantity)
        Log.d(TAG, "Quantity updated successfully")

        triggerAutoSyncIfNeeded()
    }

    // Спеціальний метод для оновлення імені
    suspend fun updateItemName(itemId: Long, newName: String) {
        Log.d(TAG, "Updating name for local ID: $itemId to $newName")

        val exists = localDao.itemExists(itemId) > 0
        if (!exists) {
            Log.w(TAG, "Item with local ID $itemId does not exist")
            return
        }

        localDao.updateName(itemId, newName.trim())
        Log.d(TAG, "Name updated successfully")

        triggerAutoSyncIfNeeded()
    }

    override suspend fun deleteItem(id: Long) {
        Log.d(TAG, "Deleting item with local ID: $id")

        val existingEntity = localDao.getItemById(id)
        if (existingEntity == null) {
            Log.w(TAG, "Item with local ID $id does not exist, cannot delete")
            return
        }

        Log.d(TAG, "Deleting item: ${existingEntity.name}, supabaseId: ${existingEntity.supabaseId}")

        // М'яке видалення - позначити для синхронізації
        localDao.softDeleteItem(id)
        Log.d(TAG, "Item soft-deleted successfully")

        triggerAutoSyncIfNeeded()
    }

    override suspend fun syncWithServer(): SyncResult {
        Log.d(TAG, "Manual sync requested")
        return try {
            autoSyncManager.triggerImmediateSync()
            SyncResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "Manual sync failed", e)
            when {
                !networkMonitor.isOnline -> SyncResult.NetworkError
                else -> SyncResult.Error(e.message ?: "Unknown sync error")
            }
        }
    }

    override suspend fun hasUnsyncedChanges(): Boolean {
        val hasChanges = localDao.getItemsNeedingSync().isNotEmpty()
        Log.d(TAG, "Has unsynced changes: $hasChanges")
        return hasChanges
    }

    private fun triggerAutoSyncIfNeeded() {
        Log.d(TAG, "Checking if auto-sync is needed. Online: ${networkMonitor.isOnline}")
        if (networkMonitor.isOnline) {
            Log.d(TAG, "Triggering immediate sync")
            autoSyncManager.triggerImmediateSync()
        } else {
            Log.d(TAG, "Device is offline, sync will happen when online")
        }
    }
}