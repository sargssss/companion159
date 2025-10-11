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

    // Get items by category (LOCAL filter, not synced to server)
    @Query("""
        SELECT * FROM inventory_items 
        WHERE isActive = 1 
        AND category = :category 
        AND (userId = :userId OR userId IS NULL) 
        ORDER BY createdAt DESC
    """)
    fun getItemsByCategory(
        category: InventoryCategory,
        userId: String?
    ): Flow<List<InventoryItemEntity>>

    // Get offline items (without userId)
    @Query("""
        SELECT * FROM inventory_items 
        WHERE isActive = 1 
        AND category = :category 
        AND userId IS NULL 
        ORDER BY createdAt DESC
    """)
    fun getItemsByCategoryOffline(
        category: InventoryCategory
    ): Flow<List<InventoryItemEntity>>

    // Get all items for sync
    @Query("SELECT * FROM inventory_items WHERE userId = :userId OR userId IS NULL")
    suspend fun getAllItems(userId: String?): List<InventoryItemEntity>

    // Get items needing sync
    @Query("""
        SELECT * FROM inventory_items 
        WHERE needsSync = 1 
        AND userId = :userId 
        AND userId IS NOT NULL
    """)
    suspend fun getItemsNeedingSync(userId: String): List<InventoryItemEntity>

    // Get offline items for migration to user
    @Query("SELECT * FROM inventory_items WHERE userId IS NULL")
    suspend fun getOfflineItems(): List<InventoryItemEntity>

    // Assign userId to offline items
    @Query("UPDATE inventory_items SET userId = :userId, needsSync = 1 WHERE userId IS NULL")
    suspend fun assignUserIdToOfflineItems(userId: String): Int

    // Get item by ID
    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getItemById(id: Long): InventoryItemEntity?

    // Get item by Supabase ID
    @Query("SELECT * FROM inventory_items WHERE supabaseId = :supabaseId")
    suspend fun getItemBySupabaseId(supabaseId: Long): InventoryItemEntity?

    // Insert item
    @Insert
    suspend fun insertItem(item: InventoryItemEntity): Long

    // Update full item
    @Query("""
        UPDATE inventory_items 
        SET itemName = :name,
            availableQuantity = :quantity,
            category = :category,
            crewName = :crewName,
            needsSync = 1,
            lastModified = :timestamp
        WHERE id = :id
    """)
    suspend fun updateItem(
        id: Long,
        name: String,
        quantity: Int,
        category: InventoryCategory,
        crewName: String,
        timestamp: Date = Date()
    ): Int

    // Update quantity only
    @Query("""
        UPDATE inventory_items 
        SET availableQuantity = :quantity,
            needsSync = 1,
            lastModified = :timestamp
        WHERE id = :id
    """)
    suspend fun updateQuantity(
        id: Long,
        quantity: Int,
        timestamp: Date = Date()
    ): Int

    // Soft delete (set isActive = false)
    @Query("""
        UPDATE inventory_items 
        SET isActive = 0,
            needsSync = 1,
            lastModified = :timestamp
        WHERE id = :id
    """)
    suspend fun softDeleteItem(
        id: Long,
        timestamp: Date = Date()
    ): Int

    // Set Supabase ID after creating on server
    @Query("""
        UPDATE inventory_items 
        SET supabaseId = :supabaseId,
            needsSync = 0,
            lastSynced = :timestamp
        WHERE id = :localId
    """)
    suspend fun setSupabaseId(
        localId: Long,
        supabaseId: Long,  // Changed from String to Long
        timestamp: Date = Date()
    ): Int

    // Mark as synced
    @Query("""
        UPDATE inventory_items 
        SET needsSync = 0,
            lastSynced = :timestamp
        WHERE id = :localId
    """)
    suspend fun markAsSynced(
        localId: Long,
        timestamp: Date = Date()
    ): Int

    // Get Supabase ID
    @Query("SELECT supabaseId FROM inventory_items WHERE id = :localId")
    suspend fun getSupabaseId(localId: Long): Long?  // Changed from String? to Long?

    // Update from server (during sync PULL)
    @Query("""
        UPDATE inventory_items 
        SET itemName = :name,
            availableQuantity = :quantity,
            category = :category,
            crewName = :crewName,
            isActive = :isActive,
            userId = :userId,
            needsSync = 0,
            lastSynced = :timestamp
        WHERE supabaseId = :supabaseId
    """)
    suspend fun updateFromServer(
        supabaseId: Long,  // Changed from String to Long
        userId: String,
        name: String,
        quantity: Int,
        category: InventoryCategory,
        crewName: String,
        isActive: Boolean,
        timestamp: Date = Date()
    ): Int
}