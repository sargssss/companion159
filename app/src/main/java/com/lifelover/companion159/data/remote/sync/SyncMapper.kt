package com.lifelover.companion159.data.remote.sync

import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.data.remote.dto.SupabaseInventoryItemDto
import com.lifelover.companion159.domain.models.StorageCategory
import java.text.SimpleDateFormat
import java.util.*

/**
 * Mapper between Room entities and Supabase DTOs
 */
object SyncMapper {

    private val iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Convert Room Entity to Supabase DTO
     * Used for uploading local data to server
     */
    fun entityToDto(entity: InventoryItemEntity): SupabaseInventoryItemDto {
        return SupabaseInventoryItemDto(
            id = entity.supabaseId,
            tenantId = 9999, // ✅ Fixed value required by Supabase
            crewName = entity.crewName,
            itemName = entity.itemName,
            availableQuantity = entity.availableQuantity,
            neededQuantity = entity.neededQuantity,
            itemCategory = mapCategoryToSupabase(entity.category),
            unit = "шт.",
            priority = entity.priority,
            isActive = entity.isActive,
            createdAt = formatDate(entity.createdAt),
            updatedAt = formatDate(entity.lastModified)
        )
    }

    /**
     * Convert Supabase DTO to Room Entity
     */
    fun dtoToEntity(
        dto: SupabaseInventoryItemDto,
        userId: String?,
        markAsSynced: Boolean = true
    ): InventoryItemEntity {
        return InventoryItemEntity(
            id = 0,
            supabaseId = dto.id,
            userId = userId,
            crewName = dto.crewName,
            itemName = dto.itemName,
            availableQuantity = dto.availableQuantity,
            neededQuantity = dto.neededQuantity,
            category = mapCategoryFromSupabase(dto.itemCategory),
            priority = dto.priority,
            isActive = dto.isActive,
            createdAt = parseDate(dto.createdAt) ?: Date(),
            lastModified = parseDate(dto.updatedAt) ?: Date(),
            lastSynced = if (markAsSynced) Date() else null,
            needsSync = !markAsSynced
        )
    }

    /**
     * Update existing Room entity with Supabase DTO data
     */
    fun updateEntityFromDto(
        existingEntity: InventoryItemEntity,
        dto: SupabaseInventoryItemDto
    ): InventoryItemEntity {
        return existingEntity.copy(
            supabaseId = dto.id,
            itemName = dto.itemName,
            availableQuantity = dto.availableQuantity,
            neededQuantity = dto.neededQuantity,
            category = mapCategoryFromSupabase(dto.itemCategory),
            priority = dto.priority,
            isActive = dto.isActive,
            lastModified = parseDate(dto.updatedAt) ?: Date(),
            lastSynced = Date(),
            needsSync = false
        )
    }

    /**
     * Map StorageCategory enum to Supabase item_category string
     */
    private fun mapCategoryToSupabase(category: StorageCategory): String {
        return when (category) {
            StorageCategory.AMMUNITION -> "БК"
            StorageCategory.EQUIPMENT -> "Обладнання"
        }
    }

    /**
     * Map Supabase item_category string to StorageCategory enum
     */
    private fun mapCategoryFromSupabase(category: String?): StorageCategory {
        return when (category?.trim()) {
            "БК" -> StorageCategory.AMMUNITION
            "Обладнання" -> StorageCategory.EQUIPMENT
            else -> StorageCategory.EQUIPMENT
        }
    }

    /**
     * Format Date to ISO 8601 string for Supabase
     */
    private fun formatDate(date: Date?): String? {
        return date?.let { iso8601Format.format(it) }
    }

    /**
     * Parse ISO 8601 string from Supabase to Date
     */
    private fun parseDate(dateString: String?): Date? {
        return try {
            dateString?.let { iso8601Format.parse(it) }
        } catch (e: Exception) {
            null
        }
    }
}