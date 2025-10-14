package com.lifelover.companion159.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Queue for offline sync operations
 *
 * Stores operations when device is offline
 * Operations are processed when connection is restored
 * Max queue size: 999 items
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Operation type (INSERT, UPDATE, DELETE)
    val operationType: String,

    // Local item ID that operation applies to
    val localItemId: Long,

    // Supabase ID (for UPDATE/DELETE operations)
    val supabaseId: Long? = null,

    // Serialized item data (JSON for INSERT/UPDATE)
    val itemData: String? = null,

    // Timestamp when operation was queued
    val queuedAt: Date = Date(),

    // Number of retry attempts
    val retryCount: Int = 0,

    // Last error message (if any)
    val lastError: String? = null,

    // Status (PENDING, PROCESSING, FAILED)
    val status: String = "PENDING"
)

/**
 * Operation types for sync queue
 */
object SyncOperationType {
    const val INSERT = "INSERT"
    const val UPDATE = "UPDATE"
    const val DELETE = "DELETE"
}

/**
 * Queue status values
 */
object SyncQueueStatus {
    const val PENDING = "PENDING"
    const val PROCESSING = "PROCESSING"
    const val FAILED = "FAILED"
}