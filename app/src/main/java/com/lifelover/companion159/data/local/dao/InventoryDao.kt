package com.lifelover.companion159.data.local.dao

import androidx.room.*
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.data.local.entities.InventoryCategory
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface InventoryDao {

    @Query("SELECT * FROM inventory_items WHERE isDeleted = 0 AND category = :category ORDER BY lastModified DESC")
    fun getItemsByCategory(category: InventoryCategory): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE id = :id AND isDeleted = 0")
    suspend fun getItemById(id: Long): InventoryItemEntity?

    @Query("SELECT * FROM inventory_items WHERE supabaseId = :supabaseId AND isDeleted = 0")
    suspend fun getItemBySupabaseId(supabaseId: String): InventoryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItemEntity): Long

    @Update
    suspend fun updateItem(item: InventoryItemEntity)

    // Оновити тільки кількість
    @Query("UPDATE inventory_items SET quantity = :quantity, needsSync = 1, lastModified = :timestamp WHERE id = :id")
    suspend fun updateQuantity(id: Long, quantity: Int, timestamp: Date = Date())

    // Оновити тільки назву
    @Query("UPDATE inventory_items SET name = :name, needsSync = 1, lastModified = :timestamp WHERE id = :id")
    suspend fun updateName(id: Long, name: String, timestamp: Date = Date())

    // М'яке видалення
    @Query("UPDATE inventory_items SET isDeleted = 1, needsSync = 1, lastModified = :timestamp WHERE id = :id")
    suspend fun softDeleteItem(id: Long, timestamp: Date = Date())

    // Жорстке видалення
    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteItemPermanently(id: Long)

    @Query("SELECT COUNT(*) FROM inventory_items WHERE isDeleted = 0 AND category = :category")
    suspend fun getItemCount(category: InventoryCategory): Int

    // Методи для синхронізації
    @Query("SELECT * FROM inventory_items WHERE needsSync = 1")
    suspend fun getItemsNeedingSync(): List<InventoryItemEntity>

    @Query("SELECT * FROM inventory_items WHERE isDeleted = 0 ORDER BY lastModified DESC")
    suspend fun getAllItems(): List<InventoryItemEntity>

    @Query("DELETE FROM inventory_items")
    suspend fun deleteAllItems()

    // КЛЮЧОВИЙ МЕТОД: оновити supabaseId після створення на сервері
    @Query("UPDATE inventory_items SET supabaseId = :supabaseId, needsSync = 0, lastSynced = :timestamp WHERE id = :localId")
    suspend fun setSupabaseId(localId: Long, supabaseId: String, timestamp: Date = Date())

    // Позначити як синхронізований
    @Query("UPDATE inventory_items SET needsSync = 0, lastSynced = :timestamp WHERE id = :localId")
    suspend fun markAsSynced(localId: Long, timestamp: Date = Date())

    // Перевірка існування по ID
    @Query("SELECT COUNT(*) FROM inventory_items WHERE id = :id")
    suspend fun itemExists(id: Long): Int

    // Перевірка чи є supabaseId
    @Query("SELECT supabaseId FROM inventory_items WHERE id = :localId")
    suspend fun getSupabaseId(localId: Long): String?
}