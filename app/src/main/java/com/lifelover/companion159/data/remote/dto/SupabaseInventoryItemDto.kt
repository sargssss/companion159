package com.lifelover.companion159.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for Supabase crew_inventory_items_duplicate table
 *
 * Maps between local Room entity and remote Supabase table
 * Handles all required and optional fields per Supabase schema
 */
@Serializable
data class SupabaseInventoryItemDto(
    // Primary key
    @SerialName("id")
    val id: Long? = null,

    // Required fields (NOT NULL in Supabase)
    @SerialName("tenant_id")
    val tenantId: Long = 9999, // Always 0 with value 0 throws error so other value

    @SerialName("crew_name")
    val crewName: String,

    @SerialName("item_name")
    val itemName: String,

    // Quantities
    @SerialName("available_quantity")
    val availableQuantity: Int = 0,

    @SerialName("needed_quantity")
    val neededQuantity: Int = 0,

    // Category mapping: AMMUNITION → "БК", EQUIPMENT → "Обладнання"
    @SerialName("item_category")
    val itemCategory: String? = null,

    // Fixed values per requirements
    @SerialName("unit")
    val unit: String = "шт.",

    @SerialName("priority")
    val priority: String = "medium",

    @SerialName("crew_type")
    val crewType: String? = null,

    // Empty/unused fields
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

    // Status
    @SerialName("is_active")
    val isActive: Boolean = true,

    // Timestamps - using String for ISO 8601 format
    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Response wrapper for Supabase queries
 * Supabase returns arrays even for single items
 */
@Serializable
data class SupabaseResponse<T>(
    val data: List<T>? = null,
    val error: SupabaseError? = null
)

/**
 * Error response from Supabase
 */
@Serializable
data class SupabaseError(
    val message: String,
    val code: String? = null,
    val details: String? = null
)