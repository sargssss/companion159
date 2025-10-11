package com.lifelover.companion159.presentation.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import com.lifelover.companion159.R
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.local.entities.iconRes
import com.lifelover.companion159.data.local.entities.titleRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemScreen(
    category: InventoryCategory,
    itemId: Long? = null, // null = create mode, non-null = edit mode
    itemName: String? = null,
    itemQuantity: Int? = null,
    onBackPressed: () -> Unit,
    onSave: (name: String, quantity: Int) -> Unit
) {
    // Determine mode
    val isEditMode = itemId != null

    // State
    var name by remember { mutableStateOf(itemName ?: "") }
    var quantity by remember { mutableIntStateOf(itemQuantity ?: 1) }
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding() // Adjust for keyboard
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = if (isEditMode) "Редагувати" else stringResource(id = R.string.add),
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(id = R.string.back)
                    )
                }
            },
            actions = {
                // Save button in app bar
                IconButton(
                    onClick = {
                        if (name.isBlank()) {
                            showError = true
                        } else {
                            onSave(name.trim(), quantity)
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Зберегти",
                        tint = if (name.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Category header with large icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(category.iconRes()),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(category.titleRes()),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Name input section
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Назва",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Введіть назву предмету") },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("Назва не може бути порожньою") }
                    } else null,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = MaterialTheme.shapes.large
                )
            }

            // Quantity section with large controls
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Кількість",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Decrease button
                        FilledIconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            enabled = quantity > 1,
                            modifier = Modifier.size(64.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.minus),
                                contentDescription = "Зменшити",
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(24.dp))

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
                            modifier = Modifier.width(120.dp),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = MaterialTheme.shapes.large
                        )

                        Spacer(modifier = Modifier.width(24.dp))

                        // Increase button
                        FilledIconButton(
                            onClick = { quantity++ },
                            modifier = Modifier.size(64.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.plus_large),
                                contentDescription = "Збільшити",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Large save button at bottom
            Button(
                onClick = {
                    if (name.isBlank()) {
                        showError = true
                    } else {
                        onSave(name.trim(), quantity)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                enabled = name.isNotBlank(),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isEditMode) "Зберегти зміни" else "Додати предмет",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
    }
}