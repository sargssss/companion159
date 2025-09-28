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
    private val repository: InventoryRepositoryImpl // Додаємо для спеціальних методів
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

    fun addNewItem(name: String, category: InventoryCategory) {
        if (name.isBlank()) return

        viewModelScope.launch {
            try {
                val item = InventoryItem(
                    name = name.trim(),
                    category = category,
                    quantity = 1
                )
                addItem(item)
                Log.d(TAG, "Added new item: $name")
                _state.value = _state.value.copy(message = "Елемент додано")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add item", e)
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun updateQuantity(item: InventoryItem, newQuantity: Int) {
        if (newQuantity < 0) return

        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating quantity for item: ${item.name}, ID: ${item.id}, new quantity: $newQuantity")

                // Використовуємо спеціальний метод для оновлення кількості
                repository.updateItemQuantity(item.id, newQuantity)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to update item quantity", e)
                _state.value = _state.value.copy(error = "Помилка оновлення кількості: ${e.message}")
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

    fun updateItemName(item: InventoryItem, newName: String) {
        if (newName.isBlank()) return

        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating name for item: ${item.name} -> $newName")

                repository.updateItemName(item.id, newName.trim())
                stopEditingItem()
                _state.value = _state.value.copy(message = "Назву оновлено")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to update item name", e)
                _state.value = _state.value.copy(error = "Помилка оновлення назви: ${e.message}")
            }
        }
    }

    fun updateFullItem(item: InventoryItem, newName: String, newQuantity: Int) {
        if (newName.isBlank() || newQuantity < 0) return

        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating full item: ${item.name} -> name: $newName, quantity: $newQuantity")

                val updatedItem = item.copy(
                    name = newName.trim(),
                    quantity = newQuantity,
                    lastModified = java.util.Date()
                )

                updateItem(updatedItem)
                stopEditingItem()
                _state.value = _state.value.copy(message = "Елемент оновлено")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to update full item", e)
                _state.value = _state.value.copy(error = "Помилка оновлення: ${e.message}")
            }
        }
    }

    fun deleteItemById(id: Long) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Deleting item with ID: $id")
                deleteItem(id)
                _state.value = _state.value.copy(message = "Елемент видалено")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete item", e)
                _state.value = _state.value.copy(error = "Помилка видалення: ${e.message}")
            }
        }
    }

    fun syncData() {
        viewModelScope.launch {
            syncService.performSync()
                .onSuccess {
                    _state.value = _state.value.copy(message = "Синхронізація успішна")
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(error = "Помилка синхронізації: ${error.message}")
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