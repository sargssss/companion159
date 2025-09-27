package com.lifelover.companion159.presentation.ui.navigation

import com.lifelover.companion159.domain.models.InventoryCategory
import kotlinx.serialization.Serializable

sealed interface NavigationDestination {
    @Serializable
    object Login : NavigationDestination

    @Serializable
    object MainMenu : NavigationDestination

    @Serializable
    data class InventoryDetail(val category: InventoryCategory) : NavigationDestination
}