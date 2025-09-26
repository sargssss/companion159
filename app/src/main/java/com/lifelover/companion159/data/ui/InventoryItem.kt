package com.lifelover.companion159.data.ui

import com.lifelover.companion159.data.room.InventoryCategory
import com.lifelover.companion159.data.room.InventoryItemEntity
import java.util.Date

data class InventoryItem(
    val id: Long = 0,
    val name: String,
    val quantity: Int,
    val category: InventoryCategory,
    val lastModified: Date = Date(),
    val isSynced: Boolean = false
)

// Extension functions для конвертації
fun InventoryItemEntity.toDomainModel(): InventoryItem {
    return InventoryItem(
        id = id,
        name = name,
        quantity = quantity,
        category = category,
        lastModified = lastModified,
        isSynced = lastSynced != null && !needsSync
    )
}

fun InventoryItem.toEntity(
    serverId: String? = null,
    needsSync: Boolean = true
): InventoryItemEntity {
    return InventoryItemEntity(
        id = id,
        name = name,
        quantity = quantity,
        category = category,
        serverId = serverId,
        lastModified = Date(),
        needsSync = needsSync
    )
}