package com.lifelover.companion159.presentation.ui.inventory.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifelover.companion159.R
import com.lifelover.companion159.domain.models.DisplayCategory


/**
 * Empty state when no items match filter
 */
@Composable
fun EmptyState(
    displayCategory: DisplayCategory,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(48.dp)
        ) {
            // Exhaustive when with sealed class
            val emptyMessage = when (displayCategory) {
                is DisplayCategory.Availability -> stringResource(R.string.empty_availability)
                is DisplayCategory.Ammunition -> stringResource(R.string.empty_ammunition)
                is DisplayCategory.Needs -> stringResource(R.string.empty_needs)
            }

            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            val emptyDescription = when (displayCategory) {
                is DisplayCategory.Needs -> stringResource(R.string.empty_description_needs)
                is DisplayCategory.Availability,
                is DisplayCategory.Ammunition -> stringResource(R.string.empty_description_default)
            }

            Text(
                text = emptyDescription,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onAddClick,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.add_item))
            }
        }
    }
}