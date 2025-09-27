package com.lifelover.companion159.data.remote.dto

import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.utils.formatDateForApi
import com.lifelover.companion159.utils.parseApiTimestamp
import java.util.Date

data class ApiInventoryItem(
    val id: String,
    val name: String,
    val quantity: Int,
    val category: String,
    val lastModified: String, // ISO timestamp
    val userId: String
)

data class SyncResponse(
    val items: List<ApiInventoryItem>,
    val deletedIds: List<String>,
    val timestamp: String
)

// Extension functions for API conversion
fun InventoryItemEntity.toApiModel(): ApiInventoryItem {
    return ApiInventoryItem(
        id = serverId ?: "",
        name = name,
        quantity = quantity,
        category = category.name.lowercase(),
        lastModified = formatDateForApi(lastModified),
        userId = "current_user" // Replace with real userId
    )
}

fun ApiInventoryItem.toEntity(): InventoryItemEntity {
    return InventoryItemEntity(
        id = 0, // Room will generate new local ID
        name = name,
        quantity = quantity,
        category = InventoryCategory.valueOf(category.uppercase()),
        serverId = id,
        lastModified = parseApiTimestamp(lastModified),
        lastSynced = Date(),
        needsSync = false
    )
}