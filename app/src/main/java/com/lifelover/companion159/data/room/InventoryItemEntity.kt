// Для InventoryItemEntity (якщо ще не створений)
package com.lifelover.companion159.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.util.Date

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey val id: Long = 0,
    val name: String,
    val quantity: Int,
    val category: InventoryCategory,

    // Sync fields
    val serverId: String? = null,
    val lastModified: Date = Date(),
    val lastSynced: Date? = null,
    val needsSync: Boolean = true,
    val isDeleted: Boolean = false
)

enum class InventoryCategory {
    SHIPS, AMMUNITION, EQUIPMENT, PROVISIONS
}

// Type converters
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