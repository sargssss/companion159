package com.lifelover.companion159.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelover.companion159.R
import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.AppError
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.toAppError
import com.lifelover.companion159.domain.models.toStorageCategory
import com.lifelover.companion159.domain.usecases.DeleteItemUseCase
import com.lifelover.companion159.domain.validation.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for inventory screens
 * Contains list of items, loading state, and optional error
 */
data class InventoryState(
    val items: List<InventoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val message: Int? = null, // String resource ID
    val currentDisplayCategory: DisplayCategory? = null
)

/**
 * ViewModel for inventory management
 *
 * Responsibilities:
 * - Load items filtered by display category (only relevant items)
 * - Validate input before repository calls
 * - Handle smart deletion through use case
 * - Provide reactive state via Flow
 */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: InventoryRepository,
    private val deleteItemUseCase: DeleteItemUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryState())
    val state = _state.asStateFlow()

    /**
     * Load items based on DisplayCategory
     * Applies smart filtering to show only relevant items
     */
    fun loadItems(displayCategory: DisplayCategory) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(
                    isLoading = true,
                    currentDisplayCategory = displayCategory
                )}

                // Get base flow from repository
                val baseFlow: Flow<List<InventoryItem>> = when (displayCategory) {
                    is DisplayCategory.Availability -> repository.getAvailabilityItems()
                    is DisplayCategory.Ammunition -> repository.getAmmunitionItems()
                    is DisplayCategory.Needs -> repository.getNeedsItems()
                }

                // Apply smart filtering
                baseFlow
                    .map { items -> filterItemsByCategory(items, displayCategory) }
                    .collect { filteredItems ->
                        _state.update { it.copy(
                            items = filteredItems,
                            isLoading = false
                        )}
                    }
            } catch (e: Exception) {
                val appError = e.toAppError()
                _state.update { it.copy(
                    isLoading = false,
                    error = appError
                )}
            }
        }
    }

    /**
     * Filter items to show only relevant ones based on category
     *
     * Rules:
     * - Availability: show only items with availableQuantity > 0
     * - Ammunition: show only items with availableQuantity > 0
     * - Needs: show only items with neededQuantity > 0
     */
    private fun filterItemsByCategory(
        items: List<InventoryItem>,
        displayCategory: DisplayCategory
    ): List<InventoryItem> {
        return when (displayCategory) {
            is DisplayCategory.Availability,
            is DisplayCategory.Ammunition -> {
                items.filter { it.availableQuantity > 0 }
            }
            is DisplayCategory.Needs -> {
                items.filter { it.neededQuantity > 0 }
            }
        }
    }

    /**
     * Add new item with validation
     * Validates all inputs before calling repository
     */
    fun addNewItem(
        name: String,
        availableQuantity: Int,
        neededQuantity: Int,
        displayCategory: DisplayCategory
    ) {
        viewModelScope.launch {
            // Validate inputs
            val validationResult = InputValidator.validateNewItem(
                name = name,
                availableQuantity = availableQuantity,
                neededQuantity = neededQuantity
            )

            validationResult
                .onSuccess { validated ->
                    try {
                        val storageCategory = displayCategory.toStorageCategory()

                        val item = InventoryItem(
                            id = 0,
                            itemName = validated.name,
                            availableQuantity = validated.availableQuantity,
                            neededQuantity = validated.neededQuantity,
                            category = storageCategory,
                            crewName = ""
                        )

                        repository.addItem(item)
                        _state.update { it.copy(message = R.string.item_added) }

                    } catch (e: Exception) {
                        _state.update { it.copy(error = e.toAppError()) }
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error as? AppError ?: AppError.Unknown(error.message ?: "")) }
                }
        }
    }

    /**
     * Update full item with validation
     */
    fun updateFullItem(
        itemId: Long,
        newName: String,
        newAvailableQuantity: Int,
        newNeededQuantity: Int,
        displayCategory: DisplayCategory
    ) {
        viewModelScope.launch {
            // Validate inputs
            val validationResult = InputValidator.validateNewItem(
                name = newName,
                availableQuantity = newAvailableQuantity,
                neededQuantity = newNeededQuantity
            )

            validationResult
                .onSuccess { validated ->
                    try {
                        val existingItem = _state.value.items.find { it.id == itemId }

                        if (existingItem == null) {
                            val storageCategory = displayCategory.toStorageCategory()
                            val item = InventoryItem(
                                id = itemId,
                                itemName = validated.name,
                                availableQuantity = validated.availableQuantity,
                                neededQuantity = validated.neededQuantity,
                                category = storageCategory,
                                crewName = ""
                            )
                            repository.updateItem(item)
                        } else {
                            val updatedItem = existingItem.copy(
                                itemName = validated.name,
                                availableQuantity = validated.availableQuantity,
                                neededQuantity = validated.neededQuantity
                            )
                            repository.updateItem(updatedItem)
                        }

                        _state.update { it.copy(message = R.string.item_updated) }

                    } catch (e: Exception) {
                        _state.update { it.copy(error = e.toAppError()) }
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error as? AppError ?: AppError.Unknown(error.message ?: "")) }
                }
        }
    }

    /**
     * Update quantity based on display category
     * Uses exhaustive when expression
     */
    fun updateQuantity(
        itemId: Long,
        newQuantity: Int,
        displayCategory: DisplayCategory
    ) {
        // Validate quantity
        val validationResult = InputValidator.validateQuantity(newQuantity)

        validationResult
            .onSuccess { validQuantity ->
                viewModelScope.launch {
                    try {
                        // Exhaustive when - all cases must be handled
                        when (displayCategory) {
                            is DisplayCategory.Availability,
                            is DisplayCategory.Ammunition -> {
                                repository.updateItemQuantity(itemId, validQuantity)
                            }
                            is DisplayCategory.Needs -> {
                                repository.updateNeededQuantity(itemId, validQuantity)
                            }
                        }
                    } catch (e: Exception) {
                        _state.update { it.copy(error = e.toAppError()) }
                    }
                }
            }
            .onFailure { error ->
                _state.update { it.copy(error = error as? AppError ?: AppError.Unknown(error.message ?: "")) }
            }
    }

    /**
     * Smart delete using use case
     * Handles business logic: zero-out or full delete
     */
    fun deleteItem(item: InventoryItem) {
        viewModelScope.launch {
            val currentCategory = _state.value.currentDisplayCategory ?: return@launch

            deleteItemUseCase(item, currentCategory)
                .onSuccess {
                    _state.update { it.copy(message = R.string.item_deleted) }
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error.toAppError()) }
                }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}