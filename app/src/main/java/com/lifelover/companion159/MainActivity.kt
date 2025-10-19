package com.lifelover.companion159

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.data.repository.PositionRepository
import com.lifelover.companion159.presentation.theme.Companion159Theme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.activity.viewModels
import com.lifelover.companion159.data.remote.sync.SyncOrchestrator
import com.lifelover.companion159.presentation.viewmodels.AuthViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    @Inject
    lateinit var syncOrchestrator: SyncOrchestrator

    @Inject
    lateinit var positionRepository: PositionRepository

    @Inject
    lateinit var inventoryRepository: InventoryRepository

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        syncOrchestrator.setupRepositoryCallback(inventoryRepository)

        setContent {
            Companion159Theme {
                val darkTheme = isSystemInDarkTheme()

                SideEffect {
                    window.statusBarColor = Color.Transparent.toArgb()
                    window.navigationBarColor = Color.Transparent.toArgb()

                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authState by authViewModel.state.collectAsState()
                    val currentPosition = positionRepository.currentPosition.collectAsState().value

                    AppNavigation(
                        navController = navController,
                        currentPosition = currentPosition,
                        isAuthenticated = authState.isAuthenticated,
                        positionRepository = positionRepository
                    )

                    LaunchedEffect(authState.isAuthenticated, currentPosition) {
                        if (authState.isAuthenticated && currentPosition != null) {
                            syncOrchestrator.syncOnStartup()
                        }
                    }
                }
            }
        }
    }
}