package com.lifelover.companion159.data.local.dao


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface InventoryDao {

    // Show items for authenticated user OR items without userId (offline items)
    @Query("SELECT * FROM inventory_items WHERE isDeleted = 0 AND category = :category AND (userId = :userId OR userId IS NULL) ORDER BY lastModified DESC")
    fun getItemsByCategory(category: InventoryCategory, userId: String?): Flow<List<InventoryItemEntity>>

    // Show all items without userId when offline
    @Query("SELECT * FROM inventory_items WHERE isDeleted = 0 AND category = :category AND userId IS NULL ORDER BY lastModified DESC")
    fun getItemsByCategoryOffline(category: InventoryCategory): Flow<List<InventoryItemEntity>>

    // Get items needing sync only if they have userId
    @Query("SELECT * FROM inventory_items WHERE needsSync = 1 AND userId = :userId AND userId IS NOT NULL")
    suspend fun getItemsNeedingSync(userId: String): List<InventoryItemEntity>

    // Get all items for user
    @Query("SELECT * FROM inventory_items WHERE isDeleted = 0 AND (userId = :userId OR userId IS NULL) ORDER BY lastModified DESC")
    suspend fun getAllItems(userId: String?): List<InventoryItemEntity>

    // Get all offline items (without userId)
    @Query("SELECT * FROM inventory_items WHERE userId IS NULL")
    suspend fun getOfflineItems(): List<InventoryItemEntity>

    // Assign userId to offline items
    @Query("UPDATE inventory_items SET userId = :userId, needsSync = 1 WHERE userId IS NULL")
    suspend fun assignUserIdToOfflineItems(userId: String): Int

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getItemById(id: Long): InventoryItemEntity?

    @Query("SELECT * FROM inventory_items WHERE supabaseId = :supabaseId")
    suspend fun getItemBySupabaseId(supabaseId: String): InventoryItemEntity?

    @Insert
    suspend fun insertItem(item: InventoryItemEntity): Long

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
    ): Int

    @Query("""
        UPDATE inventory_items 
        SET quantity = :quantity, 
            needsSync = 1, 
            lastModified = :timestamp 
        WHERE id = :id
    """)
    suspend fun updateQuantity(id: Long, quantity: Int, timestamp: Date = Date()): Int

    @Query("""
        UPDATE inventory_items 
        SET isDeleted = 1, 
            needsSync = 1, 
            lastModified = :timestamp 
        WHERE id = :id
    """)
    suspend fun softDeleteItem(id: Long, timestamp: Date = Date()): Int

    @Query("""
        UPDATE inventory_items 
        SET supabaseId = :supabaseId, 
            needsSync = 0, 
            lastSynced = :timestamp 
        WHERE id = :localId
    """)
    suspend fun setSupabaseId(localId: Long, supabaseId: String, timestamp: Date = Date()): Int

    @Query("""
        UPDATE inventory_items 
        SET needsSync = 0, 
            lastSynced = :timestamp 
        WHERE id = :localId
    """)
    suspend fun markAsSynced(localId: Long, timestamp: Date = Date()): Int

    @Query("SELECT supabaseId FROM inventory_items WHERE id = :localId")
    suspend fun getSupabaseId(localId: Long): String?

    @Query("""
        UPDATE inventory_items 
        SET name = :name,
            quantity = :quantity,
            category = :category,
            isDeleted = :isDeleted,
            userId = :userId,
            needsSync = 0,
            lastSynced = :timestamp
        WHERE supabaseId = :supabaseId
    """)
    suspend fun updateFromServer(
        supabaseId: String,
        userId: String,
        name: String,
        quantity: Int,
        category: InventoryCategory,
        isDeleted: Boolean,
        timestamp: Date = Date()
    ): Int
}