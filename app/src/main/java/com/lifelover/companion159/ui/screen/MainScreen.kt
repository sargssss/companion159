package com.lifelover.companion159.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifelover.companion159.data.MainViewModel
import com.lifelover.companion159.data.model.Post
import com.lifelover.companion159.ui.UiState
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Main screen composable function
 *
 * @Composable annotation marks this as a Compose function
 * Compose functions describe UI rather than create UI widgets
 *
 * viewModel() automatically creates and provides the ViewModel
 * It handles lifecycle and survives configuration changes
 */
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    /**
     * Column arranges children vertically
     * Similar to LinearLayout with vertical orientation
     */
    Column(
        modifier = modifier
            .fillMaxSize()      // Take up all available space
            .padding(16.dp)     // Add 16dp padding on all sides
    ) {

        Button(
            onClick = { navController.navigate("post") },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            Text("Go to Post Screen")
        }
    }
}
