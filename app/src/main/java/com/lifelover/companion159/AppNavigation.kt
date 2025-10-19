package com.lifelover.companion159

import android.util.Log
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.lifelover.companion159.data.remote.SupabaseConfig
import com.lifelover.companion159.data.repository.PositionRepository
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.presentation.ui.auth.LoginScreen
import com.lifelover.companion159.presentation.ui.inventory.AddEditItemScreen
import com.lifelover.companion159.presentation.ui.inventory.InventoryScreen
import com.lifelover.companion159.presentation.ui.main.MainMenuScreen
import com.lifelover.companion159.presentation.ui.position.PositionScreen
import com.lifelover.companion159.presentation.viewmodels.InventoryViewModel
import kotlinx.serialization.Serializable

@Serializable
object Login

@Serializable
object Position

@Serializable
object MainMenu

@Serializable
data class InventoryDetail(val displayCategory: String)

@Serializable
data class AddItem(val displayCategory: String)

@Serializable
data class EditItem(
    val displayCategory: String,
    val itemId: Long,
    val itemName: String,
    val availableQuantity: Int,
    val neededQuantity: Int
)

@Composable
fun AppNavigation(
    navController: NavHostController,
    currentPosition: String?,
    isAuthenticated: Boolean,
    positionRepository: PositionRepository
) {
    val TAG = "AppNavigation"

    val startDestination = when {
        !SupabaseConfig.isConfigured -> MainMenu
        !isAuthenticated -> Login
        positionRepository.shouldShowPositionSelection() -> Position
        else -> MainMenu
    }

    Log.d(TAG, "🚀 Starting at: ${startDestination::class.simpleName}")
    Log.d(TAG, "   authenticated: $isAuthenticated")
    Log.d(TAG, "   currentPosition: $currentPosition")
    Log.d(TAG, "   shouldShowPosition: ${positionRepository.shouldShowPositionSelection()}")

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable<Position> {
            PositionScreen(
                onPositionSaved = {
                    // Navigate to main menu
                    navController.navigate(MainMenu) {
                        popUpTo(Position) { inclusive = true }
                    }
                },
                showBackButton = isAuthenticated
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
                    // When changing position, navigate to Position screen
                    navController.navigate(Position) {
                        // Don't remove MainMenu from back stack
                        // User can go back if they cancel
                    }
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