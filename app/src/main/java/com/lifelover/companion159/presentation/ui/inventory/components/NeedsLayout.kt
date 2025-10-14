package com.lifelover.companion159.presentation.ui.inventory.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.lifelover.companion159.domain.models.InventoryItem


/**
 * Layout for Needs category
 * Shows only needed quantity with controls
 */
@Composable
fun NeedsLayout(
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
        ItemCardActionButtons(
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}