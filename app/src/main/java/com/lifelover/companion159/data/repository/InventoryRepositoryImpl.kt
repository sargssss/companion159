package com.lifelover.companion159.data.repository

import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.sync.AutoSyncManager
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.toDomainModel
import com.lifelover.companion159.domain.models.toEntity
import com.lifelover.companion159.network.NetworkMonitor
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val localDao: InventoryDao,
    private val autoSyncManager: AutoSyncManager,
    private val networkMonitor: NetworkMonitor
) : InventoryRepository {

    override fun getItemsByCategory(category: InventoryCategory): Flow<List<InventoryItem>> {
        return localDao.getItemsByCategory(category)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    override suspend fun addItem(item: InventoryItem) {
        // Insert item locally first
        val insertedId = localDao.insertItem(item.toEntity())

        // Trigger auto-sync if online
        triggerAutoSyncIfNeeded()
    }

    override suspend fun updateItem(item: InventoryItem) {
        // Update item locally first
        localDao.updateItem(item.toEntity())

        // Trigger auto-sync if online
        triggerAutoSyncIfNeeded()
    }

    override suspend fun deleteItem(id: Long) {
        // Soft delete locally first
        localDao.softDeleteItem(id)

        // Trigger auto-sync if online
        triggerAutoSyncIfNeeded()
    }

    override suspend fun syncWithServer(): SyncResult {
        // This method is still available for manual sync
        return try {
            autoSyncManager.triggerImmediateSync()
            SyncResult.Success
        } catch (e: Exception) {
            when {
                !networkMonitor.isOnline -> SyncResult.NetworkError
                else -> SyncResult.Error(e.message ?: "Unknown sync error")
            }
        }
    }

    override suspend fun hasUnsyncedChanges(): Boolean {
        return localDao.getItemsNeedingSync().isNotEmpty()
    }

    /**
     * Trigger auto-sync only if we're online and authenticated
     * This ensures immediate sync when user makes changes
     */
    private fun triggerAutoSyncIfNeeded() {
        if (networkMonitor.isOnline) {
            autoSyncManager.triggerImmediateSync()
        }
        // If offline, AutoSyncManager will automatically sync when network returns
    }
}