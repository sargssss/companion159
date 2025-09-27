package com.lifelover.companion159

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.lifelover.companion159.presentation.navigation.AppNavigation
import com.lifelover.companion159.presentation.theme.Companion159Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Companion159Theme {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
    }
}