package com.lifelover.companion159.data.remote.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class CrewInventoryItem(
    val id: Long? = null,

    @SerialName("tenant_id")
    val tenantId: Long = 9999, // its always 0 on server but if set 0 there gives error on request

    @SerialName("crew_name")
    val crewName: String,

    @SerialName("crew_type")
    val crewType: String? = null,

    @SerialName("item_name")
    val itemName: String,

    @SerialName("item_category")
    val itemCategory: String? = null,

    val unit: String = "шт",

    @SerialName("available_quantity")
    val availableQuantity: Int = 0,

    @SerialName("needed_quantity")
    val neededQuantity: Int = 0,

    val priority: String = "medium",

    val description: String? = null,
    val notes: String? = null,

    @SerialName("last_need_updated_at")
    val lastNeedUpdatedAt: String? = null,

    @SerialName("needed_by")
    val neededBy: String? = null,

    @SerialName("created_by")
    val createdBy: Long? = null,

    @SerialName("updated_by")
    val updatedBy: Long? = null,

    // FIXED: Use JsonObject or remove entirely
    val metadata: JsonObject? = null,  // Changed from Map<String, Any>?

    @SerialName("is_active")
    val isActive: Boolean = true,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)