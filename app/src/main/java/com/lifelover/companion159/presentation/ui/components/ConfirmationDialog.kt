package com.lifelover.companion159.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Reusable confirmation dialog
 *
 * Generic component for any confirmation action
 * Can be used for logout, delete, or any other confirmation
 *
 * Example usage:
 * ```
 * ConfirmationDialog(
 *     title = "Вийти з аккаунту?",
 *     message = "Ви впевнені?",
 *     confirmText = "Вийти",
 *     onConfirm = { ... },
 *     onDismiss = { ... }
 * )
 * ```
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String = "Скасувати",
    icon: ImageVector? = null,
    destructive: Boolean = false,
    subtitle: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = icon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = if (destructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        },
        title = { Text(title) },
        text = {
            Column {
                Text(message)
                subtitle?.let { sub ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = if (destructive) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

/**
 * Specialized logout confirmation dialog
 * Pre-configured for logout action
 */
@Composable
fun LogoutConfirmationDialog(
    userEmail: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    ConfirmationDialog(
        title = "Вийти з облікового запису?",
        message = "Ви впевнені, що хочете вийти?",
        subtitle = userEmail ?: "Невідомий користувач",
        confirmText = "Вийти",
        icon = Icons.AutoMirrored.Filled.ExitToApp,
        destructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}