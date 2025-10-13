package com.lifelover.companion159.presentation.ui.inventory.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifelover.companion159.R

/**
 * Quantity controls with increment/decrement buttons
 * Reusable component for inline quantity editing
 */
@Composable
fun QuantityControls(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        IconButton(
            onClick = {
                if (quantity > 0) {
                    onQuantityChange(quantity - 1)
                }
            },
            enabled = enabled && quantity > 0,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.minus_circle),
                contentDescription = stringResource(R.string.decrease)
            )
        }

        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.widthIn(min = 32.dp),
            textAlign = TextAlign.Center
        )

        IconButton(
            onClick = { onQuantityChange(quantity + 1) },
            enabled = enabled,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.plus_circle),
                contentDescription = stringResource(R.string.increase)
            )
        }
    }
}

/**
 * Display row for available quantity with inline controls
 */
@Composable
fun AvailableQuantityRow(
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.available_quantity) + ":",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        QuantityControls(
            quantity = quantity,
            onQuantityChange = onQuantityChange
        )
    }
}

/**
 * Display row for needed quantity (read-only)
 * Only shown if quantity > 0
 */
@Composable
fun NeededQuantityRow(
    quantity: Int,
    modifier: Modifier = Modifier
) {
    // Only show if quantity > 0
    if (quantity > 0) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.needed_quantity) + ":",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}