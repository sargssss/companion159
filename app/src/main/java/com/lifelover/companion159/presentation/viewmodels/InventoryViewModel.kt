package com.lifelover.companion159.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.repository.SyncResult
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.usecases.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryState(
    val items: List<InventoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val getItems: GetInventoryItemsUseCase,
    private val addItem: AddInventoryItemUseCase,
    private val updateItem: UpdateInventoryItemUseCase,
    private val deleteItem: DeleteInventoryItemUseCase,
    private val sync: SyncInventoryUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryState())
    val state = _state.asStateFlow()

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
                val item = InventoryItem(name = name.trim(), category = category)
                addItem(item)
                _state.value = _state.value.copy(message = "Item added")
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun updateQuantity(item: InventoryItem, newQuantity: Int) {
        if (newQuantity < 0) return

        viewModelScope.launch {
            try {
                val updatedItem = item.copy(quantity = newQuantity)
                updateItem(updatedItem)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun deleteItemById(id: Long) {
        viewModelScope.launch {
            try {
                deleteItem(id)
                _state.value = _state.value.copy(message = "Item deleted")
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun syncData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSyncing = true)
            when (val result = sync()) {
                is SyncResult.Success -> {
                    _state.value = _state.value.copy(
                        isSyncing = false,
                        message = "Sync completed"
                    )
                }
                is SyncResult.Error -> {
                    _state.value = _state.value.copy(
                        isSyncing = false,
                        error = result.message
                    )
                }
                is SyncResult.NetworkError -> {
                    _state.value = _state.value.copy(
                        isSyncing = false,
                        error = "No internet connection"
                    )
                }
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