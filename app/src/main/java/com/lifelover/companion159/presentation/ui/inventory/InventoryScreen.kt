package com.lifelover.companion159.presentation.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.local.entities.iconRes
import com.lifelover.companion159.data.local.entities.titleRes
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.presentation.viewmodels.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    inventoryCategory: InventoryCategory,
    onBackPressed: () -> Unit,
    onAddItem: () -> Unit, // NEW: callback for navigation
    onEditItem: (InventoryItem) -> Unit, // NEW: callback for navigation
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var itemToDelete by remember { mutableStateOf<InventoryItem?>(null) }

    // Load items when screen opens
    LaunchedEffect(inventoryCategory) {
        viewModel.loadItems(inventoryCategory)
    }

    // Handle messages
    state.message?.let { message ->
        LaunchedEffect(message) {
            viewModel.clearMessage()
        }
    }

    state.error?.let { error ->
        LaunchedEffect(error) {
            viewModel.clearError()
        }
    }

    // Delete confirmation dialog (only this dialog remains)
    itemToDelete?.let { item ->
        DeleteConfirmationDialog(
            item = item,
            onDismiss = { itemToDelete = null },
            onConfirm = {
                viewModel.deleteItemById(item.id)
                itemToDelete = null
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(inventoryCategory.titleRes()),
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
                IconButton(onClick = onAddItem) { // CHANGED: navigate instead of dialog
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.add)
                    )
                }
            }
        )

        // Content
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.items.isEmpty() -> {
                EmptyState(
                    category = inventoryCategory,
                    onAddClick = onAddItem // CHANGED: navigate instead of dialog
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        InventoryItemCard(
                            item = item,
                            onQuantityChange = { newQuantity ->
                                viewModel.updateQuantity(item, newQuantity)
                            },
                            onDelete = { itemToDelete = item },
                            onEdit = { onEditItem(item) }, // CHANGED: navigate instead of dialog
                            showSyncStatus = !item.isSynced
                        )
                    }
                }
            }
        }
    }

    // REMOVED: All dialog code (showAddDialog, InventoryItemDialog, etc.)
}

@Composable
private fun EmptyState(
    category: InventoryCategory,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(48.dp)
        ) {
            Icon(
                painter = painterResource(category.iconRes()),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )

            Text(
                text = stringResource(id = R.string.empty_list),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = stringResource(id = R.string.push_plus_to_add),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onAddClick,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Додати перший предмет")
            }
        }
    }
}