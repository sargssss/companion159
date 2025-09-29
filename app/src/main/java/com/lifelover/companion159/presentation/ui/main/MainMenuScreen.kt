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
    onLogout: () -> Unit = {}, // Callback for logout navigation
    authViewModel: AuthViewModel = hiltViewModel(),
    inventoryViewModel: InventoryViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsState()
    val inventoryState by inventoryViewModel.state.collectAsState()

    // State for user menu
    var showUserMenu by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Observe authentication state and navigate on logout
    LaunchedEffect(authState.isAuthenticated) {
        if (!authState.isAuthenticated && showLogoutDialog) {
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

                if (authState.isAuthenticated) {
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

                    // User menu with dropdown
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
                    // Offline mode - show login button
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Увійти"
                        )
                    }
                }
            }
        )

        if (authState.isAuthenticated) {
            StatusCard(
                userEmail = authState.userEmail,
                lastSyncTime = inventoryState.lastSyncTime
            )
        } else {
            OfflineStatusCard()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Menu buttons for each category
            InventoryCategory.values().forEach { category ->
                InventoryMenuButton(
                    inventoryType = category,
                    onClick = { onInventoryTypeSelected(category) }
                )
            }
        }

        inventoryState.error?.let { error ->
            LaunchedEffect(error) {
                // Показуємо Snackbar або Toast тут
            }
        }
    }

    // Діалог підтвердження виходу
    if (showLogoutDialog) {
        LogoutConfirmationDialog(
            userEmail = authState.userEmail,
            onDismiss = { showLogoutDialog = false },
            onConfirm = {
                showLogoutDialog = false
                authViewModel.signOut()
                onLogout()
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