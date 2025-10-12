package com.lifelover.companion159.data.mappers

import com.lifelover.companion159.data.local.entities.InventoryCategory as RoomCategory
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.data.remote.models.CrewInventoryItem
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.StorageCategory
import java.util.Date

/**
 * Centralized mapper for all inventory conversions
 * Handles: Entity ↔ Domain ↔ API
 */
object InventoryMapper {

    // Entity → Domain
    fun toDomain(entity: InventoryItemEntity): InventoryItem {
        return InventoryItem(
            id = entity.id,
            itemName = entity.itemName,
            availableQuantity = entity.availableQuantity,
            neededQuantity = entity.neededQuantity,
            category = when (entity.category) {
                RoomCategory.AMMUNITION -> StorageCategory.AMMUNITION
                else -> StorageCategory.EQUIPMENT
            },
            crewName = entity.crewName,
            lastModified = entity.lastModified,
            isSynced = !entity.needsSync
        )
    }

    // Domain → Entity
    fun toEntity(
        domain: InventoryItem,
        userId: String? = null,
        supabaseId: Long? = null
    ): InventoryItemEntity {
        return InventoryItemEntity(
            id = domain.id,
            itemName = domain.itemName,
            availableQuantity = domain.availableQuantity,
            neededQuantity = domain.neededQuantity,
            category = when (domain.category) {
                StorageCategory.AMMUNITION -> RoomCategory.AMMUNITION
                StorageCategory.EQUIPMENT -> RoomCategory.EQUIPMENT
            },
            userId = userId,
            crewName = domain.crewName,
            supabaseId = supabaseId,
            createdAt = Date(),
            lastModified = domain.lastModified,
            lastSynced = if (domain.isSynced) Date() else null,
            needsSync = !domain.isSynced,
            isActive = true
        )
    }

    // API → Entity
    fun fromApi(
        api: CrewInventoryItem,
        userId: String,
        existingCategory: RoomCategory = RoomCategory.EQUIPMENT
    ): InventoryItemEntity {
        return InventoryItemEntity(
            id = 0,
            itemName = api.itemName,
            availableQuantity = api.availableQuantity,
            neededQuantity = api.neededQuantity,
            category = existingCategory,
            userId = userId,
            crewName = api.crewName,
            supabaseId = api.id,
            createdAt = Date(),
            lastModified = Date(),
            lastSynced = Date(),
            needsSync = false,
            isActive = api.isActive
        )
    }

    // Entity → API
    fun toApi(entity: InventoryItemEntity): CrewInventoryItem {
        return CrewInventoryItem(
            id = entity.supabaseId,
            tenantId = 0,
            crewName = entity.crewName,
            crewType = null,
            itemName = entity.itemName,
            itemCategory = null,
            unit = "шт",
            availableQuantity = entity.availableQuantity,
            neededQuantity = entity.neededQuantity,
            priority = "medium",
            description = null,
            notes = null,
            lastNeedUpdatedAt = null,
            neededBy = null,
            createdBy = null,
            updatedBy = null,
            metadata = null,
            isActive = entity.isActive,
            createdAt = null,
            updatedAt = null
        )
    }
}