package com.lifelover.companion159

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.lifelover.companion159.ui.inventory.InventoryScreen
import com.lifelover.companion159.ui.main.MainMenuScreen
import com.lifelover.companion159.ui.theme.Companion159Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Companion159Theme {
                NavigationApp()
            }
        }
    }

    @Composable
    fun NavigationApp() {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = MainMenu
        ) {
            composable<MainMenu> {
                MainMenuScreen(
                    navController = navController,
                    onInventoryTypeSelected = { type ->
                        navController.navigate(DestInventoryDetail(type))
                    }
                )
            }
            composable<DestInventoryDetail> { backStackEntry ->
                val args = backStackEntry.toRoute<DestInventoryDetail>()
                InventoryScreen(
                    inventoryType = args.type,
                    onBackPressed = { navController.goBack() }
                )
            }
        }
    }
}