package com.lifelover.companion159.data.types

import com.lifelover.companion159.data.local.entities.InventoryCategory
import kotlinx.serialization.Serializable

@Serializable
enum class InventoryType {
    SHIPS,
    AMMUNITION,
    EQUIPMENT,
    PROVISIONS
}


// Conversion functions between your enum and room enum
fun InventoryType.toRoomCategory(): InventoryCategory = when (this) {
    InventoryType.SHIPS -> InventoryCategory.SHIPS
    InventoryType.AMMUNITION -> InventoryCategory.AMMUNITION
    InventoryType.EQUIPMENT -> InventoryCategory.EQUIPMENT
    InventoryType.PROVISIONS -> InventoryCategory.PROVISIONS
}
