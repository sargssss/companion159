package com.lifelover.companion159.domain.models

import com.lifelover.companion159.R

/**
 * Storage categories - used in database
 * Represents actual item type stored in DB
 *
 * This is ENUM - stays as is
 */
enum class StorageCategory {
    AMMUNITION,  // Ammunition items
    EQUIPMENT;   // All other items (drones, tools, food, etc.)

    /**
     * Get icon resource for this storage category
     */
    fun iconRes(): Int = when (this) {
        AMMUNITION -> R.drawable.bomb
        EQUIPMENT -> R.drawable.tool
    }
}

/**
 * Display categories for UI screens
 *
 * CHANGED: sealed class instead of enum
 * Each category knows its storage mapping
 *
 * Benefits:
 * - Open/Closed compliant
 * - Each category is self-contained
 * - Easy to add new categories
 */
sealed class DisplayCategory(
    val name: String,  // For navigation
    val titleRes: Int,
    val storageCategory: StorageCategory
) {
    /**
     * Shows EQUIPMENT items with availableQuantity > 0
     */
    data object Availability : DisplayCategory(
        name = "AVAILABILITY",
        titleRes = R.string.availability,
        storageCategory = StorageCategory.EQUIPMENT
    )

    /**
     * Shows AMMUNITION items (all quantities)
     */
    data object Ammunition : DisplayCategory(
        name = "AMMUNITION",
        titleRes = R.string.ammo,
        storageCategory = StorageCategory.AMMUNITION
    )

    /**
     * Shows ALL items with neededQuantity > 0
     */
    data object Needs : DisplayCategory(
        name = "NEEDS",
        titleRes = R.string.needs,
        storageCategory = StorageCategory.EQUIPMENT
    )

    companion object {
        /**
         * Get all display categories
         * Used for iteration in UI
         */
        val entries: List<DisplayCategory> = listOf(
            Availability,
            Ammunition,
            Needs
        )

        /**
         * Get display category by name
         * Used for navigation arguments
         */
        fun valueOf(name: String): DisplayCategory = when (name.uppercase()) {
            "AVAILABILITY" -> Availability
            "AMMUNITION" -> Ammunition
            "NEEDS" -> Needs
            else -> throw IllegalArgumentException("Unknown DisplayCategory: $name")
        }
    }
}

/**
 * Extension: Get storage category from display category
 * Simple property access - no complex logic
 */
fun DisplayCategory.toStorageCategory(): StorageCategory = this.storageCategory

/**
 * Extension: Get title resource
 */
fun DisplayCategory.titleRes(): Int = this.titleRes