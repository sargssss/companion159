package com.lifelover.companion159.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifelover.companion159.R
import com.lifelover.companion159.data.ui.InventoryType
import com.lifelover.companion159.data.ui.iconRes
import com.lifelover.companion159.data.ui.titleRes
import com.lifelover.companion159.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    inventoryType: InventoryType,
    onBackPressed: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    // Load items when screen opens
    LaunchedEffect(inventoryType) {
        viewModel.loadItems(inventoryType)
    }

    // Show messages
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            // Show snackbar or toast
            viewModel.clearMessage()
        }
    }

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error snackbar
            viewModel.clearError()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Toolbar with back button
        TopAppBar(
            title = {
                Text(
                    text = stringResource(inventoryType.titleRes()),
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_left),
                        contentDescription = stringResource(id = R.string.back)
                    )
                }
            },
            actions = {
                IconButton(onClick = { showDialog = true }) {
                    Icon(
                        painter = painterResource(id = R.drawable.plus_circle),
                        contentDescription = stringResource(id = R.string.add)
                    )
                }
            }
        )

        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        // Items list
        else if (uiState.items.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        painter = painterResource(inventoryType.iconRes()),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(id = R.string.empty_list),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(id = R.string.push_plus_to_add),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(uiState.items, key = { it.id }) { item ->
                    InventoryItemCard(
                        item = item,
                        onQuantityChange = { newQuantity ->
                            viewModel.updateItemQuantity(item, newQuantity)
                        },
                        onDelete = {
                            viewModel.deleteItem(item.id)
                        }
                    )
                }
            }
        }
    }

    // Add item dialog
    if (showDialog) {
        AddItemDialog(
            inventoryType = inventoryType,
            onDismiss = { showDialog = false },
            onAdd = { name ->
                viewModel.addItem(name, inventoryType)
                showDialog = false
            }
        )
    }
}