package com.lifelover.companion159.data.local.dao

import androidx.room.*
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.data.local.entities.InventoryCategory
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface InventoryDao {
    // Існуючі методи...
    @Query("SELECT * FROM inventory_items WHERE isDeleted = 0 AND category = :category ORDER BY lastModified DESC")
    fun getItemsByCategory(category: InventoryCategory): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE id = :id AND isDeleted = 0")
    suspend fun getItemById(id: Long): InventoryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItemEntity): Long

    @Update
    suspend fun updateItem(item: InventoryItemEntity)

    @Query("UPDATE inventory_items SET isDeleted = 1, needsSync = 1, lastModified = :timestamp WHERE id = :id")
    suspend fun softDeleteItem(id: Long, timestamp: Date = Date())

    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteItemPermanently(id: Long)

    @Query("SELECT COUNT(*) FROM inventory_items WHERE isDeleted = 0 AND category = :category")
    suspend fun getItemCount(category: InventoryCategory): Int

    // Нові методи для Supabase синхронізації:
    @Query("SELECT * FROM inventory_items WHERE needsSync = 1")
    suspend fun getItemsNeedingSync(): List<InventoryItemEntity>

    @Query("SELECT * FROM inventory_items WHERE serverId = :serverId AND isDeleted = 0")
    suspend fun getItemByServerId(serverId: String): InventoryItemEntity?

    @Query("SELECT * FROM inventory_items WHERE isDeleted = 0 ORDER BY lastModified DESC")
    suspend fun getAllItems(): List<InventoryItemEntity>

    @Query("DELETE FROM inventory_items")
    suspend fun deleteAllItems()

    @Query("UPDATE inventory_items SET serverId = :serverId, needsSync = 0 WHERE id = :localId")
    suspend fun updateServerId(localId: Long, serverId: String)
}