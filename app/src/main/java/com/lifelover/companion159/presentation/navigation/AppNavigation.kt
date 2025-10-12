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
import com.lifelover.companion159.domain.models.DisplayCategory
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
data class InventoryDetail(val displayCategory: String)  // CHANGED: use String for DisplayCategory

@Serializable
data class AddItem(val displayCategory: String)  // CHANGED

@Serializable
data class EditItem(
    val displayCategory: String,  // CHANGED
    val itemId: Long,
    val itemName: String,
    val availableQuantity: Int,
    val neededQuantity: Int  // NEW
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
                onDisplayCategorySelected = { displayCategory ->
                    navController.navigate(InventoryDetail(displayCategory.name))
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
            val displayCategory = DisplayCategory.valueOf(args.displayCategory)

            InventoryScreen(
                displayCategory = displayCategory,
                onBackPressed = { navController.popBackStack() },
                onAddItem = {
                    navController.navigate(AddItem(displayCategory.name))
                },
                onEditItem = { item ->
                    navController.navigate(
                        EditItem(
                            displayCategory = displayCategory.name,
                            itemId = item.id,
                            itemName = item.itemName,
                            availableQuantity = item.availableQuantity,
                            neededQuantity = item.neededQuantity
                        )
                    )
                }
            )
        }

        composable<AddItem> { backStackEntry ->
            val args = backStackEntry.toRoute<AddItem>()
            val displayCategory = DisplayCategory.valueOf(args.displayCategory)
            val viewModel: InventoryViewModel = hiltViewModel()

            AddEditItemScreen(
                displayCategory = displayCategory,
                itemId = null,
                itemName = null,
                availableQuantity = null,
                neededQuantity = null,
                onBackPressed = { navController.popBackStack() },
                onSave = { name, availableQty, neededQty ->
                    viewModel.addNewItem(name, availableQty, neededQty, displayCategory)
                    navController.popBackStack()
                }
            )
        }

        composable<EditItem> { backStackEntry ->
            val args = backStackEntry.toRoute<EditItem>()
            val displayCategory = DisplayCategory.valueOf(args.displayCategory)
            val viewModel: InventoryViewModel = hiltViewModel()

            AddEditItemScreen(
                displayCategory = displayCategory,
                itemId = args.itemId,
                itemName = args.itemName,
                availableQuantity = args.availableQuantity,
                neededQuantity = args.neededQuantity,
                onBackPressed = { navController.popBackStack() },
                onSave = { name, availableQty, neededQty ->
                    viewModel.updateFullItem(
                        itemId = args.itemId,
                        newName = name,
                        newAvailableQuantity = availableQty,
                        newNeededQuantity = neededQty,
                        displayCategory = displayCategory
                    )
                    navController.popBackStack()
                }
            )
        }
    }
}