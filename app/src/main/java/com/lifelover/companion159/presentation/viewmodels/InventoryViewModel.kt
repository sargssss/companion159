package com.lifelover.companion159.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelover.companion159.R
import com.lifelover.companion159.domain.models.AppError
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.toAppError
import com.lifelover.companion159.domain.usecases.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryState(
    val items: List<InventoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val message: Int? = null,
    val currentDisplayCategory: DisplayCategory? = null
)

/**
 * ViewModel for inventory operations
 *
 * Responsibilities:
 * - Manage UI state (loading, error, success messages)
 * - Coordinate Use Cases for business operations
 * - Handle coroutine lifecycle
 * - Transform errors to user-friendly messages
 *
 * Business logic delegated to Use Cases:
 * - AddItemUseCase - adding new items
 * - UpdateItemUseCase - full item updates
 * - UpdateQuantityUseCase - single quantity updates
 * - DeleteItemUseCase - item deletion
 * - LoadItemsUseCase - loading filtered items
 */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val loadItemsUseCase: LoadItemsUseCase,
    private val addItemUseCase: AddItemUseCase,
    private val updateItemUseCase: UpdateItemUseCase,
    private val updateQuantityUseCase: UpdateQuantityUseCase,
    private val deleteItemUseCase: DeleteItemUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "InventoryViewModel"
    }

    private val _state = MutableStateFlow(InventoryState())
    val state = _state.asStateFlow()

    /**
     * Load items for display category
     * Uses LoadItemsUseCase for business logic
     */
    fun loadItems(displayCategory: DisplayCategory) {
        viewModelScope.launch {
            try {
                _state.update {
                    it.copy(
                        items = emptyList(),
                        isLoading = true,
                        currentDisplayCategory = displayCategory,
                        error = null
                    )
                }

                loadItemsUseCase(displayCategory)
                    .catch { exception ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = exception.toAppError()
                            )
                        }
                    }
                    .collect { items ->
                        _state.update {
                            it.copy(
                                items = items,
                                isLoading = false,
                                error = null
                            )
                        }
                    }

            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.toAppError()
                    )
                }
            }
        }
    }

    /**
     * Add new item
     * Uses AddItemUseCase for validation and business logic
     */
    fun addNewItem(
        name: String,
        availableQuantity: Int,
        neededQuantity: Int,
        displayCategory: DisplayCategory
    ) {
        viewModelScope.launch {
            addItemUseCase(
                name = name,
                availableQuantity = availableQuantity,
                neededQuantity = neededQuantity,
                displayCategory = displayCategory
            ).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            message = R.string.item_added,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(error = error as? AppError ?: error.toAppError())
                    }
                }
            )
        }
    }

    /**
     * Update full item
     * Uses UpdateItemUseCase for validation and business logic
     */
    fun updateFullItem(
        itemId: Long,
        newName: String,
        newAvailableQuantity: Int,
        newNeededQuantity: Int,
        displayCategory: DisplayCategory
    ) {
        viewModelScope.launch {
            updateItemUseCase(
                itemId = itemId,
                newName = newName,
                newAvailableQuantity = newAvailableQuantity,
                newNeededQuantity = newNeededQuantity,
                displayCategory = displayCategory
            ).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            message = R.string.item_updated,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(error = error as? AppError ?: error.toAppError())
                    }
                }
            )
        }
    }

    /**
     * Update single quantity
     * Uses UpdateQuantityUseCase for validation and business logic
     */
    fun updateQuantity(
        itemId: Long,
        newQuantity: Int,
        displayCategory: DisplayCategory
    ) {
        viewModelScope.launch {
            updateQuantityUseCase(
                itemId = itemId,
                newQuantity = newQuantity,
                quantityType = displayCategory.quantityType
            ).fold(
                onSuccess = {
                    _state.update { it.copy(error = null) }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(error = error as? AppError ?: error.toAppError())
                    }
                }
            )
        }
    }

    /**
     * Delete item
     * Uses DeleteItemUseCase for business logic
     */
    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            deleteItemUseCase(item).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            message = R.string.item_deleted,
                            error = null
                        )
                    }
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(error = error as? AppError ?: error.toAppError())
                    }
                }
            )
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}