package com.lifelover.companion159.presentation.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lifelover.companion159.R
import com.lifelover.companion159.data.sync.SyncStatus // Правильний імпорт
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.presentation.ui.auth.AuthViewModel
import com.lifelover.companion159.presentation.ui.inventory.InventoryMenuButton
import com.lifelover.companion159.presentation.viewmodels.InventoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    onInventoryTypeSelected: (InventoryCategory) -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
    inventoryViewModel: InventoryViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsState()
    val inventoryState by inventoryViewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(id = R.string.in_stock),
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                // Індикатор синхронізації
                when (inventoryState.syncStatus) {
                    SyncStatus.SYNCING -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 12.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    SyncStatus.SUCCESS -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Синхронізовано",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                    SyncStatus.ERROR -> {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Помилка синхронізації",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                    else -> {}
                }

                // Кнопка синхронізації
                if (authState.isAuthenticated) {
                    IconButton(
                        onClick = { inventoryViewModel.syncData() },
                        enabled = inventoryState.syncStatus != SyncStatus.SYNCING
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Синхронізувати"
                        )
                    }
                }

                // Меню користувача
                if (authState.isAuthenticated) {
                    IconButton(onClick = { /* TODO: Show user menu */ }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Користувач"
                        )
                    }
                }
            }
        )

        // Статус бар
        if (authState.isAuthenticated) {
            StatusCard(
                userEmail = authState.userEmail,
                lastSyncTime = inventoryState.lastSyncTime
            )
        } else {
            OfflineStatusCard()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Menu buttons for each category
            InventoryCategory.values().forEach { category ->
                InventoryMenuButton(
                    inventoryType = category,
                    onClick = { onInventoryTypeSelected(category) }
                )
            }
        }

        // Повідомлення про помилку
        inventoryState.error?.let { error ->
            LaunchedEffect(error) {
                // Показуємо Snackbar або Toast тут
            }
        }
    }
}

@Composable
private fun StatusCard(
    userEmail: String?,
    lastSyncTime: Long?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Користувач: ${userEmail ?: "Невідомий"}",
                style = MaterialTheme.typography.bodyMedium
            )

            lastSyncTime?.let { time ->
                val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                Text(
                    text = "Синхр.: ${formatter.format(Date(time))}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun OfflineStatusCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Офлайн режим",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}