package com.lifelover.companion159.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelover.companion159.data.sync.SyncService
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.sync.SyncStatus
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.usecases.*
import com.lifelover.companion159.data.repository.InventoryRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryState(
    val items: List<InventoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val lastSyncTime: Long? = null,
    val error: String? = null,
    val message: String? = null,
    val editingItem: InventoryItem? = null
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val getItems: GetInventoryItemsUseCase,
    private val addItem: AddInventoryItemUseCase,
    private val updateItem: UpdateInventoryItemUseCase,
    private val deleteItem: DeleteInventoryItemUseCase,
    private val sync: SyncInventoryUseCase,
    private val syncService: SyncService,
    private val repository: InventoryRepositoryImpl
) : ViewModel() {

    companion object {
        private const val TAG = "InventoryViewModel"
    }

    private val _state = MutableStateFlow(InventoryState())
    val state = _state.asStateFlow()

    init {
        observeSyncStatus()
    }

    private fun observeSyncStatus() {
        viewModelScope.launch {
            syncService.syncStatus.collect { status ->
                _state.update { it.copy(
                    syncStatus = status,
                    isSyncing = status == SyncStatus.SYNCING
                )}
            }
        }

        viewModelScope.launch {
            syncService.lastSyncTime.collect { time ->
                _state.update { it.copy(lastSyncTime = time) }
            }
        }
    }

    fun loadItems(category: InventoryCategory) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            getItems(category).collect { items ->
                _state.value = _state.value.copy(
                    items = items,
                    isLoading = false
                )
            }
        }
    }

    // Updated to include quantity parameter
    fun addNewItem(name: String, quantity: Int, category: InventoryCategory) {
        if (name.isBlank() || quantity <= 0) return

        viewModelScope.launch {
            try {
                Log.d(TAG, "Creating new item: $name with quantity: $quantity")
                val item = InventoryItem(
                    id = 0, // New item always has ID = 0
                    name = name.trim(),
                    category = category,
                    quantity = quantity // Use specified quantity
                )
                addItem(item)
                Log.d(TAG, "New item created: $name")
                _state.value = _state.value.copy(message = "Item added")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add item", e)
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    // Optimistic quantity update - does not block UI
    fun updateQuantity(item: InventoryItem, newQuantity: Int) {
        if (newQuantity < 0) return

        // Launch update without waiting for completion
        viewModelScope.launch {
            try {
                Log.d(TAG, "Optimistic quantity update: ${item.name}, ID: ${item.id}, new quantity: $newQuantity")

                // Key: Do not wait for sync completion - UI remains responsive
                repository.updateItemQuantity(item.id, newQuantity)

                Log.d(TAG, "Quantity update initiated (non-blocking)")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to update quantity", e)
                _state.value = _state.value.copy(error = "Error updating quantity: ${e.message}")
            }
        }
    }

    fun startEditingItem(item: InventoryItem) {
        Log.d(TAG, "Starting to edit item: ${item.name}")
        _state.value = _state.value.copy(editingItem = item)
    }

    fun stopEditingItem() {
        Log.d(TAG, "Stopping item editing")
        _state.value = _state.value.copy(editingItem = null)
    }

    // Optimistic full item update
    fun updateFullItem(item: InventoryItem, newName: String, newQuantity: Int) {
        if (newName.isBlank() || newQuantity < 0) return

        viewModelScope.launch {
            try {
                Log.d(TAG, "Optimistic full update: ${item.name} -> name: $newName, quantity: $newQuantity")

                // Create updated item preserving original ID
                val updatedItem = item.copy(
                    name = newName.trim(),
                    quantity = newQuantity
                )

                // Key: Do not wait for sync completion
                updateItem(updatedItem)
                stopEditingItem()
                _state.value = _state.value.copy(message = "Item updated")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to update full item", e)
                _state.value = _state.value.copy(error = "Update error: ${e.message}")
            }
        }
    }

    // Optimistic deletion
    fun deleteItemById(id: Long) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Optimistic delete for item ID: $id")

                // Key: Do not wait for sync completion
                deleteItem(id)
                _state.value = _state.value.copy(message = "Item deleted")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete item", e)
                _state.value = _state.value.copy(error = "Delete error: ${e.message}")
            }
        }
    }

    // Manual synchronization when user explicitly requests sync
    fun syncData() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Manual sync requested")
                syncService.performSync()
                    .onSuccess {
                        _state.value = _state.value.copy(message = "Sync successful")
                    }
                    .onFailure { error ->
                        _state.value = _state.value.copy(error = "Sync error: ${error.message}")
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Manual sync failed", e)
                _state.value = _state.value.copy(error = "Sync error: ${e.message}")
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}