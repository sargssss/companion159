package com.lifelover.companion159.presentation.ui.inventory.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifelover.companion159.R
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

/**
 * Reusable quantity input section with proper debounced changes
 *
 * Features:
 * - Increment/decrement buttons for quick adjustments
 * - Direct text input for precise quantity entry
 * - Input validation (0-999999 range)
 * - Debounced callbacks (500ms) that cancels previous pending calls
 *
 * Debounce logic:
 * - User changes trigger local state update immediately (responsive UI)
 * - Actual callback fires only after 500ms of inactivity
 * - If user changes value again within 500ms → previous callback is cancelled
 * - New callback scheduled for the updated value
 * - Prevents multiple rapid sync operations and network requests
 *
 * Example:
 * - User types: 1 → 10 → 100 (within 500ms each)
 * - Only one callback fires with final value (100) after 500ms of no changes
 * - NOT three separate callbacks for each intermediate value
 */
@OptIn(FlowPreview::class)
@Composable
fun QuantitySection(
    title: String,
    quantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    // Local state for immediate UI updates (responsive input)
    var localQuantity by remember { mutableIntStateOf(quantity) }

    // Get latest callback to use in LaunchedEffect
    val latestOnQuantityChange by rememberUpdatedState(onQuantityChange)

    // Debounce logic: observe local quantity changes with 500ms debounce
    // Previous pending calls are automatically cancelled when new value arrives
    LaunchedEffect(Unit) {
        snapshotFlow { localQuantity }
            .debounce(500L) // Wait 500ms of inactivity before emitting
            .collect { debouncedValue ->
                latestOnQuantityChange(debouncedValue)
            }
    }

    // Sync external quantity changes to local state
    LaunchedEffect(quantity) {
        if (localQuantity != quantity) {
            localQuantity = quantity
        }
    }

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
                // Decrement button
                FilledIconButton(
                    onClick = { if (localQuantity > 0) localQuantity-- },
                    enabled = localQuantity > 0,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.minus),
                        contentDescription = stringResource(R.string.decrease),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Text input with validation
                OutlinedTextField(
                    value = localQuantity.toString(),
                    onValueChange = { value ->
                        if (value.isEmpty()) {
                            // Allow empty input (user may continue typing)
                            localQuantity = 0
                        } else {
                            // Parse and validate input
                            value.toIntOrNull()?.let { newQty ->
                                if (newQty in 0..999999) {
                                    // Valid range - update local state
                                    localQuantity = newQty
                                }
                                // Out of range - silently ignore (no UI update)
                            }
                        }
                    },
                    modifier = Modifier.width(100.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = localQuantity < 0 || localQuantity > 999999
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Increment button
                FilledIconButton(
                    onClick = {
                        if (localQuantity < 999999) localQuantity++
                    },
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