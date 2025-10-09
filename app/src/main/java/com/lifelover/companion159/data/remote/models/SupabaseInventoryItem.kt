package com.lifelover.companion159.data.remote.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseInventoryItem(
    val id: String? = null,
    val name: String,
    val quantity: Int,
    val category: String,
    val position: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("is_deleted")
    val isDeleted: Boolean = false
)