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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

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
                    text = if (showBackButton) "Змінити позицію" else "Встановіть вашу позицію",
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = if (showBackButton) {
                {
                    IconButton(onClick = { onBackPressed?.invoke() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            } else null
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
                    "Оновіть вашу позицію"
                else
                    "Для початку роботи вкажіть вашу позицію",
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
                label = { Text("Позиція") },
                placeholder = { Text("Введіть або оберіть позицію") },
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
                    text = "Або оберіть зі списку:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.forEach { position ->
                        OutlinedButton(
                            onClick = { positionInput = position },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(position)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Error message
            state.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Save button
            Button(
                onClick = {
                    viewModel.savePosition(positionInput)
                },
                enabled = positionInput.isNotBlank() && !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Зберегти позицію")
                }
            }
        }
    }
}