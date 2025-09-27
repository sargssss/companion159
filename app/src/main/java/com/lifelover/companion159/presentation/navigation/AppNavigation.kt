package com.lifelover.companion159.presentation.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.presentation.auth.AuthViewModel
import com.lifelover.companion159.presentation.auth.LoginScreen
import com.lifelover.companion159.presentation.ui.inventory.InventoryScreen
import com.lifelover.companion159.presentation.ui.main.MainMenuScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (authState.isLoggedIn) NavigationDestination.MainMenu else NavigationDestination.Login
    ) {
        composable<NavigationDestination.Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavigationDestination.MainMenu) {
                        popUpTo(NavigationDestination.Login) { inclusive = true }
                    }
                }
            )
        }

        composable<NavigationDestination.MainMenu> {
            MainMenuScreen(
                onInventoryTypeSelected = { category ->
                    navController.navigateToInventoryDetail(category)
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(NavigationDestination.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable<NavigationDestination.InventoryDetail> { backStackEntry ->
            val args = backStackEntry.toRoute<NavigationDestination.InventoryDetail>()
            InventoryScreen(
                inventoryCategory = args.category,
                onBackPressed = { navController.navigateBack() }
            )
        }
    }
}

// Оновлені destination'и
sealed interface NavigationDestination {
    @Serializable
    object Login : NavigationDestination

    @Serializable
    object MainMenu : NavigationDestination

    @Serializable
    data class InventoryDetail(val category: InventoryCategory) : NavigationDestination
}