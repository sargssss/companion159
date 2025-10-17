package com.lifelover.companion159.presentation.ui.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.runtime.Composable
import com.lifelover.companion159.presentation.ui.components.ConfirmationDialog

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