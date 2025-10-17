package com.lifelover.companion159.presentation.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifelover.companion159.R
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.presentation.ui.components.CategoryGrid
import com.lifelover.companion159.presentation.ui.components.LogoutConfirmationDialog
import com.lifelover.companion159.presentation.ui.components.UserMenu
import com.lifelover.companion159.presentation.viewmodels.AuthViewModel
import com.lifelover.companion159.presentation.viewmodels.MainMenuViewModel

// Lines 23-36: Update MainMenuScreen signature
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    onDisplayCategorySelected: (DisplayCategory) -> Unit = {},
    onLogout: () -> Unit = {},
    onChangePosition: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
    mainMenuViewModel: MainMenuViewModel = hiltViewModel() // ADD: ViewModel for sync
) {
    val authState by authViewModel.state.collectAsState()
    val syncState by mainMenuViewModel.syncState.collectAsState() // ADD: Observe sync state
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Handle logout navigation
    LaunchedEffect(authState.hasExplicitlyLoggedOut) {
        if (authState.hasExplicitlyLoggedOut) {
            authViewModel.clearLogoutFlag()
            onLogout()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar with user menu AND sync button
        TopAppBar(
            title = {
                Text(
                    text = stringResource(id = R.string.in_stock),
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                // ADD: Sync button
                IconButton(
                    onClick = { mainMenuViewModel.triggerManualSync() },
                    enabled = !syncState.isSyncing
                ) {
                    if (syncState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Face, //change icon
                            contentDescription = "Синхронізувати",
                            tint = if (syncState.lastSyncTime != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

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

        // ADD: Sync status message
        syncState.error?.let { error ->
            Snackbar(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("Помилка синхронізації: $error")
            }
        }
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