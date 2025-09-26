package com.lifelover.companion159.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface InventoryDao {
    // Basic CRUD operations - реалізуються на етапі 1
    @Query("SELECT * FROM inventory_items WHERE isDeleted = 0 AND category = :category")
    fun getItemsByCategory(category: InventoryCategory): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE id = :id AND isDeleted = 0")
    suspend fun getItemById(id: Long): InventoryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItemEntity): Long

    @Update
    suspend fun updateItem(item: InventoryItemEntity)

    @Query("UPDATE inventory_items SET isDeleted = 1, needsSync = 1, lastModified = :timestamp WHERE id = :id")
    suspend fun softDeleteItem(id: Long, timestamp: Date = Date())

    // Sync-specific operations - додаються на етапі 2
    @Query("SELECT * FROM inventory_items WHERE needsSync = 1")
    suspend fun getItemsNeedingSync(): List<InventoryItemEntity>

    @Query("SELECT * FROM inventory_items WHERE serverId = :serverId")
    suspend fun getItemByServerId(serverId: String): InventoryItemEntity?

    @Query("UPDATE inventory_items SET needsSync = 0, lastSynced = :timestamp WHERE id = :id")
    suspend fun markAsSynced(id: Long, timestamp: Date = Date())

    @Query("DELETE FROM inventory_items WHERE isDeleted = 1 AND lastSynced IS NOT NULL")
    suspend fun cleanupDeletedItems()
}