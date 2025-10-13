package com.lifelover.companion159.presentation.ui.inventory.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifelover.companion159.R

/**
 * Item header with name and sync indicator
 * Reusable component
 */
@Composable
fun ItemHeader(
    name: String,
    isSynced: Boolean,
    showSyncStatus: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        if (showSyncStatus && !isSynced) {
            Icon(
                painter = painterResource(R.drawable.sync_attention),
                contentDescription = "Не синхронізовано",
                modifier = Modifier
                    .size(16.dp)
                    .padding(start = 4.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}