package com.lifelover.companion159.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.lifelover.companion159.data.local.entities.SyncQueueEntity
import com.lifelover.companion159.data.local.entities.SyncQueueStatus

/**
 * DAO for sync queue operations
 *
 * Currently NOT actively used - sync handled by needsSync flag
 * Kept for future offline queue implementation
 */
@Dao
interface SyncQueueDao {

    @Insert
    suspend fun enqueue(operation: SyncQueueEntity): Long

    @Query("""
        SELECT * FROM sync_queue 
        WHERE status = '${SyncQueueStatus.PENDING}'
        ORDER BY queuedAt ASC
    """)
    suspend fun getAllPending(): List<SyncQueueEntity>

    @Query("""
        UPDATE sync_queue 
        SET status = '${SyncQueueStatus.PROCESSING}'
        WHERE id = :operationId
    """)
    suspend fun markAsProcessing(operationId: Long)

    @Query("""
        UPDATE sync_queue 
        SET status = '${SyncQueueStatus.FAILED}',
            retryCount = retryCount + 1,
            lastError = :error
        WHERE id = :operationId
    """)
    suspend fun markAsFailed(operationId: Long, error: String)

    @Query("DELETE FROM sync_queue WHERE id = :operationId")
    suspend fun remove(operationId: Long)

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = '${SyncQueueStatus.PENDING}'")
    suspend fun getQueueSize(): Int

    @Query("""
        DELETE FROM sync_queue 
        WHERE status = '${SyncQueueStatus.FAILED}' 
        AND queuedAt < datetime('now', '-7 days')
    """)
    suspend fun cleanupOldFailedOperations()
}