package com.lifelover.companion159.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.StorageCategory
import java.util.Date

/**
 * Room entity for inventory items
 * Simplified: removed sync fields, using StorageCategory
 */
@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Basic item info
    val itemName: String,
    val availableQuantity: Int = 0,
    val neededQuantity: Int = 0,

    // Category - using domain StorageCategory directly
    val category: StorageCategory,

    val priority: String = "medium", //low|medium|high|urgent

    // User/Position context
    val userId: String? = null,
    val crewName: String,

    // Server sync fields (kept for future use)
    val supabaseId: Long? = null,

    // Timestamps
    val createdAt: Date = Date(),
    val lastModified: Date = Date(),
    val lastSynced: Date? = null,

    // Sync status (kept for future use)
    val needsSync: Boolean = true,
    val isActive: Boolean = true
)

/**
 * Extension: Convert Entity to Domain model
 * Replaces InventoryMapper.toDomain()
 */
fun InventoryItemEntity.toDomain() = InventoryItem(
    id = id,
    itemName = itemName,
    availableQuantity = availableQuantity,
    neededQuantity = neededQuantity,
    category = category,
    crewName = crewName,
    priority = priority,
    lastModified = lastModified,
    isSynced = !needsSync
)

/**
 * Type converters for Room
 */
class Converters {
    @TypeConverter
    fun fromCategory(category: StorageCategory): String = category.name

    @TypeConverter
    fun toCategory(category: String): StorageCategory =
        StorageCategory.valueOf(category)

    @TypeConverter
    fun fromDate(date: Date?): Long? = date?.time

    @TypeConverter
    fun toDate(timestamp: Long?): Date? = timestamp?.let { Date(it) }
}