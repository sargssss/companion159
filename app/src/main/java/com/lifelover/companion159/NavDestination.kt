package com.lifelover.companion159

import kotlinx.serialization.Serializable


@Serializable
object Inventory

@Serializable
object MainMenu

@Serializable
data class DestInventoryDetail(
    val type: InventoryType
)
