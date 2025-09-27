package com.lifelover.companion159.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelover.companion159.data.InventoryItem
import com.lifelover.companion159.data.repository.LocalInventoryRepository
import com.lifelover.companion159.data.ui.InventoryType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryUiState(
    val items: List<InventoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: LocalInventoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    fun loadItems(category: InventoryType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                repository.getItemsByCategory(category).collect { items ->
                    _uiState.value = _uiState.value.copy(
                        items = items,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    fun addItem(name: String, category: InventoryType) {
        viewModelScope.launch {
            try {
                val item = InventoryItem(
                    name = name,
                    category = category
                )
                repository.addItem(item)

                _uiState.value = _uiState.value.copy(
                    message = "Item додано успішно"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message
                )
            }
        }
    }

    fun updateItemQuantity(item: InventoryItem, newQuantity: Int) {
        viewModelScope.launch {
            try {
                item.quantity.value = newQuantity
                repository.updateItem(item)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message
                )
            }
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteItem(id)
                _uiState.value = _uiState.value.copy(
                    message = "Item видалено"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}