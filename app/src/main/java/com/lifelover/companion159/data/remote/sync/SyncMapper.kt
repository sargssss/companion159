package com.lifelover.companion159.data.remote.sync

import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.data.remote.dto.SupabaseInventoryItemDto
import com.lifelover.companion159.domain.models.StorageCategory
import java.text.SimpleDateFormat
import java.util.*

/**
 * Single mapper for all Local ↔ Remote transformations
 *
 * Consolidates:
 * - Entity → DTO (for upload)
 * - DTO → Entity (for download)
 * - Conflict resolution timestamps
 * - Category mapping
 */
object SyncMapper {

    private val iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Entity → DTO for server upload
     */
    fun entityToDto(entity: InventoryItemEntity): SupabaseInventoryItemDto {
        return SupabaseInventoryItemDto(
            id = entity.supabaseId,
            tenantId = 0,
            crewName = entity.crewName,
            itemName = entity.itemName,
            availableQuantity = entity.availableQuantity,
            neededQuantity = entity.neededQuantity,
            itemCategory = mapCategoryToRemote(entity.category),
            unit = "шт.",
            priority = entity.priority,
            isActive = entity.isActive,
            createdAt = formatDate(entity.createdAt),
            updatedAt = formatDate(entity.lastModified)
        )
    }

    /**
     * DTO → Entity for local storage (new item from server)
     */
    fun dtoToNewEntity(
        dto: SupabaseInventoryItemDto,
        userId: String?
    ): InventoryItemEntity {
        return InventoryItemEntity(
            id = 0, // Room will generate
            tenantId = 0,
            supabaseId = dto.id,
            itemName = dto.itemName,
            availableQuantity = dto.availableQuantity,
            neededQuantity = dto.neededQuantity,
            category = mapCategoryFromRemote(dto.itemCategory),
            userId = userId,
            crewName = dto.crewName,
            priority = dto.priority,
            isActive = dto.isActive,
            createdAt = parseDate(dto.createdAt) ?: Date(),
            lastModified = parseDate(dto.updatedAt) ?: Date(),
            lastSynced = Date(), // Mark as synced since it came from server
            needsSync = false,
        )
    }

    /**
     * DTO → Entity for merging with existing local item
     */
    fun dtoToExistingEntity(
        existing: InventoryItemEntity,
        dto: SupabaseInventoryItemDto
    ): InventoryItemEntity {
        return existing.copy(
            itemName = dto.itemName,
            availableQuantity = dto.availableQuantity,
            neededQuantity = dto.neededQuantity,
            category = mapCategoryFromRemote(dto.itemCategory),
            priority = dto.priority,
            isActive = dto.isActive,
            lastModified = parseDate(dto.updatedAt) ?: Date(),
            lastSynced = Date(),
            needsSync = false
        )
    }

    /**
     * Check if remote version is newer
     */
    fun isRemoteNewer(
        localModified: Date,
        remoteUpdatedAt: String?
    ): Boolean {
        val remoteDate = remoteUpdatedAt?.let { parseDate(it) } ?: return false
        return remoteDate.after(localModified)
    }

    // ========================================
    // Helper functions
    // ========================================

    private fun mapCategoryToRemote(category: StorageCategory): String {
        return when (category) {
            StorageCategory.AMMUNITION -> "БК"
            StorageCategory.EQUIPMENT -> "Обладнання"
        }
    }

    private fun mapCategoryFromRemote(category: String?): StorageCategory {
        return when (category?.trim()) {
            "БК" -> StorageCategory.AMMUNITION
            "Обладнання" -> StorageCategory.EQUIPMENT
            else -> StorageCategory.EQUIPMENT
        }
    }

    private fun formatDate(date: Date?): String? {
        return date?.let { iso8601Format.format(it) }
    }

    private fun parseDate(dateString: String?): Date? {
        return try {
            dateString?.let { iso8601Format.parse(it) }
        } catch (e: Exception) {
            null
        }
    }
}