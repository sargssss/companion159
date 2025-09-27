package com.lifelover.companion159.presentation.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.presentation.ui.inventory.InventoryScreen
import com.lifelover.companion159.presentation.ui.main.MainMenuScreen
import kotlinx.serialization.Serializable

@Serializable
object MainMenu

@Serializable
data class InventoryDetail(val category: InventoryCategory)

@Composable
fun AppNavigation(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = MainMenu
    ) {
        composable<MainMenu> {
            MainMenuScreen(
                onInventoryTypeSelected = { category ->
                    navController.navigate(InventoryDetail(category))
                }
            )
        }

        composable<InventoryDetail> { backStackEntry ->
            val args = backStackEntry.toRoute<InventoryDetail>()
            InventoryScreen(
                inventoryCategory = args.category,
                onBackPressed = { navController.popBackStack() }
            )
        }
    }
}