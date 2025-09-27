package com.lifelover.companion159.domain.models

import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import java.util.Date

data class InventoryItem(
    val id: Long = 0,
    val name: String,
    val quantity: Int = 1,
    val category: InventoryCategory,
    val lastModified: Date = Date(),
    val isSynced: Boolean = false
)

// Extension functions for conversion
fun InventoryItemEntity.toDomainModel(): InventoryItem {
    return InventoryItem(
        id = id,
        name = name,
        quantity = quantity,
        category = category,
        lastModified = lastModified,
        isSynced = !needsSync
    )
}

fun InventoryItem.toEntity(): InventoryItemEntity {
    return InventoryItemEntity(
        id = if (id == 0L) 0 else id,
        name = name,
        quantity = quantity,
        category = category,
        serverId = null,
        lastModified = lastModified,
        needsSync = true
    )
}