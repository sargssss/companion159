package com.lifelover.companion159.domain.models

import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import java.util.Date

data class InventoryItem(
    val id: Long = 0,
    val itemName: String,
    val availableQuantity: Int = 0,
    val neededQuantity: Int = 0,  // NEW
    val category: InventoryCategory,
    val crewName: String,
    val lastModified: Date = Date(),
    val isSynced: Boolean = false
)

// Extension functions for conversion
fun InventoryItemEntity.toDomainModel(): InventoryItem {
    return InventoryItem(
        id = id,
        itemName = itemName,
        availableQuantity = availableQuantity,
        neededQuantity = neededQuantity,  // NEW
        category = category,
        crewName = crewName,
        lastModified = lastModified,
        isSynced = !needsSync
    )
}

fun InventoryItem.toEntity(): InventoryItemEntity {
    return InventoryItemEntity(
        id = id,
        itemName = itemName,
        availableQuantity = availableQuantity,
        neededQuantity = neededQuantity,  // NEW
        category = category,
        crewName = crewName,
        supabaseId = null,
        lastModified = Date(),
        needsSync = true,
        isActive = true
    )
}