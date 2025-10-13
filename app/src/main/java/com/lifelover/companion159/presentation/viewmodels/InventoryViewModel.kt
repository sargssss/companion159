package com.lifelover.companion159.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.toStorageCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for inventory screens
 * Simplified: removed sync fields
 */
data class InventoryState(
    val items: List<InventoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val currentDisplayCategory: DisplayCategory? = null
)

/**
 * ViewModel for inventory operations
 * Simplified: calls Repository directly (no UseCases)
 * Requires authenticated user for all operations
 */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: InventoryRepository
) : ViewModel() {

    companion object {
        private const val TAG = "InventoryViewModel"
    }

    private val _state = MutableStateFlow(InventoryState())
    val state = _state.asStateFlow()

    // ============================================================
    // LOAD ITEMS
    // ============================================================

    /**
     * Load items based on DisplayCategory
     * Called from InventoryScreen
     *
     * FIXED: Use when with sealed class
     */
    fun loadItems(displayCategory: DisplayCategory) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(
                    isLoading = true,
                    currentDisplayCategory = displayCategory
                )}

                // FIXED: Exhaustive when with sealed class
                val flow = when (displayCategory) {
                    is DisplayCategory.Availability -> repository.getAvailabilityItems()
                    is DisplayCategory.Ammunition -> repository.getAmmunitionItems()
                    is DisplayCategory.Needs -> repository.getNeedsItems()
                }

                // Collect and update state
                flow.collect { items ->
                    _state.update { it.copy(
                        items = items,
                        isLoading = false
                    )}
                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "❌ User not authenticated", e)
                _state.update { it.copy(
                    isLoading = false,
                    error = "Необхідно увійти в акаунт"
                )}
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading items", e)
                _state.update { it.copy(
                    isLoading = false,
                    error = e.message
                )}
            }
        }
    }

    // ============================================================
    // CREATE ITEM
    // ============================================================

    /**
     * Add new item
     * Validation done here (UI logic)
     */
    fun addNewItem(
        name: String,
        availableQuantity: Int,
        neededQuantity: Int,
        displayCategory: DisplayCategory
    ) {
        // Validation
        if (name.isBlank()) {
            _state.update { it.copy(error = "Назва не може бути порожньою") }
            return
        }

        if (availableQuantity < 0 || neededQuantity < 0) {
            _state.update { it.copy(error = "Кількість не може бути від'ємною") }
            return
        }

        viewModelScope.launch {
            try {
                val storageCategory = displayCategory.toStorageCategory()
                Log.d(TAG, "➕ Adding item: $name ($storageCategory)")

                val item = InventoryItem(
                    id = 0,
                    itemName = name.trim(),
                    availableQuantity = availableQuantity,
                    neededQuantity = neededQuantity,
                    category = storageCategory,
                    crewName = ""  // Will be set in repository
                )

                repository.addItem(item)
                Log.d(TAG, "✅ Item created successfully")
                _state.update { it.copy(message = "Предмет додано") }

            } catch (e: IllegalStateException) {
                Log.e(TAG, "❌ User not authenticated", e)
                _state.update { it.copy(error = "Необхідно увійти в акаунт") }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to add item", e)
                _state.update { it.copy(error = "Помилка: ${e.message}") }
            }
        }
    }

    // ============================================================
    // UPDATE ITEM
    // ============================================================

    /**
     * Update full item with both quantities
     * Used when editing item from AddEditItemScreen
     */
    fun updateFullItem(
        itemId: Long,
        newName: String,
        newAvailableQuantity: Int,
        newNeededQuantity: Int,
        displayCategory: DisplayCategory
    ) {
        // Validation
        if (newName.isBlank()) {
            _state.update { it.copy(error = "Назва не може бути порожньою") }
            return
        }

        if (newAvailableQuantity < 0 || newNeededQuantity < 0) {
            _state.update { it.copy(error = "Кількість не може бути від'ємною") }
            return
        }

        viewModelScope.launch {
            try {
                // Get existing item to preserve other fields
                val existingItem = _state.value.items.find { it.id == itemId }

                if (existingItem == null) {
                    Log.w(TAG, "⚠️ Item not found in state, creating with category")
                    val storageCategory = displayCategory.toStorageCategory()

                    val item = InventoryItem(
                        id = itemId,
                        itemName = newName.trim(),
                        availableQuantity = newAvailableQuantity,
                        neededQuantity = newNeededQuantity,
                        category = storageCategory,
                        crewName = ""
                    )
                    repository.updateItem(item)
                } else {
                    val updatedItem = existingItem.copy(
                        itemName = newName.trim(),
                        availableQuantity = newAvailableQuantity,
                        neededQuantity = newNeededQuantity
                    )
                    repository.updateItem(updatedItem)
                }

                Log.d(TAG, "✅ Item updated successfully")
                _state.update { it.copy(message = "Предмет оновлено") }

            } catch (e: SecurityException) {
                Log.e(TAG, "❌ Security error", e)
                _state.update { it.copy(error = "Недостатньо прав") }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "❌ User not authenticated", e)
                _state.update { it.copy(error = "Необхідно увійти в акаунт") }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to update item", e)
                _state.update { it.copy(error = "Помилка оновлення: ${e.message}") }
            }
        }
    }

    /**
     * Update quantity based on display category
     * - AVAILABILITY/AMMUNITION: updates available_quantity
     * - NEEDS: updates needed_quantity
     *
     * FIXED: Use when with sealed class
     */
    fun updateQuantity(
        itemId: Long,
        newQuantity: Int,
        displayCategory: DisplayCategory
    ) {
        if (newQuantity < 0) {
            _state.update { it.copy(error = "Кількість не може бути від'ємною") }
            return
        }

        viewModelScope.launch {
            try {
                // FIXED: Exhaustive when with sealed class
                when (displayCategory) {
                    is DisplayCategory.Availability,
                    is DisplayCategory.Ammunition -> {
                        Log.d(TAG, "📦 Updating available: $itemId -> $newQuantity")
                        repository.updateItemQuantity(itemId, newQuantity)
                    }
                    is DisplayCategory.Needs -> {
                        Log.d(TAG, "📦 Updating needed: $itemId -> $newQuantity")
                        repository.updateNeededQuantity(itemId, newQuantity)
                    }
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "❌ Security error", e)
                _state.update { it.copy(error = "Недостатньо прав") }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "❌ User not authenticated", e)
                _state.update { it.copy(error = "Необхідно увійти в акаунт") }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to update quantity", e)
                _state.update { it.copy(error = "Помилка: ${e.message}") }
            }
        }
    }

    // ============================================================
    // DELETE ITEM
    // ============================================================

    /**
     * Delete item by ID
     */
    fun deleteItem(id: Long) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🗑️ Deleting item ID: $id")
                repository.deleteItem(id)
                _state.update { it.copy(message = "Предмет видалено") }

            } catch (e: SecurityException) {
                Log.e(TAG, "❌ Security error", e)
                _state.update { it.copy(error = "Недостатньо прав") }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "❌ User not authenticated", e)
                _state.update { it.copy(error = "Необхідно увійти в акаунт") }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to delete item", e)
                _state.update { it.copy(error = "Помилка видалення: ${e.message}") }
            }
        }
    }

    // ============================================================
    // STATE MANAGEMENT
    // ============================================================

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}