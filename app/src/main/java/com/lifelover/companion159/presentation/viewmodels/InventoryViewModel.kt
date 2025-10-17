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

/**
 * UI state for inventory screen
 */
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
 * Does NOT contain business logic - delegated to Use Cases:
 * - AddItemUseCase - adding new items
 * - UpdateItemUseCase - full item updates
 * - UpdateQuantityUseCase - single quantity updates
 * - DeleteItemUseCase - item deletion
 * - LoadItemsUseCase - loading filtered items
 *
 * @param loadItemsUseCase Use case for loading items
 * @param addItemUseCase Use case for adding items
 * @param updateItemUseCase Use case for full item updates
 * @param updateQuantityUseCase Use case for quantity updates
 * @param deleteItemUseCase Use case for item deletion
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
     *
     * Subscribes to reactive Flow from LoadItemsUseCase
     * Updates UI automatically when items change
     *
     * @param displayCategory Category to display (Availability/Ammunition/Needs)
     */
    fun loadItems(displayCategory: DisplayCategory) {
        viewModelScope.launch {
            try {
                // Set loading state
                _state.update { it.copy(
                    items = emptyList(),
                    isLoading = true,
                    currentDisplayCategory = displayCategory,
                    error = null
                )}

                // Load items via Use Case (Flow)
                loadItemsUseCase(displayCategory)
                    .catch { exception ->
                        // Handle Flow errors
                        _state.update { it.copy(
                            isLoading = false,
                            error = exception.toAppError()
                        )}
                    }
                    .collect { items ->
                        // Update UI with loaded items
                        _state.update { it.copy(
                            items = items,
                            isLoading = false,
                            error = null
                        )}
                    }

            } catch (e: Exception) {
                // Handle initial subscription errors
                _state.update { it.copy(
                    isLoading = false,
                    error = e.toAppError()
                )}
            }
        }
    }

    /**
     * Add new item
     *
     * Delegates to AddItemUseCase for:
     * - Input validation
     * - Category mapping
     * - Repository persistence
     *
     * @param name Item name
     * @param availableQuantity Available quantity
     * @param neededQuantity Needed quantity
     * @param displayCategory Display category
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
                    // Show success message
                    _state.update { it.copy(
                        message = R.string.item_added,
                        error = null
                    )}
                },
                onFailure = { error ->
                    // Show error
                    _state.update { it.copy(
                        error = error as? AppError ?: error.toAppError()
                    )}
                }
            )
        }
    }

    /**
     * Update full item (name + quantities)
     *
     * Delegates to UpdateItemUseCase for:
     * - Input validation
     * - Category mapping
     * - Repository persistence
     *
     * Used when editing item through form
     *
     * @param itemId Item ID to update
     * @param newName New item name
     * @param newAvailableQuantity New available quantity
     * @param newNeededQuantity New needed quantity
     * @param displayCategory Display category
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
                    // Show success message
                    _state.update { it.copy(
                        message = R.string.item_updated,
                        error = null
                    )}
                },
                onFailure = { error ->
                    // Show error
                    _state.update { it.copy(
                        error = error as? AppError ?: error.toAppError()
                    )}
                }
            )
        }
    }

    /**
     * Update single quantity (available or needed)
     *
     * Delegates to UpdateQuantityUseCase for:
     * - Quantity validation
     * - Quantity type determination
     * - Repository persistence
     *
     * Used for quick updates via +/- buttons
     *
     * @param itemId Item ID to update
     * @param newQuantity New quantity value
     * @param displayCategory Display category (determines quantity type)
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
                displayCategory = displayCategory
            ).fold(
                onSuccess = {
                    // No success message for inline quantity updates
                    // UI updates automatically via Flow
                    _state.update { it.copy(error = null) }
                },
                onFailure = { error ->
                    // Show error
                    _state.update { it.copy(
                        error = error as? AppError ?: error.toAppError()
                    )}
                }
            )
        }
    }

    /**
     * Delete item
     *
     * Delegates to DeleteItemUseCase for:
     * - Soft delete
     * - Repository persistence
     *
     * @param item Item to delete
     */
    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            deleteItemUseCase(item).fold(
                onSuccess = {
                    // Show success message
                    _state.update { it.copy(
                        message = R.string.item_deleted,
                        error = null
                    )}
                },
                onFailure = { error ->
                    // Show error
                    _state.update { it.copy(
                        error = error as? AppError ?: error.toAppError()
                    )}
                }
            )
        }
    }

    /**
     * Clear success message
     * Called after message is shown to user
     */
    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    /**
     * Clear error
     * Called after error is shown to user
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}