package com.lifelover.companion159.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelover.companion159.data.sync.SyncService
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.sync.SyncStatus
import com.lifelover.companion159.domain.models.InventoryItem
import com.lifelover.companion159.domain.models.DisplayCategory
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
    val currentDisplayCategory: DisplayCategory? = null
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
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

    /**
     * Load items based on DisplayCategory
     */
    fun loadItemsForDisplay(displayCategory: DisplayCategory) {
        viewModelScope.launch {
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
        }
    }

    /**
     * Add new item
     * Category logic:
     * - If displayCategory is AMMUNITION -> InventoryCategory.AMMUNITION
     * - If displayCategory is NEEDS -> Check if ammunition exists, otherwise EQUIPMENT
     * - Otherwise -> InventoryCategory.EQUIPMENT (default)
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
                Log.d(TAG, "Creating item: $name")
                Log.d(TAG, "   Available: $availableQuantity")
                Log.d(TAG, "   Needed: $neededQuantity")
                Log.d(TAG, "   Display category: $displayCategory")

                // Determine internal category
                val internalCategory = when (displayCategory) {
                    DisplayCategory.AMMUNITION -> {
                        // Creating from БК screen -> always AMMUNITION
                        InventoryCategory.AMMUNITION
                    }
                    DisplayCategory.NEEDS -> {
                        // Creating from Потреба screen
                        // Check if item already exists as ammunition
                        val existingItem = _state.value.items.find {
                            it.itemName.equals(name.trim(), ignoreCase = true)
                        }
                        existingItem?.category ?: InventoryCategory.EQUIPMENT
                    }
                    else -> {
                        // Creating from Наявність -> always EQUIPMENT
                        InventoryCategory.EQUIPMENT
                    }
                }

                Log.d(TAG, "   Internal category: $internalCategory")

                val item = InventoryItem(
                    id = 0,
                    itemName = name.trim(),
                    availableQuantity = availableQuantity,
                    neededQuantity = neededQuantity,
                    category = internalCategory,
                    crewName = ""  // Will be set in repository
                )

                repository.addItem(item)
                Log.d(TAG, "Item created successfully")
                _state.update { it.copy(message = "Предмет додано") }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to add item", e)
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * Update full item with both quantities
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
                Log.d(TAG, "🔄 Updating full item")
                Log.d(TAG, "   Item ID: $itemId")
                Log.d(TAG, "   New name: $newName")
                Log.d(TAG, "   New available: $newAvailableQuantity")
                Log.d(TAG, "   New needed: $newNeededQuantity")
                Log.d(TAG, "   Display category: $displayCategory")

                // Get existing item to preserve other fields
                val existingItem = _state.value.items.find { it.id == itemId }
                if (existingItem == null) {
                    Log.e(TAG, "❌ Item not found in current state: $itemId")
                    // Try to get from repository
                    val item = InventoryItem(
                        id = itemId,
                        itemName = newName.trim(),
                        availableQuantity = newAvailableQuantity,
                        neededQuantity = newNeededQuantity,
                        category = when (displayCategory) {
                            DisplayCategory.AMMUNITION -> InventoryCategory.AMMUNITION
                            else -> InventoryCategory.EQUIPMENT
                        },
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

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to update item", e)
                _state.update { it.copy(error = "Помилка оновлення: ${e.message}") }
            }
        }
    }

    /**
     * Update available quantity (for AVAILABILITY and AMMUNITION categories)
     */
    fun updateAvailableQuantity(item: InventoryItem, newQuantity: Int) {
        if (newQuantity < 0) return

        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating available quantity: ${item.itemName}, new: $newQuantity")
                repository.updateItemQuantity(item.id, newQuantity)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to update available quantity", e)
                _state.update { it.copy(error = "Помилка: ${e.message}") }
            }
        }
    }

    /**
     * Update needed quantity (for NEEDS category)
     * CRITICAL: This triggers DB update which should reflect in UI via Flow
     */
    fun updateNeededQuantity(item: InventoryItem, newQuantity: Int) {
        if (newQuantity < 0) return

        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Updating needed quantity: ${item.itemName}")
                Log.d(TAG, "   Item ID: ${item.id}")
                Log.d(TAG, "   Old needed: ${item.neededQuantity} -> New needed: $newQuantity")

                repository.updateNeededQuantity(item.id, newQuantity)

                Log.d(TAG, "✅ Needed quantity update completed")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to update needed quantity", e)
                _state.update { it.copy(error = "Помилка: ${e.message}") }
            }
        }
    }
    /**
     * Delete item
     */
    fun deleteItemById(id: Long) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Deleting item ID: $id")
                repository.deleteItem(id)
                _state.update { it.copy(message = "Предмет видалено") }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete item", e)
                _state.update { it.copy(error = "Помилка видалення: ${e.message}") }
            }
        }
    }

    /**
     * Manual sync
     */
    fun syncData() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Manual sync requested")
                syncService.performSync()
                    .onSuccess {
                        _state.update { it.copy(message = "Синхронізація успішна") }
                    }
                    .onFailure { error ->
                        _state.update { it.copy(error = "Помилка синхронізації: ${error.message}") }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Manual sync failed", e)
                _state.update { it.copy(error = "Помилка синхронізації: ${e.message}") }
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