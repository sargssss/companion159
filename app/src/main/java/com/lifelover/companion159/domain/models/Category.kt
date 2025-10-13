package com.lifelover.companion159.domain.models

import com.lifelover.companion159.R

/**
 * Storage categories - used in database
 * Represents actual item type stored in DB
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
 * Represents the three main screens in the app
 * These are FILTERS, not storage types
 */
enum class DisplayCategory {
    AVAILABILITY,  // Shows EQUIPMENT items with availableQuantity > 0
    AMMUNITION,    // Shows AMMUNITION items (all quantities)
    NEEDS;         // Shows ALL items with neededQuantity > 0

    /**
     * Get title resource for this display category
     */
    fun titleRes(): Int = when (this) {
        AVAILABILITY -> R.string.availability
        AMMUNITION -> R.string.ammo
        NEEDS -> R.string.needs
    }

    /**
     * Convert display category to storage category for new items
     *
     * Rule:
     * - AMMUNITION screen → stores as AMMUNITION
     * - AVAILABILITY/NEEDS screens → stores as EQUIPMENT
     */
    fun toStorageCategory(): StorageCategory = when (this) {
        AMMUNITION -> StorageCategory.AMMUNITION
        AVAILABILITY, NEEDS -> StorageCategory.EQUIPMENT
    }
}