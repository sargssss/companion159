package com.lifelover.companion159.domain.usecases.inventory

import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.models.DisplayCategory
import javax.inject.Inject

/**
 * Update item quantity based on display category
 *
 * Logic:
 * - AVAILABILITY: updates available_quantity
 * - AMMUNITION: updates available_quantity
 * - NEEDS: updates needed_quantity
 */
class UpdateQuantityUseCase @Inject constructor(
    private val repository: InventoryRepository
) {
    /**
     * Update quantity for item in specific display category
     *
     * @param itemId Item ID
     * @param newQuantity New quantity value
     * @param displayCategory Which screen/category is being updated
     */
    suspend operator fun invoke(
        itemId: Long,
        newQuantity: Int,
        displayCategory: DisplayCategory
    ) {
        when (displayCategory) {
            DisplayCategory.AVAILABILITY -> {
                // Update available quantity for items in "Наявність" screen
                repository.updateItemQuantity(itemId, newQuantity)
            }
            DisplayCategory.AMMUNITION -> {
                // Update available quantity for ammunition in "БК" screen
                repository.updateItemQuantity(itemId, newQuantity)
            }
            DisplayCategory.NEEDS -> {
                // Update needed quantity for items in "Потреби" screen
                repository.updateNeededQuantity(itemId, newQuantity)
            }
        }
    }
}