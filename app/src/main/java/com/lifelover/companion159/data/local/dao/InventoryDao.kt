package com.lifelover.companion159.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.domain.models.StorageCategory
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * DAO for inventory item operations
 *
 * Focus: UI queries and basic CRUD
 * Sync-specific queries moved to SyncDao
 */
@Dao
interface InventoryDao {

    // ========================================
    // UI QUERIES - Filtered for display
    // ========================================

    /**
     * Get availability items (non-ammunition with availableQuantity > 0)
     */
    @Query("""
        SELECT * FROM inventory_items 
        WHERE isActive = 1 
        AND category != 'AMMUNITION'
        AND availableQuantity > 0
        AND crewName = :crewName
        AND (userId = :userId OR userId IS NULL) 
        ORDER BY createdAt DESC
    """)
    fun getAvailabilityItems(userId: String?, crewName: String): Flow<List<InventoryItemEntity>>

    /**
     * Get ammunition items (with availableQuantity > 0)
     */
    @Query("""
        SELECT * FROM inventory_items 
        WHERE isActive = 1 
        AND category = 'AMMUNITION'
        AND crewName = :crewName
        AND (userId = :userId OR userId IS NULL) 
        ORDER BY createdAt DESC
    """)
    fun getAmmunitionItems(userId: String?, crewName: String): Flow<List<InventoryItemEntity>>

    /**
     * Get needs items (with neededQuantity > 0)
     */
    @Query("""
        SELECT * FROM inventory_items 
        WHERE isActive = 1 
        AND neededQuantity > 0
        AND crewName = :crewName
        AND (userId = :userId OR userId IS NULL) 
        ORDER BY createdAt DESC
    """)
    fun getNeedsItems(userId: String?, crewName: String): Flow<List<InventoryItemEntity>>

    // ========================================
    // BASIC CRUD OPERATIONS
    // ========================================

    /**
     * Get single item by ID
     */
    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getItemById(id: Long): InventoryItemEntity?

    /**
     * Insert new item
     */
    @Insert
    suspend fun insertItem(item: InventoryItemEntity): Long

    /**
     * Update item with all fields including needs
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
     * Update only available quantity
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
     * Update only needed quantity
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
     * Soft delete - mark as inactive
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

    /**
     * Get all items (for debugging)
     */
    @Query("""
        SELECT * FROM inventory_items 
        WHERE crewName = :crewName
        AND (userId = :userId OR userId IS NULL)
    """)
    suspend fun getAllItems(userId: String?, crewName: String): List<InventoryItemEntity>
}