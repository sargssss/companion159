package com.lifelover.companion159.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifelover.companion159.data.sync.SyncStatus // Використовуємо правильний SyncStatus

@Composable
fun SyncStatusIndicator(
    status: SyncStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        when (status) {
            SyncStatus.SUCCESS -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Синхронізовано",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Синхронізовано",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            SyncStatus.ERROR -> {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Не синхронізовано",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Не синхронізовано",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            SyncStatus.SYNCING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Синхронізація...",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            SyncStatus.OFFLINE -> {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Offline",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            SyncStatus.IDLE -> {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Готовий до синхронізації",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "Готовий",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}