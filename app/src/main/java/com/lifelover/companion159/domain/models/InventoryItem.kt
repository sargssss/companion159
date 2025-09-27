package com.lifelover.companion159.domain.models

import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.data.ui.toInventoryType
import com.lifelover.companion159.data.ui.toRoomCategory
import java.util.Date

data class InventoryItem(
    val id: Long = 0,
    val name: String,
    val quantity: Int = 1,
    val category: InventoryCategory,
    val lastModified: Date = Date(),
    val isSynced: Boolean = false
)

enum class InventoryCategory {
    SHIPS,      // Drones/Ships
    AMMUNITION, // Ammunition
    EQUIPMENT,  // Equipment
    PROVISIONS  // Provisions
}

// Extension functions for UI resources
fun InventoryCategory.titleRes(): Int = when (this) {
    InventoryCategory.SHIPS -> com.lifelover.companion159.R.string.drones
    InventoryCategory.AMMUNITION -> com.lifelover.companion159.R.string.ammo
    InventoryCategory.EQUIPMENT -> com.lifelover.companion159.R.string.tool
    InventoryCategory.PROVISIONS -> com.lifelover.companion159.R.string.food
}

fun InventoryCategory.iconRes(): Int = when (this) {
    InventoryCategory.SHIPS -> com.lifelover.companion159.R.drawable.drone
    InventoryCategory.AMMUNITION -> com.lifelover.companion159.R.drawable.bomb
    InventoryCategory.EQUIPMENT -> com.lifelover.companion159.R.drawable.tool
    InventoryCategory.PROVISIONS -> com.lifelover.companion159.R.drawable.food
}

// Extension functions for conversion between domain model and entity
fun InventoryItemEntity.toDomainModel(): InventoryItem {
    return InventoryItem(
        id = id,
        name = name,
        quantity = quantity,
        category = category.toInventoryType(),
        lastModified = lastModified,
        isSynced = lastSynced != null && !needsSync
    )
}

fun InventoryItem.toEntityForInsert(): InventoryItemEntity {
    return InventoryItemEntity(
        name = name,
        quantity = quantity.value,
        category = category.toRoomCategory(),
        serverId = null,
        lastModified = Date(),
        needsSync = true
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