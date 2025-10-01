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
import com.lifelover.companion159.data.sync.SyncStatus
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.presentation.ui.auth.AuthViewModel
import com.lifelover.companion159.presentation.ui.inventory.InventoryMenuButton
import com.lifelover.companion159.presentation.viewmodels.InventoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    onInventoryTypeSelected: (InventoryCategory) -> Unit = {},
    onLogout: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
    inventoryViewModel: InventoryViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsState()
    val inventoryState by inventoryViewModel.state.collectAsState()

    // State for user menu
    var showUserMenu by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // FIXED: Only navigate to login if user explicitly logged out
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
                // Sync status indicator
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

                // Show sync button only when authenticated AND online
                if (authState.isAuthenticated && !authState.isOffline) {
                    IconButton(
                        onClick = { inventoryViewModel.syncData() },
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

                // User menu - show for both authenticated and unauthenticated
                if (authState.isAuthenticated) {
                    // Authenticated user menu
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
                            // Settings
                            DropdownMenuItem(
                                text = { Text("Налаштування") },
                                onClick = {
                                    showUserMenu = false
                                    // TODO: Navigate to settings
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null
                                    )
                                }
                            )

                            HorizontalDivider()

                            // Logout
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
                    // Not authenticated - show login button
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Увійти"
                        )
                    }
                }
            }
        )

        // Status card - show different info based on auth and network status
        when {
            authState.isAuthenticated && !authState.isOffline -> {
                // Online with account
                StatusCard(
                    userEmail = authState.userEmail,
                    lastSyncTime = inventoryState.lastSyncTime,
                    isOffline = false
                )
            }
            authState.isAuthenticated && authState.isOffline -> {
                // Offline with account
                StatusCard(
                    userEmail = authState.userEmail,
                    lastSyncTime = inventoryState.lastSyncTime,
                    isOffline = true
                )
            }
            !authState.isAuthenticated && authState.isOffline -> {
                // Offline without account
                OfflineStatusCard(message = "Офлайн режим (без аккаунта)")
            }
            else -> {
                // Online without account
                OfflineStatusCard(message = "Режим без аккаунта")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Menu buttons for each category
            InventoryCategory.entries.forEach { category ->
                InventoryMenuButton(
                    inventoryType = category,
                    onClick = { onInventoryTypeSelected(category) }
                )
            }
        }

        inventoryState.error?.let { error ->
            LaunchedEffect(error) {
                // Show Snackbar or Toast here
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Несинхронізовані зміни будуть збережені локально.",
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

@Composable
private fun StatusCard(
    userEmail: String?,
    lastSyncTime: Long?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Користувач: ${userEmail ?: "Невідомий"}",
                style = MaterialTheme.typography.bodyMedium
            )

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

@Composable
private fun OfflineStatusCard() {
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
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Офлайн режим",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}