package com.lifelover.companion159.data.remote.sync

import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.data.remote.dto.SupabaseInventoryItemDto
import com.lifelover.companion159.domain.models.StorageCategory
import java.util.Date

/**
 * Centralized mapper for all local ↔ remote transformations
 *
 * Single source of truth for:
 * - Entity ↔ DTO conversions
 * - Category name mappings
 * - Date formatting for sync
 * - Conflict resolution logic
 *
 * By consolidating all transformations here, we ensure:
 * 1. Consistency across sync operations
 * 2. Easy to update when server schema changes
 * 3. No transformation logic scattered in Service layer
 * 4. Reusable by different sync implementations (Supabase, REST, etc.)
 */
object SyncMapper {

    // ========================================
    // Entity ↔ DTO Transformations
    // ========================================

    /**
     * Convert local entity to remote DTO for upload
     * Handles all transformations: dates, categories, fields
     *
     * @param entity Local database entity
     * @return DTO ready for server upload
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
            createdAt = SyncDateUtils.formatForSync(entity.createdAt),
            updatedAt = SyncDateUtils.formatForSync(entity.lastModified)
        )
    }

    /**
     * Convert remote DTO to new local entity
     * Used when server has items not in local DB
     * Creates fresh entity with default sync markers
     *
     * @param dto Remote data from server
     * @param userId Current user ID for item ownership
     * @return Entity ready for local database insert
     */
    fun dtoToNewEntity(
        dto: SupabaseInventoryItemDto,
        userId: String?
    ): InventoryItemEntity {
        return InventoryItemEntity(
            id = 0, // Room will auto-generate
            supabaseId = dto.id,
            itemName = dto.itemName,
            availableQuantity = dto.availableQuantity,
            neededQuantity = dto.neededQuantity,
            category = mapCategoryFromRemote(dto.itemCategory),
            userId = userId,
            crewName = dto.crewName,
            priority = dto.priority,
            isActive = dto.isActive,
            createdAt = SyncDateUtils.parseFromSync(dto.createdAt) ?: Date(),
            lastModified = SyncDateUtils.parseFromSync(dto.updatedAt) ?: Date(),
            lastSynced = Date(),
            needsSync = false
        )
    }

    /**
     * Merge remote DTO into existing local entity
     * Updates only sync-relevant fields, preserves local-only data
     *
     * @param existing Current local entity to merge into
     * @param dto Remote data from server
     * @return Updated entity ready for database update
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
            lastModified = SyncDateUtils.parseFromSync(dto.updatedAt) ?: Date(),
            lastSynced = Date(),
            needsSync = false
        )
    }

    // ========================================
    // Category Mapping
    // ========================================

    /**
     * Map remote category string to StorageCategory enum
     * Handles null and unknown values gracefully
     *
     * @param category Remote category string (e.g., "БК", "Обладнання")
     * @return StorageCategory - defaults to EQUIPMENT for unknown values
     */
    fun mapCategoryFromRemote(category: String?): StorageCategory {
        return when (category?.trim()) {
            "БК" -> StorageCategory.AMMUNITION
            "Обладнання" -> StorageCategory.EQUIPMENT
            else -> StorageCategory.EQUIPMENT
        }
    }

    /**
     * Map StorageCategory enum to remote string
     * Ensures consistent naming for server communication
     *
     * @param category Local StorageCategory
     * @return Remote category string compatible with server schema
     */
    fun mapCategoryToRemote(category: StorageCategory): String {
        return when (category) {
            StorageCategory.AMMUNITION -> "БК"
            StorageCategory.EQUIPMENT -> "Обладнання"
        }
    }

    // ========================================
    // Conflict Resolution
    // ========================================

    /**
     * Determine if remote version is newer than local
     * Compares timestamps for merge conflict resolution
     *
     * Used in download sync to decide whether to update local item
     * with server data or keep local version
     *
     * @param localModified Local modification timestamp
     * @param remoteUpdatedAt Remote update timestamp string (ISO8601)
     * @return true if remote is newer, false otherwise or on parse error
     */
    fun isRemoteNewer(
        localModified: Date,
        remoteUpdatedAt: String?
    ): Boolean {
        val remoteDate = remoteUpdatedAt?.let { SyncDateUtils.parseFromSync(it) } ?: return false
        return remoteDate.after(localModified)
    }
}