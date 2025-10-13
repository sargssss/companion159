package com.lifelover.companion159.presentation.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifelover.companion159.R
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.presentation.viewmodels.AuthViewModel

/**
 * Main menu screen with category buttons
 * Simplified: removed sync UI (not functional)
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
                // User menu (only if authenticated)
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
                            // User email
                            authState.userEmail?.let { email ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    onClick = {},
                                    enabled = false
                                )
                                HorizontalDivider()
                            }

                            // Change position
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

                            // Settings (placeholder)
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
                    // Login button (if not authenticated)
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Увійти"
                        )
                    }
                }
            }
        )

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