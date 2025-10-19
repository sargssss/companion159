package com.lifelover.companion159.presentation.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifelover.companion159.R
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.presentation.ui.inventory.components.EmptyState
import com.lifelover.companion159.presentation.viewmodels.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    displayCategory: DisplayCategory,
    onBackPressed: () -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (InventoryItem) -> Unit,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var itemToDelete by remember { mutableStateOf<InventoryItem?>(null) }

    // Load items when screen opens
    LaunchedEffect(displayCategory) {
        viewModel.loadItems(displayCategory)
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

    // Delete confirmation dialog
    itemToDelete?.let { item ->
        DeleteConfirmationDialog(
            item = item,
            onDismiss = { itemToDelete = null },
            onConfirm = {
                viewModel.deleteItem(item, displayCategory)
                itemToDelete = null
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(displayCategory.titleRes),
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
            },
            actions = {
                IconButton(onClick = onAddItem) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.content_description_add_button)
                    )
                }
            }
        )

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
                    displayCategory = displayCategory,
                    onAddClick = onAddItem
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
                            displayCategory = displayCategory,
                            onQuantityChange = { newQuantity ->
                                viewModel.updateQuantity(item.id, newQuantity, displayCategory)
                            },
                            onDelete = { itemToDelete = item },
                            onEdit = { onEditItem(item) },
                            showSyncStatus = !item.isSynced
                        )
                    }
                }
            }
        }
    }
}