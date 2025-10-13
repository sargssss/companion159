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
import androidx.compose.ui.unit.dp
import com.lifelover.companion159.R
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.presentation.ui.inventory.components.AvailableQuantityRow
import com.lifelover.companion159.presentation.ui.inventory.components.NeededQuantityRow
import com.lifelover.companion159.presentation.ui.inventory.components.QuantityControls

/**
 * Card component for displaying inventory item
 *
 * Features:
 * - Shows different quantities based on display category
 * - Availability: shows available quantity + needed (if > 0)
 * - Ammunition: shows available quantity + needed (if > 0)
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

            // Quantity display based on category
            when (displayCategory) {
                is DisplayCategory.Availability,
                is DisplayCategory.Ammunition -> {
                    AvailabilityOrAmmunitionLayout(
                        item = item,
                        onQuantityChange = onQuantityChange,
                        onEdit = onEdit,
                        onDelete = onDelete
                    )
                }
                is DisplayCategory.Needs -> {
                    NeedsLayout(
                        item = item,
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
 * Layout for Availability and Ammunition categories
 * Shows available quantity with controls + needed quantity if > 0
 */
@Composable
private fun AvailabilityOrAmmunitionLayout(
    item: InventoryItem,
    onQuantityChange: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Available quantity with controls
        AvailableQuantityRow(
            quantity = item.availableQuantity,
            onQuantityChange = onQuantityChange
        )

        // Needed quantity (only if > 0)
        NeededQuantityRow(quantity = item.neededQuantity)

        // Action buttons
        ActionButtons(
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}

/**
 * Layout for Needs category
 * Shows only needed quantity with controls
 */
@Composable
private fun NeedsLayout(
    item: InventoryItem,
    onQuantityChange: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Needed quantity controls
        QuantityControls(
            quantity = item.neededQuantity,
            onQuantityChange = onQuantityChange
        )

        // Action buttons
        ActionButtons(
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}

/**
 * Reusable action buttons component
 */
@Composable
private fun ActionButtons(
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
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