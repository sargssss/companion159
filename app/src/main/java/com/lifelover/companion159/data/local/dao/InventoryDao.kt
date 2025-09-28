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

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getItemById(id: Long): InventoryItemEntity?

    @Query("SELECT * FROM inventory_items WHERE supabaseId = :supabaseId")
    suspend fun getItemBySupabaseId(supabaseId: String): InventoryItemEntity?

    // ЄДИНИЙ метод для створення НОВИХ записів (БЕЗ OnConflictStrategy.REPLACE!)
    @Insert
    suspend fun insertItem(item: InventoryItemEntity): Long

    // ВИПРАВЛЕНО: Оновлення ІСНУЮЧОГО запису по ID
    @Query("""
        UPDATE inventory_items 
        SET name = :name, 
            quantity = :quantity, 
            category = :category,
            needsSync = 1, 
            lastModified = :timestamp 
        WHERE id = :id
    """)
    suspend fun updateItem(
        id: Long,
        name: String,
        quantity: Int,
        category: InventoryCategory,
        timestamp: Date = Date()
    ): Int // Повертає кількість оновлених рядків

    // Оновити тільки кількість
    @Query("""
        UPDATE inventory_items 
        SET quantity = :quantity, 
            needsSync = 1, 
            lastModified = :timestamp 
        WHERE id = :id
    """)
    suspend fun updateQuantity(id: Long, quantity: Int, timestamp: Date = Date()): Int

    // Оновити тільки назву
    @Query("""
        UPDATE inventory_items 
        SET name = :name, 
            needsSync = 1, 
            lastModified = :timestamp 
        WHERE id = :id
    """)
    suspend fun updateName(id: Long, name: String, timestamp: Date = Date()): Int

    // М'яке видалення ІСНУЮЧОГО запису
    @Query("""
        UPDATE inventory_items 
        SET isDeleted = 1, 
            needsSync = 1, 
            lastModified = :timestamp 
        WHERE id = :id
    """)
    suspend fun softDeleteItem(id: Long, timestamp: Date = Date()): Int

    // Жорстке видалення
    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteItemPermanently(id: Long): Int

    @Query("SELECT COUNT(*) FROM inventory_items WHERE isDeleted = 0 AND category = :category")
    suspend fun getItemCount(category: InventoryCategory): Int

    // Методи для синхронізації
    @Query("SELECT * FROM inventory_items WHERE needsSync = 1")
    suspend fun getItemsNeedingSync(): List<InventoryItemEntity>

    @Query("SELECT * FROM inventory_items WHERE isDeleted = 0 ORDER BY lastModified DESC")
    suspend fun getAllItems(): List<InventoryItemEntity>

    @Query("DELETE FROM inventory_items")
    suspend fun deleteAllItems()

    // Встановити supabaseId для ІСНУЮЧОГО запису
    @Query("""
        UPDATE inventory_items 
        SET supabaseId = :supabaseId, 
            needsSync = 0, 
            lastSynced = :timestamp 
        WHERE id = :localId
    """)
    suspend fun setSupabaseId(localId: Long, supabaseId: String, timestamp: Date = Date()): Int

    // Позначити ІСНУЮЧИЙ запис як синхронізований
    @Query("""
        UPDATE inventory_items 
        SET needsSync = 0, 
            lastSynced = :timestamp 
        WHERE id = :localId
    """)
    suspend fun markAsSynced(localId: Long, timestamp: Date = Date()): Int

    // Перевірка існування
    @Query("SELECT COUNT(*) FROM inventory_items WHERE id = :id")
    suspend fun itemExists(id: Long): Int

    // Отримати supabaseId
    @Query("SELECT supabaseId FROM inventory_items WHERE id = :localId")
    suspend fun getSupabaseId(localId: Long): String?

    // НОВИЙ метод: Оновити запис з сервера (для sync) по supabaseId
    @Query("""
        UPDATE inventory_items 
        SET name = :name,
            quantity = :quantity,
            category = :category,
            isDeleted = :isDeleted,
            needsSync = 0,
            lastSynced = :timestamp
        WHERE supabaseId = :supabaseId
    """)
    suspend fun updateFromServer(
        supabaseId: String,
        name: String,
        quantity: Int,
        category: InventoryCategory,
        isDeleted: Boolean,
        timestamp: Date = Date()
    ): Int
}