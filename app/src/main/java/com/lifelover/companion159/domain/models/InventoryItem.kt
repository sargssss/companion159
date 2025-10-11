package com.lifelover.companion159.domain.models

import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import java.util.Date

data class InventoryItem(
    val id: Long = 0,
    val itemName: String,  // FIXED: renamed from 'name'
    val availableQuantity: Int = 1,  // FIXED: renamed from 'quantity'
    val category: InventoryCategory,
    val crewName: String,  // FIXED: added crew name (was position)
    val lastModified: Date = Date(),
    val isSynced: Boolean = false
)

// Extension functions for conversion
fun InventoryItemEntity.toDomainModel(): InventoryItem {
    return InventoryItem(
        id = id,
        itemName = itemName,  // FIXED
        availableQuantity = availableQuantity,  // FIXED
        category = category,
        crewName = crewName,  // FIXED
        lastModified = lastModified,
        isSynced = !needsSync
    )
}

fun InventoryItem.toEntity(): InventoryItemEntity {
    return InventoryItemEntity(
        id = id,
        itemName = itemName,  // FIXED
        availableQuantity = availableQuantity,  // FIXED
        category = category,
        crewName = crewName,  // FIXED
        supabaseId = null,
        lastModified = Date(),
        needsSync = true,
        isActive = true
    )
}