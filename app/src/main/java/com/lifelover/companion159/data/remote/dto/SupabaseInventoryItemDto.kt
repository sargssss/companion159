package com.lifelover.companion159.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for Supabase crew_inventory_items_duplicate table
 */
@Serializable
data class SupabaseInventoryItemDto(
    @SerialName("id")
    val id: Long? = null,

    @SerialName("tenant_id")
    val tenantId: Long = 9999,

    @SerialName("crew_name")
    val crewName: String,

    @SerialName("item_name")
    val itemName: String,

    @SerialName("available_quantity")
    val availableQuantity: Int = 0,

    @SerialName("needed_quantity")
    val neededQuantity: Int = 0,

    @SerialName("item_category")
    val itemCategory: String? = null,

    @SerialName("unit")
    val unit: String = "шт.",

    @SerialName("priority")
    val priority: String = "medium",

    @SerialName("crew_type")
    val crewType: String? = null,

    @SerialName("description")
    val description: String? = null,

    @SerialName("notes")
    val notes: String? = null,

    @SerialName("last_need_updated_at")
    val lastNeedUpdatedAt: String? = null,

    @SerialName("needed_by")
    val neededBy: String? = null,

    @SerialName("created_by")
    val createdBy: Long? = null,

    @SerialName("updated_by")
    val updatedBy: Long? = null,

    @SerialName("metadata")
    val metadata: Map<String, String>? = null,

    @SerialName("is_active")
    val isActive: Boolean = true,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class SupabaseResponse<T>(
    val data: List<T>? = null,
    val error: SupabaseError? = null
)

@Serializable
data class SupabaseError(
    val message: String,
    val code: String? = null,
    val details: String? = null
)