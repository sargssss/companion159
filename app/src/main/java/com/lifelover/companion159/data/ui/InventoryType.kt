package com.lifelover.companion159.data.ui

import com.lifelover.companion159.R
import com.lifelover.companion159.data.room.InventoryCategory
import kotlinx.serialization.Serializable

@Serializable
enum class InventoryType {
    SHIPS,      // Борти/Дрони
    AMMUNITION, // Боєкомплект
    EQUIPMENT,  // Обладнання
    PROVISIONS  // Провізія
}

// Extension functions for UI resources
fun InventoryType.titleRes(): Int = when (this) {
    InventoryType.SHIPS -> R.string.drones
    InventoryType.AMMUNITION -> R.string.ammo
    InventoryType.EQUIPMENT -> R.string.tool
    InventoryType.PROVISIONS -> R.string.food
}

fun InventoryType.iconRes(): Int = when (this) {
    InventoryType.SHIPS -> R.drawable.drone
    InventoryType.AMMUNITION -> R.drawable.bomb
    InventoryType.EQUIPMENT -> R.drawable.tool
    InventoryType.PROVISIONS -> R.drawable.food
}

// Conversion functions between your enum and room enum
fun InventoryType.toRoomCategory(): InventoryCategory = when (this) {
    InventoryType.SHIPS -> InventoryCategory.SHIPS
    InventoryType.AMMUNITION -> InventoryCategory.AMMUNITION
    InventoryType.EQUIPMENT -> InventoryCategory.EQUIPMENT
    InventoryType.PROVISIONS -> InventoryCategory.PROVISIONS
}

fun InventoryCategory.toInventoryType(): InventoryType = when (this) {
    InventoryCategory.SHIPS -> InventoryType.SHIPS
    InventoryCategory.AMMUNITION -> InventoryType.AMMUNITION
    InventoryCategory.EQUIPMENT -> InventoryType.EQUIPMENT
    InventoryCategory.PROVISIONS -> InventoryType.PROVISIONS
}