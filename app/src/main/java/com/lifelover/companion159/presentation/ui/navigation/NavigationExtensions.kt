package com.lifelover.companion159.presentation.ui.navigation

import androidx.navigation.NavController
import com.lifelover.companion159.data.local.entities.InventoryCategory

fun NavController.navigateToMainMenu() {
    navigate(NavigationDestination.MainMenu) {
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

fun NavController.navigateToInventoryDetail(category: InventoryCategory) {
    navigate(NavigationDestination.InventoryDetail(category))
}

fun NavController.navigateToLogin() {
    navigate(NavigationDestination.Login) {
        popUpTo(0) { inclusive = true }
    }
}

fun NavController.navigateBack() {
    popBackStack()
}