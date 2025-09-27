package com.lifelover.companion159.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InventoryItemDto(
    val id: Long? = null,
    val name: String,
    val quantity: Int,
    val category: String,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

@Serializable
data class InventoryItemCreateDto(
    val name: String,
    val quantity: Int,
    val category: String
)

@Serializable
data class InventoryItemUpdateDto(
    val name: String? = null,
    val quantity: Int? = null,
    val category: String? = null
)

@Serializable
data class InventoryStatsDto(
    val category: String,
    @SerialName("total_items")
    val totalItems: Long,
    @SerialName("total_quantity")
    val totalQuantity: Long
)