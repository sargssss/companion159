package com.lifelover.companion159.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO specifically for synchronization operations
 *
 * Separated from InventoryDao to avoid confusion between:
 * - UI queries (filtered by quantity, isActive, etc.)
 * - Sync queries (need ALL items with needsSync=1)
 *
 * Benefits:
 * - Clear separation of concerns
 * - No code duplication
 * - Easy to understand sync logic
 */
@Dao
interface SyncDao {

    /**
     * Get all items needing sync for specific crew
     *
     * Returns ALL items with needsSync=1, regardless of:
     * - quantity (can be 0)
     * - isActive (can be false for deleted items)
     *
     * Used by SyncOrchestrator for automatic sync
     *
     * @param userId Current user ID (nullable for shared items)
     * @param crewName Crew name to filter by
     * @return List of items needing sync
     */
    @Query("""
        SELECT * FROM inventory_items 
        WHERE needsSync = 1 
        AND crewName = :crewName
        ORDER BY lastModified DESC
    """)
    suspend fun getItemsNeedingSync(userId: String?, crewName: String): List<InventoryItemEntity>

    /**
     * Observe items needing sync in real-time
     *
     * Flow-based version for reactive sync
     * Emits whenever items with needsSync=1 change
     *
     * @param userId Current user ID
     * @param crewName Crew name to filter by
     * @return Flow of items needing sync
     */
    @Query("""
        SELECT * FROM inventory_items 
        WHERE needsSync = 1 
        AND crewName = :crewName
        ORDER BY lastModified DESC
    """)
    fun observeItemsNeedingSync(userId: String?, crewName: String): Flow<List<InventoryItemEntity>>

    /**
     * Get count of items needing sync
     *
     * Useful for UI indicators (e.g., badge showing "5 items pending")
     *
     * @param userId Current user ID
     * @param crewName Crew name to filter by
     * @return Number of items needing sync
     */
    @Query("""
        SELECT COUNT(*) FROM inventory_items 
        WHERE needsSync = 1 
        AND crewName = :crewName
        AND (userId = :userId OR userId IS NULL)
    """)
    suspend fun getPendingSyncCount(userId: String?, crewName: String): Int

    /**
     * Observe pending sync count in real-time
     *
     * Flow-based version for UI indicators
     *
     * @param userId Current user ID
     * @param crewName Crew name to filter by
     * @return Flow of pending sync count
     */
    @Query("""
        SELECT COUNT(*) FROM inventory_items 
        WHERE needsSync = 1 
        AND crewName = :crewName
        AND (userId = :userId OR userId IS NULL)
    """)
    fun observePendingSyncCount(userId: String?, crewName: String): Flow<Int>

    /**
     * Mark item as synced
     *
     * Called after successful upload to Supabase
     * Sets needsSync=0 and updates lastSynced timestamp
     *
     * @param localId Local item ID
     * @param timestamp Sync timestamp (default: current time)
     */
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

    /**
     * Update Supabase ID after successful insert
     *
     * Called when new item is inserted to Supabase
     * Saves server-generated ID and marks as synced
     *
     * @param localId Local item ID
     * @param supabaseId Server-generated ID
     * @param timestamp Sync timestamp (default: current time)
     */
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

    /**
     * Get item by Supabase ID
     *
     * Used during download sync to find existing local item
     * Prevents duplicates by checking supabaseId
     *
     * @param supabaseId Server ID
     * @param userId Current user ID
     * @return Item or null if not found
     */
    @Query("""
        SELECT * FROM inventory_items 
        WHERE supabaseId = :supabaseId
        AND (userId = :userId OR userId IS NULL)
        LIMIT 1
    """)
    suspend fun getItemBySupabaseId(supabaseId: Long, userId: String?): InventoryItemEntity?

    /**
     * Get all items for specific crew
     *
     * Used for full sync operations
     * Returns ALL items (active and inactive)
     *
     * @param userId Current user ID
     * @param crewName Crew name to filter by
     * @return List of all items
     */
    @Query("""
        SELECT * FROM inventory_items 
        WHERE crewName = :crewName
        AND (userId = :userId OR userId IS NULL)
        ORDER BY createdAt DESC
    """)
    suspend fun getAllItemsForSync(userId: String?, crewName: String): List<InventoryItemEntity>

    /**
     * Reset all sync flags
     *
     * DANGEROUS: Sets needsSync=1 for all items
     * Use only for force full sync or troubleshooting
     *
     * @param crewName Crew name to reset
     */
    @Query("""
        UPDATE inventory_items 
        SET needsSync = 1
        WHERE crewName = :crewName
    """)
    suspend fun resetSyncFlags(crewName: String): Int

    /**
     * Get items by sync status
     *
     * Useful for debugging and monitoring
     *
     * @param needsSync Sync status to filter by
     * @param crewName Crew name to filter by
     * @param userId Current user ID
     * @return List of items matching status
     */
    @Query("""
        SELECT * FROM inventory_items 
        WHERE needsSync = :needsSync
        AND crewName = :crewName
        AND (userId = :userId OR userId IS NULL)
        ORDER BY lastModified DESC
    """)
    suspend fun getItemsBySyncStatus(
        needsSync: Boolean,
        crewName: String,
        userId: String?
    ): List<InventoryItemEntity>
}