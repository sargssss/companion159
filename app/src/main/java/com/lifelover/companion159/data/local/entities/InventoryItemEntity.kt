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
    val itemName: String,  // Renamed from 'name'
    val availableQuantity: Int = 0,  // Renamed from 'quantity'

    // Category stored LOCALLY only (not synced to server)
    val category: InventoryCategory,  // KEEP for local filtering

    // User/Position context
    val userId: String? = null,  // Keep for local auth check
    val crewName: String,  // Renamed from 'position', REQUIRED

    // Server sync fields
    val supabaseId: Long? = null,  // Changed from String to Long!

    // Timestamps
    val createdAt: Date = Date(),
    val lastModified: Date = Date(),
    val lastSynced: Date? = null,

    // Sync status
    val needsSync: Boolean = true,
    val isActive: Boolean = true  // Renamed from 'isDeleted' with inverted logic
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