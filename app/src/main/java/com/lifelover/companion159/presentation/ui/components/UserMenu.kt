package com.lifelover.companion159.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

/**
 * Reusable user menu component
 *
 * Features:
 * - Shows user email
 * - Change position action
 * - Settings action
 * - Logout action
 *
 * Can be used in any screen with authenticated user
 */
@Composable
fun UserMenu(
    userEmail: String?,
    isAuthenticated: Boolean,
    onChangePosition: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    if (isAuthenticated) {
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "User menu"
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                // User email
                userEmail?.let { email ->
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
                        showMenu = false
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

                // Settings
                DropdownMenuItem(
                    text = { Text("Налаштування") },
                    onClick = {
                        showMenu = false
                        onSettings()
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
                        showMenu = false
                        onLogout()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
            }
        }
    } else {
        // Login button for unauthenticated users
        IconButton(onClick = onLogout) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Увійти"
            )
        }
    }
}