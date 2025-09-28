package com.lifelover.companion159.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.util.Date

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // Локальний Room ID
    val name: String,
    val quantity: Int,
    val category: InventoryCategory,

    // Sync fields для Supabase - КЛЮЧОВЕ ПОЛЕ!
    val supabaseId: String? = null,      // ID з Supabase (UUID)
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
    InventoryCategory.SHIPS -> com.lifelover.companion159.R.drawable.drone
    InventoryCategory.AMMUNITION -> com.lifelover.companion159.R.drawable.bomb
    InventoryCategory.EQUIPMENT -> com.lifelover.companion159.R.drawable.tool
    InventoryCategory.PROVISIONS -> com.lifelover.companion159.R.drawable.food
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