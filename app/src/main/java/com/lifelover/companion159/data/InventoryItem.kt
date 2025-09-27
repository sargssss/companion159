package com.lifelover.companion159.data

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import com.lifelover.companion159.data.room.InventoryItemEntity
import com.lifelover.companion159.data.ui.InventoryType
import com.lifelover.companion159.data.ui.toInventoryType
import com.lifelover.companion159.data.ui.toRoomCategory
import java.util.Date

data class InventoryItem(
    val id: Long = 0,
    val name: String,
    var quantity: MutableIntState = mutableIntStateOf(1),
    val category: InventoryType, // Use your existing enum
    val lastModified: Date = Date(),
    val isSynced: Boolean = false
)

// Extension functions for conversion between domain model and entity
fun InventoryItemEntity.toDomainModel(): InventoryItem {
    return InventoryItem(
        id = id,
        name = name,
        quantity = mutableIntStateOf(quantity),
        category = category.toInventoryType(),
        lastModified = lastModified,
        isSynced = lastSynced != null && !needsSync
    )
}

fun InventoryItem.toEntity(): InventoryItemEntity {
    return InventoryItemEntity(
        id = id,
        name = name,
        quantity = quantity.value,
        category = category.toRoomCategory(),
        serverId = null,
        lastModified = Date(),
        needsSync = true
    )
}