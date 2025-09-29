package com.lifelover.companion159.presentation.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.remote.config.SupabaseConfig
import com.lifelover.companion159.presentation.ui.auth.LoginScreen
import com.lifelover.companion159.presentation.ui.inventory.InventoryScreen
import com.lifelover.companion159.presentation.ui.main.MainMenuScreen
import kotlinx.serialization.Serializable

@Serializable
object Login

@Serializable
object MainMenu

@Serializable
data class InventoryDetail(val category: InventoryCategory)

@Composable
fun AppNavigation(
    navController: NavHostController
) {
    val startDestination = if (SupabaseConfig.isConfigured) {
        Login
    } else {
        MainMenu
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(MainMenu) {
                        popUpTo(Login) { inclusive = true }
                    }
                }
            )
        }

        composable<MainMenu> {
            MainMenuScreen(
                onInventoryTypeSelected = { category ->
                    navController.navigate(InventoryDetail(category))
                },
                onLogout = {
                    navController.navigate(Login) {
                        popUpTo(MainMenu) { inclusive = true }
                    }
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