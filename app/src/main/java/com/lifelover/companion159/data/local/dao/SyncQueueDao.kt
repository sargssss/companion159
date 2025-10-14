package com.lifelover.companion159.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.lifelover.companion159.data.local.entities.SyncQueueEntity
import com.lifelover.companion159.data.local.entities.SyncQueueStatus

/**
 * DAO for sync queue operations
 *
 * Manages offline operation queue
 * FIFO processing (oldest first)
 */
@Dao
interface SyncQueueDao {

    /**
     * Insert operation into queue
     */
    @Insert
    suspend fun enqueue(operation: SyncQueueEntity): Long

    /**
     * Get all pending operations
     * Ordered by queuedAt (FIFO)
     */
    @Query("""
        SELECT * FROM sync_queue 
        WHERE status = '${SyncQueueStatus.PENDING}'
        ORDER BY queuedAt ASC
        LIMIT 999
    """)
    suspend fun getAllPending(): List<SyncQueueEntity>

    /**
     * Get pending operations for specific item
     * Used to avoid duplicate operations
     */
    @Query("""
        SELECT * FROM sync_queue 
        WHERE localItemId = :localItemId 
        AND status = '${SyncQueueStatus.PENDING}'
        ORDER BY queuedAt ASC
    """)
    suspend fun getPendingForItem(localItemId: Long): List<SyncQueueEntity>

    /**
     * Mark operation as processing
     */
    @Query("""
        UPDATE sync_queue 
        SET status = '${SyncQueueStatus.PROCESSING}'
        WHERE id = :operationId
    """)
    suspend fun markAsProcessing(operationId: Long)

    /**
     * Mark operation as failed and increment retry count
     */
    @Query("""
        UPDATE sync_queue 
        SET status = '${SyncQueueStatus.FAILED}',
            retryCount = retryCount + 1,
            lastError = :error
        WHERE id = :operationId
    """)
    suspend fun markAsFailed(operationId: Long, error: String)

    /**
     * Remove successfully processed operation
     */
    @Query("DELETE FROM sync_queue WHERE id = :operationId")
    suspend fun remove(operationId: Long)

    /**
     * Clear all pending operations
     * Used for reset/cleanup
     */
    @Query("DELETE FROM sync_queue WHERE status = '${SyncQueueStatus.PENDING}'")
    suspend fun clearPending()

    /**
     * Get queue size
     */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = '${SyncQueueStatus.PENDING}'")
    suspend fun getQueueSize(): Int

    /**
     * Remove old failed operations (older than 7 days)
     * Cleanup to prevent queue bloat
     */
    @Query("""
        DELETE FROM sync_queue 
        WHERE status = '${SyncQueueStatus.FAILED}' 
        AND queuedAt < datetime('now', '-7 days')
    """)
    suspend fun cleanupOldFailedOperations()
}