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
import com.lifelover.companion159.presentation.ui.position.PositionScreen
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.presentation.viewmodels.InventoryViewModel
import com.lifelover.companion159.presentation.ui.inventory.AddEditItemScreen
import kotlinx.serialization.Serializable

@Serializable
object Login

@Serializable
object Position

@Serializable
object MainMenu

@Serializable
data class InventoryDetail(val category: InventoryCategory)

@Serializable
data class AddItem(val category: InventoryCategory)

@Serializable
data class EditItem(
    val category: InventoryCategory,
    val itemId: Long,
    val itemName: String,
    val itemQuantity: Int
)

@Composable
fun AppNavigation(
    navController: NavHostController,
    isPositionSet: Boolean
) {
    val startDestination = when {
        !isPositionSet -> Position
        SupabaseConfig.isConfigured -> Login
        else -> MainMenu
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<Position> {
            PositionScreen(
                onPositionSaved = {
                    navController.navigate(
                        if (SupabaseConfig.isConfigured) Login else MainMenu
                    ) {
                        popUpTo(Position) { inclusive = true }
                    }
                },
                showBackButton = false
            )
        }

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
                },
                onChangePosition = {
                    navController.navigate(Position)
                }
            )
        }

        composable<InventoryDetail> { backStackEntry ->
            val args = backStackEntry.toRoute<InventoryDetail>()
            InventoryScreen(
                inventoryCategory = args.category,
                onBackPressed = { navController.popBackStack() },
                onAddItem = { // NEW: Navigate to Add screen
                    navController.navigate(AddItem(args.category))
                },
                onEditItem = { item -> // FIXED: Use correct property names
                    navController.navigate(
                        EditItem(
                            category = args.category,
                            itemId = item.id,
                            itemName = item.itemName,
                            itemQuantity = item.availableQuantity
                        )
                    )
                }
            )
        }

        // NEW: Add Item screen
        composable<AddItem> { backStackEntry ->
            val args = backStackEntry.toRoute<AddItem>()
            val viewModel: InventoryViewModel = hiltViewModel()

            AddEditItemScreen(
                category = args.category,
                itemId = null, // null = create mode
                itemName = null,
                itemQuantity = null,
                onBackPressed = { navController.popBackStack() },
                onSave = { name, quantity ->
                    viewModel.addNewItem(name, quantity, args.category)
                    navController.popBackStack()
                }
            )
        }

        // NEW: Edit Item screen
        composable<EditItem> { backStackEntry ->
            val args = backStackEntry.toRoute<EditItem>()
            val viewModel: InventoryViewModel = hiltViewModel()

            AddEditItemScreen(
                category = args.category,
                itemId = args.itemId,
                itemName = args.itemName,
                itemQuantity = args.itemQuantity,
                onBackPressed = { navController.popBackStack() },
                onSave = { name, quantity ->
                    // FIXED: Use correct property names and include crewName
                    val item = InventoryItem(
                        id = args.itemId,
                        itemName = args.itemName,
                        availableQuantity = args.itemQuantity,
                        category = args.category,
                        crewName = "" // Will be set by repository
                    )
                    viewModel.updateFullItem(item, name, quantity)
                    navController.popBackStack()
                }
            )
        }
    }
}