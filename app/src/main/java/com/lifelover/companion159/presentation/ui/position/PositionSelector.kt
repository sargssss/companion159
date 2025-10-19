package com.lifelover.companion159.presentation.ui.position

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifelover.companion159.R
import com.lifelover.companion159.presentation.ui.components.SecondaryButton

/**
 * Reusable position selector component
 * Features:
 * - Predefined positions as buttons when field is empty
 * - Autocomplete suggestions when typing (case-insensitive)
 * - Real-time filtering from PREDEFINED_POSITIONS
 */
@Composable
fun PositionSelector(
    positionInput: String,
    onPositionChange: (String) -> Unit,
    suggestions: List<String>,
    isError: Boolean = false
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Position input field
        OutlinedTextField(
            value = positionInput,
            onValueChange = onPositionChange,
            label = { Text(stringResource(R.string.position_label)) },
            placeholder = { Text(stringResource(R.string.position_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = isError
        )

        // Show suggestions when there's input
        if (positionInput.isNotBlank() && suggestions.isNotEmpty()) {
            AutocompleteSuggestionsList(
                suggestions = suggestions,
                onSuggestionClick = onPositionChange
            )
        }

        // Show predefined options when input is empty
        if (positionInput.isBlank()) {
            Text(
                text = stringResource(R.string.position_select_list),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { position ->
                    SecondaryButton(
                        text = position,
                        onClick = { onPositionChange(position) }
                    )
                }
            }
        }
    }
}

/**
 * Dropdown list with autocomplete suggestions
 * Displayed when user types in the position field
 */
@Composable
private fun AutocompleteSuggestionsList(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        LazyColumn {
            items(suggestions) { suggestion ->
                Text(
                    text = suggestion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
                if (suggestion != suggestions.last()) {
                    HorizontalDivider()
                }
            }
        }
    }
}