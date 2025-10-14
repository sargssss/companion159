package com.lifelover.companion159.domain.models

import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import java.util.Date

/**
 * Domain model for inventory item
 * Contains only business-relevant fields
 */
data class InventoryItem(
    val id: Long = 0,
    val itemName: String,
    val availableQuantity: Int = 0,
    val neededQuantity: Int = 0,
    val category: StorageCategory,
    val priority: String = "medium",
    val crewName: String,
    val lastModified: Date = Date(),
    val isSynced: Boolean = false
)

/**
 * Extension: Convert to Entity
 * Replaces InventoryMapper.toEntity()
 */
fun InventoryItem.toEntity(
    userId: String?,
    supabaseId: Long? = null
) = InventoryItemEntity(
    id = id,
    itemName = itemName,
    availableQuantity = availableQuantity,
    neededQuantity = neededQuantity,
    category = category,
    userId = userId,
    crewName = crewName,
    priority = priority,
    supabaseId = supabaseId,
    createdAt = Date(),
    lastModified = lastModified,
    lastSynced = if (isSynced) Date() else null,
    needsSync = !isSynced,
    isActive = true
)