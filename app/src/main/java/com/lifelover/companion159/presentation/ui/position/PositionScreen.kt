package com.lifelover.companion159.presentation.ui.position

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifelover.companion159.R
import com.lifelover.companion159.domain.models.toUserMessage
import com.lifelover.companion159.presentation.ui.components.PrimaryButton
import com.lifelover.companion159.presentation.ui.components.SecondaryButton

/**
 * Position setup screen
 *
 * Features:
 * - Position validation
 * - Autocomplete suggestions
 * - Standardized buttons
 * - Full i18n support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PositionScreen(
    onPositionSaved: () -> Unit,
    showBackButton: Boolean = false,
    onBackPressed: (() -> Unit)? = null,
    viewModel: PositionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var positionInput by remember { mutableStateOf(state.currentPosition ?: "") }
    val suggestions = viewModel.getAutocompleteSuggestions(positionInput)

    // Update input when current position changes
    LaunchedEffect(state.currentPosition) {
        if (state.currentPosition != null && positionInput.isEmpty()) {
            positionInput = state.currentPosition!!
        }
    }

    // Navigate after successful save
    LaunchedEffect(state.isPositionSaved) {
        if (state.isPositionSaved) {
            onPositionSaved()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = if (showBackButton)
                        stringResource(R.string.position_change_title)
                    else
                        stringResource(R.string.position_title),
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                if (showBackButton && onBackPressed != null) {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back_button)
                        )
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title and description
            Text(
                text = if (showBackButton)
                    stringResource(R.string.position_update_description)
                else
                    stringResource(R.string.position_description),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "Позиція буде використовуватись для ідентифікації ваших даних",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Position input field
            OutlinedTextField(
                value = positionInput,
                onValueChange = { positionInput = it },
                label = { Text(stringResource(R.string.position_label)) },
                placeholder = { Text(stringResource(R.string.position_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = state.error != null
            )

            // Show suggestions when there's input
            if (positionInput.isNotBlank() && suggestions.isNotEmpty()) {
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
                                    .clickable {
                                        positionInput = suggestion
                                    }
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
                            onClick = { positionInput = position }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Error message
            state.error?.let { error ->
                Text(
                    text = stringResource(error.toUserMessage()),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Save button
            PrimaryButton(
                text = stringResource(R.string.position_save),
                onClick = { viewModel.savePosition(positionInput) },
                enabled = positionInput.isNotBlank() && !state.isLoading,
                icon = Icons.Default.Check,
                loading = state.isLoading
            )
        }
    }
}