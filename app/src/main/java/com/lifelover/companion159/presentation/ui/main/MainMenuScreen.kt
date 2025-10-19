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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Alignment
import com.lifelover.companion159.presentation.ui.components.PositionStatusCard

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
    val currentPosition by mainMenuViewModel.currentPosition.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }

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
                    Column {
                        Text(
                            text = stringResource(id = R.string.inventory),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    // Manual sync button
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
                                painter = painterResource(R.drawable.sync_check),
                                contentDescription = stringResource(R.string.sync)
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            if (!currentPosition.isNullOrEmpty()) {
                PositionStatusCard(
                    position = currentPosition!!,
                    onChangePosition = onChangePosition
                )
            }

            // Category grid
            CategoryGrid(
                modifier = Modifier.padding(vertical = 8.dp),
                onCategorySelected = onDisplayCategorySelected
            )
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