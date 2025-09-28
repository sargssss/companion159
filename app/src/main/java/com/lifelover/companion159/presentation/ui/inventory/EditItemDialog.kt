package com.lifelover.companion159.presentation.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.lifelover.companion159.R
import com.lifelover.companion159.data.local.entities.iconRes
import com.lifelover.companion159.domain.models.InventoryItem

@Composable
fun EditItemDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onUpdate: (String, Int) -> Unit
) {
    var itemName by remember { mutableStateOf(item.name) }
    var itemQuantity by remember { mutableStateOf(item.quantity.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(item.category.iconRes()),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Редагувати елемент",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text(stringResource(id = R.string.naming)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(id = R.string.enter_title)) }
                )

                OutlinedTextField(
                    value = itemQuantity,
                    onValueChange = { value ->
                        // Дозволяємо тільки числа
                        if (value.isEmpty() || value.all { it.isDigit() }) {
                            itemQuantity = value
                        }
                    },
                    label = { Text("Кількість") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("0") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(id = R.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val quantity = itemQuantity.toIntOrNull() ?: 0
                            if (itemName.isNotBlank() && quantity >= 0) {
                                onUpdate(itemName.trim(), quantity)
                            }
                        },
                        enabled = itemName.isNotBlank() &&
                                itemQuantity.isNotEmpty() &&
                                (itemQuantity.toIntOrNull() ?: -1) >= 0
                    ) {
                        Text("Зберегти")
                    }
                }
            }
        }
    }
}