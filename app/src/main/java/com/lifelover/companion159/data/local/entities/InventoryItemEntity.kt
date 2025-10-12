package com.lifelover.companion159.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.lifelover.companion159.R
import java.util.Date

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Basic item info
    val itemName: String,
    val availableQuantity: Int = 0,
    val neededQuantity: Int = 0,

    // Category stored LOCALLY only
    val category: InventoryCategory,

    // User/Position context
    val userId: String? = null,
    val crewName: String,

    // Server sync fields
    val supabaseId: Long? = null,

    // Timestamps
    val createdAt: Date = Date(),
    val lastModified: Date = Date(),
    val lastSynced: Date? = null,

    // Sync status
    val needsSync: Boolean = true,
    val isActive: Boolean = true
)
// Keep existing enum for LOCAL use
enum class InventoryCategory {
    SHIPS, AMMUNITION, EQUIPMENT, PROVISIONS
}

// Keep existing helper functions
fun InventoryCategory.titleRes(): Int = when (this) {
    InventoryCategory.SHIPS -> R.string.drones
    InventoryCategory.AMMUNITION -> R.string.ammo
    InventoryCategory.EQUIPMENT -> R.string.tool
    InventoryCategory.PROVISIONS -> R.string.food
}

fun InventoryCategory.iconRes(): Int = when (this) {
    InventoryCategory.SHIPS -> R.drawable.drone
    InventoryCategory.AMMUNITION -> R.drawable.bomb
    InventoryCategory.EQUIPMENT -> R.drawable.tool
    InventoryCategory.PROVISIONS -> R.drawable.food
}

class Converters {
    @TypeConverter
    fun fromCategory(category: InventoryCategory): String = category.name

    @TypeConverter
    fun toCategory(category: String): InventoryCategory =
        InventoryCategory.valueOf(category)

    @TypeConverter
    fun fromDate(date: Date?): Long? = date?.time

    @TypeConverter
    fun toDate(timestamp: Long?): Date? = timestamp?.let { Date(it) }
}