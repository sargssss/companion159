package com.lifelover.companion159.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.domain.models.StorageCategory
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * DAO for inventory operations
 * Simplified: removed sync methods, using StorageCategory
 */
@Dao
interface InventoryDao {

    // ============================================================
    // QUERY METHODS (for display filtering)
    // ============================================================

    /**
     * Get items for AVAILABILITY display
     * Shows non-ammunition with availableQuantity > 0
     */
    @Query("""
        SELECT * FROM inventory_items 
        WHERE isActive = 1 
        AND category != 'AMMUNITION'
        AND availableQuantity > 0
        AND (userId = :userId OR userId IS NULL) 
        ORDER BY createdAt DESC
    """)
    fun getAvailabilityItems(userId: String?): Flow<List<InventoryItemEntity>>

    /**
     * Get items for AMMUNITION display
     * Shows ammunition only (all quantities)
     */
    @Query("""
        SELECT * FROM inventory_items 
        WHERE isActive = 1 
        AND category = 'AMMUNITION'
        AND (userId = :userId OR userId IS NULL) 
        ORDER BY createdAt DESC
    """)
    fun getAmmunitionItems(userId: String?): Flow<List<InventoryItemEntity>>

    /**
     * Get items for NEEDS display
     * Shows ALL items with neededQuantity > 0
     */
    @Query("""
        SELECT * FROM inventory_items 
        WHERE isActive = 1 
        AND neededQuantity > 0
        AND (userId = :userId OR userId IS NULL) 
        ORDER BY createdAt DESC
    """)
    fun getNeedsItems(userId: String?): Flow<List<InventoryItemEntity>>

    /**
     * Get all items for user (for export/analysis)
     */
    @Query("SELECT * FROM inventory_items WHERE userId = :userId OR userId IS NULL")
    suspend fun getAllItems(userId: String?): List<InventoryItemEntity>

    // ============================================================
    // SINGLE ITEM QUERIES
    // ============================================================

    /**
     * Get item by local ID
     */
    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getItemById(id: Long): InventoryItemEntity?

    // ============================================================
    // CREATE / UPDATE / DELETE
    // ============================================================

    /**
     * Insert new item
     */
    @Insert
    suspend fun insertItem(item: InventoryItemEntity): Long

    /**
     * Update full item with both quantities
     */
    @Query("""
        UPDATE inventory_items 
        SET itemName = :name,
            availableQuantity = :availableQuantity,
            neededQuantity = :neededQuantity,
            category = :category,
            crewName = :crewName,
            needsSync = 1,
            lastModified = :timestamp
        WHERE id = :id
    """)
    suspend fun updateItemWithNeeds(
        id: Long,
        name: String,
        availableQuantity: Int,
        neededQuantity: Int,
        category: StorageCategory,
        crewName: String,
        timestamp: Date = Date()
    ): Int

    /**
     * Update available quantity only
     */
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

    /**
     * Update needed quantity only
     */
    @Query("""
        UPDATE inventory_items 
        SET neededQuantity = :quantity,
            needsSync = 1,
            lastModified = :timestamp
        WHERE id = :id
    """)
    suspend fun updateNeededQuantity(
        id: Long,
        quantity: Int,
        timestamp: Date = Date()
    ): Int

    /**
     * Soft delete (set isActive = false)
     */
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
}