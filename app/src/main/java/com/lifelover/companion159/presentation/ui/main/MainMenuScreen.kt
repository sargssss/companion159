package com.lifelover.companion159.presentation.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifelover.companion159.R
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.presentation.ui.auth.LogoutConfirmationDialog
import com.lifelover.companion159.presentation.ui.components.CategoryGrid
import com.lifelover.companion159.presentation.ui.components.UserMenu
import com.lifelover.companion159.presentation.viewmodels.AuthViewModel
import com.lifelover.companion159.presentation.viewmodels.MainMenuViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    onDisplayCategorySelected: (DisplayCategory) -> Unit = {},
    onLogout: () -> Unit = {},
    onChangePosition: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
    mainMenuViewModel: MainMenuViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsState()
    val syncState by mainMenuViewModel.syncState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Show sync success message
    var showSyncSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(syncState.lastSyncTime) {
        if (syncState.lastSyncTime != null && !syncState.isSyncing) {
            showSyncSuccess = true
        }
    }

    // Handle logout navigation
    LaunchedEffect(authState.hasExplicitlyLoggedOut) {
        if (authState.hasExplicitlyLoggedOut) {
            authViewModel.clearLogoutFlag()
            onLogout()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.in_stock),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    // Manual sync button (for force full sync)
                    IconButton(
                        onClick = { mainMenuViewModel.triggerManualSync() },
                        enabled = !syncState.isSyncing
                    ) {
                        if (syncState.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.sync_check),
                                contentDescription = stringResource(R.string.sync),
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
        },
        snackbarHost = {
            // Show success message
            if (showSyncSuccess) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { showSyncSuccess = false }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text("✅ Синхронізація завершена")
                }
            }

            // Show error message
            syncState.error?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    action = {
                        TextButton(onClick = { mainMenuViewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text("❌ Помилка синхронізації: $error")
                }
            }
        }
    ) { paddingValues ->
        // Category grid
        CategoryGrid(
            modifier = Modifier.padding(paddingValues),
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