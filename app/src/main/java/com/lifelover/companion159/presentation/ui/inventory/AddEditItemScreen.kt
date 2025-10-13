package com.lifelover.companion159.presentation.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.lifelover.companion159.presentation.ui.components.PrimaryButton

/**
 * Screen for adding or editing inventory items
 *
 * Features:
 * - Validates input before saving
 * - Different quantity fields based on category
 * - Standardized buttons
 * - Full i18n support
 */
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = if (isEditMode)
                        stringResource(R.string.edit_item)
                    else
                        stringResource(R.string.add),
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.content_description_back_button)
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
                text = stringResource(displayCategory.titleRes),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            // Name input
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.item_name_label),
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
                    placeholder = { Text(stringResource(R.string.item_name_placeholder)) },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(stringResource(R.string.error_name_required)) }
                    } else null
                )
            }

            // Available quantity for AMMUNITION and AVAILABILITY
            when (displayCategory) {
                is DisplayCategory.Ammunition,
                is DisplayCategory.Availability -> {
                    QuantitySection(
                        title = stringResource(R.string.available_quantity),
                        quantity = availableQty,
                        onQuantityChange = { availableQty = it }
                    )
                }
                is DisplayCategory.Needs -> {
                    // Don't show available quantity for NEEDS
                }
            }

            // Needed quantity for AMMUNITION and NEEDS
            when (displayCategory) {
                is DisplayCategory.Ammunition,
                is DisplayCategory.Needs -> {
                    QuantitySection(
                        title = stringResource(R.string.needed_quantity),
                        quantity = neededQty,
                        onQuantityChange = { neededQty = it }
                    )
                }
                is DisplayCategory.Availability -> {
                    // Don't show needed quantity for AVAILABILITY
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            PrimaryButton(
                text = if (isEditMode)
                    stringResource(R.string.save)
                else
                    stringResource(R.string.add),
                onClick = {
                    if (name.isBlank()) {
                        showError = true
                    } else {
                        onSave(name.trim(), availableQty, neededQty)
                    }
                },
                enabled = name.isNotBlank(),
                icon = Icons.Default.Check
            )
        }
    }
}

/**
 * Reusable quantity input section
 */
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
                        contentDescription = stringResource(R.string.decrease),
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
                        contentDescription = stringResource(R.string.increase),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}