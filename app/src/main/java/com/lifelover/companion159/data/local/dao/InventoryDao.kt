package com.lifelover.companion159.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.domain.models.StorageCategory
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface InventoryDao {

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

    @Query("""
        SELECT * FROM inventory_items 
        WHERE isActive = 1 
        AND category = 'AMMUNITION'
        AND crewName = :crewName
        AND (userId = :userId OR userId IS NULL) 
        ORDER BY createdAt DESC
    """)
    fun getAmmunitionItems(userId: String?, crewName: String): Flow<List<InventoryItemEntity>>

    @Query("""
        SELECT * FROM inventory_items 
        WHERE isActive = 1 
        AND neededQuantity > 0
        AND crewName = :crewName
        AND (userId = :userId OR userId IS NULL) 
        ORDER BY createdAt DESC
    """)
    fun getNeedsItems(userId: String?, crewName: String): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getItemById(id: Long): InventoryItemEntity?

    @Insert
    suspend fun insertItem(item: InventoryItemEntity): Long

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

    @Query("""
        SELECT * FROM inventory_items 
        WHERE crewName = :crewName
        AND (userId = :userId OR userId IS NULL)
    """)
    suspend fun getAllItems(userId: String?, crewName: String): List<InventoryItemEntity>

    @Query("""
        UPDATE inventory_items 
        SET supabaseId = :supabaseId,
            lastSynced = :timestamp,
            needsSync = 0
        WHERE id = :localId
    """)
    suspend fun updateSupabaseId(
        localId: Long,
        supabaseId: Long,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE inventory_items 
        SET lastSynced = :timestamp,
            needsSync = 0
        WHERE id = :localId
    """)
    suspend fun markAsSynced(
        localId: Long,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("""
    SELECT * FROM inventory_items 
    WHERE needsSync = 1 
    AND crewName = :crewName
    AND (userId = :userId OR userId IS NULL)
""")
    suspend fun getItemsNeedingSync(userId: String?, crewName: String): List<InventoryItemEntity>

    @Query("""
        SELECT * FROM inventory_items 
        WHERE supabaseId = :supabaseId
        AND (userId = :userId OR userId IS NULL)
        LIMIT 1
    """)
    suspend fun getItemBySupabaseId(supabaseId: Long, userId: String?): InventoryItemEntity?

    /**
     * Observe items needing sync in real-time
     * Used by SyncOrchestrator for automatic sync
     */
    @Query("""
        SELECT * FROM inventory_items 
        WHERE needsSync = 1 
        AND crewName = :crewName
        AND (userId = :userId OR userId IS NULL)
    """)
    fun observeItemsNeedingSync(userId: String?, crewName: String): Flow<List<InventoryItemEntity>>
}