package com.lifelover.companion159.utils

import androidx.navigation.NavController
import com.lifelover.companion159.MainMenu
import com.lifelover.companion159.data.ui.InventoryType

fun NavController.navigateToMainMenu() {
    navigate(MainMenu)
}

fun NavController.goBack() {
    popBackStack()
}

fun NavController.navigateToInventory(type: InventoryType) {
    this.navigate("inventory/${type.name}")
}