package com.lifelover.companion159.presentation.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifelover.companion159.R
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.domain.models.InventoryItem

/**
 * Card component for displaying inventory item
 *
 * - Shows different quantities based on display category
 * - Availability: shows both available and needed quantities
 * - Ammunition: shows both available and needed quantities
 * - Needs: shows only needed quantity
 * - Inline quantity controls
 * - Edit and delete actions
 */
@Composable
fun InventoryItemCard(
    item: InventoryItem,
    displayCategory: DisplayCategory,
    onQuantityChange: (Int) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    showSyncStatus: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Name section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.itemName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                if (showSyncStatus && !item.isSynced) {
                    Icon(
                        painter = painterResource(R.drawable.sync_attention),
                        contentDescription = stringResource(R.string.not_synced),
                        modifier = Modifier
                            .size(16.dp)
                            .padding(start = 4.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quantity controls and display based on category
            when (displayCategory) {
                is DisplayCategory.Availability -> {
                    // Show both available and needed quantities
                    AvailabilityQuantityControls(
                        item = item,
                        onQuantityChange = onQuantityChange,
                        onEdit = onEdit,
                        onDelete = onDelete
                    )
                }
                is DisplayCategory.Ammunition,
                is DisplayCategory.Needs -> {
                    // Show single quantity (available for Ammunition, needed for Needs)
                    SingleQuantityControls(
                        displayedQuantity = when (displayCategory) {
                            is DisplayCategory.Needs -> item.neededQuantity
                            else -> item.availableQuantity
                        },
                        onQuantityChange = onQuantityChange,
                        onEdit = onEdit,
                        onDelete = onDelete
                    )
                }
            }
        }
    }
}

/**
 * Quantity controls for Availability category
 * Shows both available and needed quantities in compact format
 */
@Composable
private fun AvailabilityQuantityControls(
    item: InventoryItem,
    onQuantityChange: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Available quantity row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.available_quantity) + ":",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (item.availableQuantity > 0) {
                            onQuantityChange(item.availableQuantity - 1)
                        }
                    },
                    enabled = item.availableQuantity > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.minus_circle),
                        contentDescription = stringResource(R.string.decrease)
                    )
                }

                Text(
                    text = item.availableQuantity.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.widthIn(min = 32.dp),
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = { onQuantityChange(item.availableQuantity + 1) },
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

        // Needed quantity display (read-only, edit to change)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.needed_quantity) + ":",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = item.neededQuantity.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (item.neededQuantity > 0)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Single quantity controls for Ammunition and Needs categories
 */
@Composable
private fun SingleQuantityControls(
    displayedQuantity: Int,
    onQuantityChange: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Quantity controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (displayedQuantity > 0) {
                        onQuantityChange(displayedQuantity - 1)
                    }
                },
                enabled = displayedQuantity > 0,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.minus_circle),
                    contentDescription = stringResource(R.string.decrease)
                )
            }

            Text(
                text = displayedQuantity.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.widthIn(min = 32.dp),
                textAlign = TextAlign.Center
            )

            IconButton(
                onClick = { onQuantityChange(displayedQuantity + 1) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.plus_circle),
                    contentDescription = stringResource(R.string.increase)
                )
            }
        }

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}