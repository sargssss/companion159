package com.lifelover.companion159.presentation.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifelover.companion159.R
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.presentation.ui.components.CategoryGrid
import com.lifelover.companion159.presentation.ui.components.LogoutConfirmationDialog
import com.lifelover.companion159.presentation.ui.components.UserMenu
import com.lifelover.companion159.presentation.viewmodels.AuthViewModel

/**
 * Main menu screen
 *
 * Uses reusable components
 * - UserMenu component
 * - CategoryGrid component
 * - LogoutConfirmationDialog component
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    onDisplayCategorySelected: (DisplayCategory) -> Unit = {},
    onLogout: () -> Unit = {},
    onChangePosition: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Handle logout navigation
    LaunchedEffect(authState.hasExplicitlyLoggedOut) {
        if (authState.hasExplicitlyLoggedOut) {
            authViewModel.clearLogoutFlag()
            onLogout()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar with user menu
        TopAppBar(
            title = {
                Text(
                    text = stringResource(id = R.string.in_stock),
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                UserMenu(
                    userEmail = authState.userEmail,
                    isAuthenticated = authState.isAuthenticated,
                    onChangePosition = onChangePosition,
                    onSettings = { /* TODO: Settings screen */ },
                    onLogout = { showLogoutDialog = true }
                )
            }
        )

        // Category grid
        CategoryGrid(
            onCategorySelected = onDisplayCategorySelected
        )
    }

    // Logout confirmation
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            userEmail = authState.userEmail,
            onConfirm = {
                showLogoutDialog = false
                authViewModel.signOut()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}