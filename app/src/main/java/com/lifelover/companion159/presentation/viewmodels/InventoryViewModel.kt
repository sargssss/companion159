package com.lifelover.companion159.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelover.companion159.domain.models.DisplayCategory
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.toStorageCategory
import com.lifelover.companion159.data.repository.InventoryRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for inventory screens
 */
data class InventoryState(
    val items: List<InventoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val currentDisplayCategory: DisplayCategory? = null,
    // Keep sync UI states for future use (but not functional now)
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val lastSyncTime: Long? = null
)

/**
 * Sync status enum - kept for UI compatibility
 * Will be functional again when sync is re-implemented
 */
enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
    OFFLINE
}

/**
 * ViewModel for inventory operations
 * Manages UI state and coordinates with repository
 * Requires authenticated user for all operations
 */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val repository: InventoryRepositoryImpl
) : ViewModel() {

    companion object {
        private const val TAG = "InventoryViewModel"
    }

    private val _state = MutableStateFlow(InventoryState())
    val state = _state.asStateFlow()

    /**
     * Load items based on DisplayCategory
     * This method is called from InventoryScreen
     */
    fun loadItems(displayCategory: DisplayCategory) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(
                    isLoading = true,
                    currentDisplayCategory = displayCategory
                )}

                val flow = when (displayCategory) {
                    DisplayCategory.AVAILABILITY -> repository.getAvailabilityItems()
                    DisplayCategory.AMMUNITION -> repository.getAmmunitionItems()
                    DisplayCategory.NEEDS -> repository.getNeedsItems()
                }

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

    /**
     * Add new item
     * Uses extension function to convert DisplayCategory → StorageCategory
     */
    fun addNewItem(
        name: String,
        availableQuantity: Int,
        neededQuantity: Int,
        displayCategory: DisplayCategory
    ) {
        if (name.isBlank()) return

        viewModelScope.launch {
            try {
                // ✅ USE EXTENSION FUNCTION - no duplication!
                val storageCategory = displayCategory.toStorageCategory()
                Log.d(TAG, "   Storage category: $storageCategory")

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
                _state.update { it.copy(error = e.message) }
            }
        }
    }

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
        if (newName.isBlank()) return

        viewModelScope.launch {
            try {
                // Get existing item to preserve other fields
                val existingItem = _state.value.items.find { it.id == itemId }
                if (existingItem == null) {
                    Log.e(TAG, "❌ Item not found in current state: $itemId")

                    // ✅ USE EXTENSION FUNCTION
                    val storageCategory = displayCategory.toStorageCategory()

                    val item = InventoryItem(
                        id = itemId,
                        itemName = newName.trim(),
                        availableQuantity = newAvailableQuantity,
                        neededQuantity = newNeededQuantity,
                        category = storageCategory,
                        crewName = ""  // Will be set in repository
                    )
                    repository.updateItem(item)
                    Log.d(TAG, "✅ Item updated (no existing state)")
                    _state.update { it.copy(message = "Предмет оновлено") }
                    return@launch
                }

                val updatedItem = existingItem.copy(
                    itemName = newName.trim(),
                    availableQuantity = newAvailableQuantity,
                    neededQuantity = newNeededQuantity
                )

                Log.d(TAG, "📦 Updating item in repository...")
                repository.updateItem(updatedItem)
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
     */
    fun updateQuantity(
        itemId: Long,
        newQuantity: Int,
        displayCategory: DisplayCategory
    ) {
        if (newQuantity < 0) return

        viewModelScope.launch {
            try {
                when (displayCategory) {
                    DisplayCategory.AVAILABILITY,
                    DisplayCategory.AMMUNITION -> {
                        Log.d(TAG, "Updating available quantity: $itemId -> $newQuantity")
                        repository.updateItemQuantity(itemId, newQuantity)
                    }
                    DisplayCategory.NEEDS -> {
                        Log.d(TAG, "Updating needed quantity: $itemId -> $newQuantity")
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

    /**
     * Delete item by ID
     */
    fun deleteItem(id: Long) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Deleting item ID: $id")
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

    /**
     * Sync function - kept for UI compatibility
     * Does nothing until sync is re-implemented
     */
    fun sync() {
        Log.d(TAG, "⚠️ Sync called but not implemented (sync removed)")
        _state.update { it.copy(
            syncStatus = SyncStatus.IDLE,
            message = "Синхронізація тимчасово недоступна"
        )}
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}