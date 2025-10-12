package com.lifelover.companion159.presentation.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifelover.companion159.R
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.domain.models.titleRes
import com.lifelover.companion159.presentation.ui.auth.AuthViewModel
import com.lifelover.companion159.presentation.viewmodels.InventoryViewModel
import com.lifelover.companion159.presentation.viewmodels.SyncStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main menu screen with category buttons
 * Sync UI is kept for future implementation but not functional
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    onDisplayCategorySelected: (DisplayCategory) -> Unit = {},
    onLogout: () -> Unit = {},
    onChangePosition: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
    inventoryViewModel: InventoryViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsState()
    val inventoryState by inventoryViewModel.state.collectAsState()

    var showUserMenu by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Handle logout
    LaunchedEffect(authState.hasExplicitlyLoggedOut) {
        if (authState.hasExplicitlyLoggedOut) {
            authViewModel.clearLogoutFlag()
            onLogout()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(id = R.string.in_stock),
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                // Sync status indicator (kept for UI, but not functional)
                when (inventoryState.syncStatus) {
                    SyncStatus.SYNCING -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(32.dp)
                                .padding(top = 6.dp, end = 14.dp),
                            strokeWidth = 2.dp
                        )
                    }

                    SyncStatus.SUCCESS -> {
                        Icon(
                            painter = painterResource(R.drawable.sync_check),
                            contentDescription = "Синхронізовано",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(42.dp)
                                .padding(end = 12.dp)
                        )
                    }

                    SyncStatus.ERROR -> {
                        Icon(
                            painter = painterResource(R.drawable.sync_attention),
                            contentDescription = "Помилка синхронізації",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    else -> {}
                }

                // Sync button (kept for UI, shows message when clicked)
                if (authState.isAuthenticated && !authState.isOffline) {
                    IconButton(
                        onClick = { inventoryViewModel.sync() },
                        enabled = inventoryState.syncStatus != SyncStatus.SYNCING
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.repeat),
                            contentDescription = "Синхронізувати",
                            modifier = Modifier
                                .size(42.dp)
                                .padding(end = 12.dp)
                        )
                    }
                }

                // User menu
                if (authState.isAuthenticated) {
                    Box {
                        IconButton(onClick = { showUserMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "User menu"
                            )
                        }

                        DropdownMenu(
                            expanded = showUserMenu,
                            onDismissRequest = { showUserMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Змінити позицію") },
                                onClick = {
                                    showUserMenu = false
                                    onChangePosition()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = null
                                    )
                                }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text("Налаштування") },
                                onClick = {
                                    showUserMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null
                                    )
                                }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Вийти з аккаунту",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showUserMenu = false
                                    showLogoutDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ExitToApp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                } else {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Увійти"
                        )
                    }
                }
            }
        )

        // Status card (kept for UI)
        when {
            authState.isAuthenticated && !authState.isOffline -> {
                StatusCard(
                    userEmail = authState.userEmail,
                    lastSyncTime = inventoryState.lastSyncTime,
                    isOffline = false
                )
            }

            authState.isAuthenticated && authState.isOffline -> {
                StatusCard(
                    userEmail = authState.userEmail,
                    lastSyncTime = inventoryState.lastSyncTime,
                    isOffline = true
                )
            }

            !authState.isAuthenticated && authState.isOffline -> {
                OfflineStatusCard(message = "Офлайн режим (без аккаунта)")
            }

            else -> {
                OfflineStatusCard(message = "Режим без аккаунта")
            }
        }

        // Category buttons
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DisplayCategory.entries.forEach { category ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    onClick = { onDisplayCategorySelected(category) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(category.titleRes()),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Show messages from ViewModel
        inventoryState.message?.let { message ->
            LaunchedEffect(message) {
                // Show Snackbar
                inventoryViewModel.clearMessage()
            }
        }

        inventoryState.error?.let { error ->
            LaunchedEffect(error) {
                // Show Snackbar
                inventoryViewModel.clearError()
            }
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            userEmail = authState.userEmail,
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                authViewModel.signOut()
            }
        )
    }
}

/**
 * Logout confirmation dialog
 */
@Composable
private fun LogoutConfirmationDialog(
    userEmail: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text("Вийти з облікового запису?")
        },
        text = {
            Column {
                Text("Ви впевнені, що хочете вийти?")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = userEmail ?: "Невідомий користувач",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Вийти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )
}

/**
 * Status card showing user info
 * Sync info kept for UI but not functional
 */
@Composable
private fun StatusCard(
    userEmail: String?,
    lastSyncTime: Long?,
    isOffline: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOffline)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Користувач: ${userEmail ?: "Невідомий"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    if (isOffline) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.sync_attention),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Офлайн",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                if (!isOffline) {
                    lastSyncTime?.let { time ->
                        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                        Text(
                            text = "Синхр.: ${formatter.format(Date(time))}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * Offline status card
 */
@Composable
private fun OfflineStatusCard(message: String = "Офлайн режим") {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.offline),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}