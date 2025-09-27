package com.lifelover.companion159

import androidx.navigation.NavController
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