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
    val name: String,
    val quantity: Int,
    val category: InventoryCategory,
    val userId: String? = null,
    val supabaseId: String? = null,

    val position: String? = null,
    val createdAt: Date = Date(),
    val lastModified: Date = Date(),
    val lastSynced: Date? = null,
    val needsSync: Boolean = true,
    val isDeleted: Boolean = false
)

enum class InventoryCategory {
    SHIPS, AMMUNITION, EQUIPMENT, PROVISIONS
}

fun InventoryCategory.titleRes(): Int = when (this) {
    InventoryCategory.SHIPS -> com.lifelover.companion159.R.string.drones
    InventoryCategory.AMMUNITION -> com.lifelover.companion159.R.string.ammo
    InventoryCategory.EQUIPMENT -> com.lifelover.companion159.R.string.tool
    InventoryCategory.PROVISIONS -> com.lifelover.companion159.R.string.food
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
    fun toCategory(category: String): InventoryCategory = InventoryCategory.valueOf(category)

    @TypeConverter
    fun fromDate(date: Date?): Long? = date?.time

    @TypeConverter
    fun toDate(timestamp: Long?): Date? = timestamp?.let { Date(it) }
}