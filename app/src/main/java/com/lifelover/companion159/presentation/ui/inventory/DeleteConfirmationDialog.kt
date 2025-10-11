package com.lifelover.companion159.presentation.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lifelover.companion159.R
import com.lifelover.companion159.domain.models.InventoryItem

@Composable
fun DeleteConfirmationDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Видалити",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Message - FIXED: Use itemName instead of name
                Text(
                    text = "Впевнені що хочете видалити \"${item.itemName}\"?",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Action buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(
                            stringResource(id = R.string.delete),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            stringResource(id = R.string.cancel),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}