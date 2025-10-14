package com.lifelover.companion159.presentation.ui.inventory.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.lifelover.companion159.domain.models.InventoryItem

/**
 * Layout for Availability and Ammunition categories
 * Shows available quantity with controls + needed quantity if > 0
 */
@Composable
fun AvailabilityOrAmmunitionLayout(
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
        ItemCardActionButtons(
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}