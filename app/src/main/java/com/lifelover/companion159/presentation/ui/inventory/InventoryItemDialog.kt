package com.lifelover.companion159.presentation.ui.inventory


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lifelover.companion159.R
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.local.entities.iconRes
import com.lifelover.companion159.domain.models.InventoryItem

@Composable
fun InventoryItemDialog(
    inventoryType: InventoryCategory,
    editingItem: InventoryItem? = null, // null for create, item for edit
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    // Determine if this is edit mode
    val isEditMode = editingItem != null

    // Initialize values based on mode
    var itemName by remember { mutableStateOf(editingItem?.name ?: "") }
    var quantity by remember { mutableIntStateOf(editingItem?.quantity ?: 1) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header with icon and title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        modifier = Modifier.size(32.dp),
                        painter = painterResource(inventoryType.iconRes()),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isEditMode) "Edit Item" else stringResource(id = R.string.add),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Name input field
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text(stringResource(id = R.string.naming)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(id = R.string.enter_title)) },
                    shape = RoundedCornerShape(12.dp)
                )

                // Quantity section
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Quantity",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Decrease button
                        IconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            enabled = quantity > 1
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.minus),
                                contentDescription = "Decrease quantity",
                                tint = if (quantity > 1) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                        }

                        // Quantity display/input
                        OutlinedTextField(
                            value = quantity.toString(),
                            onValueChange = { value ->
                                value.toIntOrNull()?.let { newQuantity ->
                                    if (newQuantity >= 1) {
                                        quantity = newQuantity
                                    }
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Increase button
                        IconButton(
                            onClick = { quantity++ }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.plus_large),
                                contentDescription = "Increase quantity",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(id = R.string.cancel))
                    }

                    Button(
                        onClick = {
                            if (itemName.isNotBlank()) {
                                onSave(itemName.trim(), quantity)
                            }
                        },
                        enabled = itemName.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isEditMode) "Save" else stringResource(id = R.string.add))
                    }
                }
            }
        }
    }
}

// Helper composables for backward compatibility
@Composable
fun AddItemDialog(
    inventoryType: InventoryCategory,
    onDismiss: () -> Unit,
    onAdd: (String, Int) -> Unit
) {
    InventoryItemDialog(
        inventoryType = inventoryType,
        editingItem = null, // null means create mode
        onDismiss = onDismiss,
        onSave = onAdd
    )
}

@Composable
fun EditItemDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onUpdate: (String, Int) -> Unit
) {
    InventoryItemDialog(
        inventoryType = item.category,
        editingItem = item, // pass item for edit mode
        onDismiss = onDismiss,
        onSave = onUpdate
    )
}