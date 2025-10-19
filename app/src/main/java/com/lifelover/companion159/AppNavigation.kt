package com.lifelover.companion159

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import android.util.Log
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
    isAuthenticated: Boolean,
    positionRepository: PositionRepository
) {
    val TAG = "AppNavigation"

    // Get position directly from flow - always current
    val currentPosition by positionRepository.currentPosition.collectAsState()

    Log.d(TAG, "=== STATE ===")
    Log.d(TAG, "isAuthenticated: $isAuthenticated")
    Log.d(TAG, "currentPosition: '$currentPosition'")

    // Determine destination based on CURRENT state
    val targetDestination = when {
        !SupabaseConfig.isConfigured -> {
            Log.d(TAG, "→ MainMenu (Supabase not configured)")
            MainMenu
        }
        !isAuthenticated -> {
            Log.d(TAG, "→ Login (not authenticated)")
            Login
        }
        currentPosition.isNullOrBlank() -> {
            Log.d(TAG, "→ Position (position not set)")
            Position
        }
        else -> {
            Log.d(TAG, "→ MainMenu (authenticated + position set)")
            MainMenu
        }
    }

    Log.d(TAG, "Target: ${targetDestination::class.simpleName}")

    // Navigate when destination changes
    LaunchedEffect(targetDestination) {
        val currentBackStackEntry = navController.currentBackStackEntry
        val currentRoute = currentBackStackEntry?.destination?.route

        Log.d(TAG, "Check navigation: current=$currentRoute → target=${targetDestination::class.simpleName}")

        // Compare by class name
        val shouldNavigate = when (targetDestination) {
            is MainMenu -> currentRoute?.contains("MainMenu") != true
            is Login -> currentRoute?.contains("Login") != true
            is Position -> currentRoute?.contains("Position") != true
            else -> false
        }

        if (shouldNavigate) {
            Log.d(TAG, "→ Navigate to ${targetDestination::class.simpleName}")
            navController.navigate(targetDestination) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // Set initial destination for NavHost
    val startDestination = remember(isAuthenticated, currentPosition) {
        val initial = when {
            !SupabaseConfig.isConfigured -> MainMenu
            !isAuthenticated -> Login
            currentPosition.isNullOrBlank() -> Position
            else -> MainMenu
        }
        Log.d(TAG, "Start destination: ${initial::class.simpleName}")
        initial
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable<Position> {
            Log.d(TAG, "🎨 PositionScreen")
            PositionScreen(
                onPositionSaved = {
                    Log.d(TAG, "Position saved → MainMenu")
                    navController.navigate(MainMenu) {
                        popUpTo(Position) { inclusive = true }
                    }
                },
                showBackButton = isAuthenticated
            )
        }

        composable<Login> {
            Log.d(TAG, "🎨 LoginScreen")
            LoginScreen(
                onLoginSuccess = {
                    Log.d(TAG, "Login success → check position")
                    navController.navigate(MainMenu) {
                        popUpTo(Login) { inclusive = true }
                    }
                }
            )
        }

        composable<MainMenu> {
            Log.d(TAG, "🎨 MainMenuScreen")
            MainMenuScreen(
                onDisplayCategorySelected = { displayCategory ->
                    navController.navigate(InventoryDetail(displayCategory.name))
                },
                onLogout = {
                    Log.d(TAG, "Logout → Login")
                    navController.navigate(Login) {
                        popUpTo(MainMenu) { inclusive = true }
                    }
                },
                onChangePosition = {
                    Log.d(TAG, "Change position → Position")
                    navController.navigate(Position)
                }
            )
        }

        composable<InventoryDetail> { backStackEntry ->
            val args = backStackEntry.toRoute<InventoryDetail>()
            val displayCategory = DisplayCategory.valueOf(args.displayCategory)
            Log.d(TAG, "🎨 InventoryScreen (${displayCategory.name})")
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
            Log.d(TAG, "🎨 AddEditItemScreen (Add)")
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
            Log.d(TAG, "🎨 AddEditItemScreen (Edit)")
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