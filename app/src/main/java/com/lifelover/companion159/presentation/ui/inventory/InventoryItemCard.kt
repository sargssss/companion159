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

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    displayCategory: DisplayCategory,
    onQuantityChange: (Int) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    showSyncStatus: Boolean = false
) {
    // Determine which quantity to display based on category
    val displayedQuantity = when (displayCategory) {
        DisplayCategory.NEEDS -> item.neededQuantity
        else -> item.availableQuantity
    }

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
                        contentDescription = "Не синхронізовано",
                        modifier = Modifier
                            .size(16.dp)
                            .padding(start = 4.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Controls section
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
                            contentDescription = "Зменшити"
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
                            contentDescription = "Збільшити"
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
                            contentDescription = "Редагувати",
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
                            contentDescription = stringResource(id = R.string.delete),
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}