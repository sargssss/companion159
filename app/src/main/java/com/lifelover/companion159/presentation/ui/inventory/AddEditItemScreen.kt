package com.lifelover.companion159.presentation.ui.inventory

import android.util.Log
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
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.domain.models.titleRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemScreen(
    displayCategory: DisplayCategory,
    itemId: Long? = null,
    itemName: String? = null,
    availableQuantity: Int? = null,
    neededQuantity: Int? = null,
    onBackPressed: () -> Unit,
    onSave: (name: String, availableQty: Int, neededQty: Int) -> Unit
) {
    val isEditMode = itemId != null

    var name by remember { mutableStateOf(itemName ?: "") }
    var availableQty by remember { mutableIntStateOf(availableQuantity ?: 0) }
    var neededQty by remember { mutableIntStateOf(neededQuantity ?: 0) }
    var showError by remember { mutableStateOf(false) }

    // Log initial values
    LaunchedEffect(Unit) {
        Log.d("AddEditItemScreen", "═══════════════════════════════════")
        Log.d("AddEditItemScreen", if (isEditMode) "EDIT MODE" else "CREATE MODE")
        Log.d("AddEditItemScreen", "Item ID: $itemId")
        Log.d("AddEditItemScreen", "Initial name: $itemName")
        Log.d("AddEditItemScreen", "Initial available: $availableQuantity")
        Log.d("AddEditItemScreen", "Initial needed: $neededQuantity")
        Log.d("AddEditItemScreen", "Display category: $displayCategory")
        Log.d("AddEditItemScreen", "═══════════════════════════════════")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
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
                IconButton(
                    onClick = {
                        if (name.isBlank()) {
                            showError = true
                        } else {
                            Log.d("AddEditItemScreen", "Save clicked")
                            Log.d("AddEditItemScreen", "   Name: $name")
                            Log.d("AddEditItemScreen", "   Available: $availableQty")
                            Log.d("AddEditItemScreen", "   Needed: $neededQty")
                            onSave(name.trim(), availableQty, neededQty)
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Category header
            Text(
                text = stringResource(displayCategory.titleRes()),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // Name input
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Назва",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Введіть назву") },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("Назва обов'язкова") }
                    } else null
                )
            }

            // CHANGED: Show available quantity for AMMUNITION and AVAILABILITY
            if (displayCategory == DisplayCategory.AMMUNITION ||
                displayCategory == DisplayCategory.AVAILABILITY) {
                QuantitySection(
                    title = "Наявна кількість",
                    quantity = availableQty,
                    onQuantityChange = {
                        Log.d("AddEditItemScreen", "Available quantity changed: $it")
                        availableQty = it
                    }
                )
            }

            // CHANGED: Show needed quantity for AMMUNITION and NEEDS
            if (displayCategory == DisplayCategory.AMMUNITION ||
                displayCategory == DisplayCategory.NEEDS) {
                QuantitySection(
                    title = "Потрібна кількість",
                    quantity = neededQty,
                    onQuantityChange = {
                        Log.d("AddEditItemScreen", "Needed quantity changed: $it")
                        neededQty = it
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = {
                    if (name.isBlank()) {
                        showError = true
                    } else {
                        Log.d("AddEditItemScreen", "Bottom save button clicked")
                        Log.d("AddEditItemScreen", "   Name: $name")
                        Log.d("AddEditItemScreen", "   Available: $availableQty")
                        Log.d("AddEditItemScreen", "   Needed: $neededQty")
                        onSave(name.trim(), availableQty, neededQty)
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditMode) "Зберегти" else "Додати",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun QuantitySection(
    title: String,
    quantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
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
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = { if (quantity > 0) onQuantityChange(quantity - 1) },
                    enabled = quantity > 0,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.minus),
                        contentDescription = "Зменшити",
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                OutlinedTextField(
                    value = quantity.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { newQty ->
                            if (newQty >= 0) onQuantityChange(newQty)
                        }
                    },
                    modifier = Modifier.width(100.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.width(16.dp))

                FilledIconButton(
                    onClick = { onQuantityChange(quantity + 1) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.plus_large),
                        contentDescription = "Збільшити",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}