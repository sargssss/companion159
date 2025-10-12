package com.lifelover.companion159.domain.models

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
    val crewName: String,
    val lastModified: Date = Date(),
    val isSynced: Boolean = false
)