package com.lifelover.companion159.domain.models

import com.lifelover.companion159.R

/**
 * Unified category model that replaces both DisplayCategory and InventoryCategory
 * Represents both storage type and display filtering logic
 */
sealed class Category(
    val titleRes: Int,
    val iconRes: Int?
) {
    /**
     * Items that have available_quantity > 0 (excluding ammunition)
     * Used for display filtering only
     */
    data object Availability : Category(
        titleRes = R.string.availability,
        iconRes = null
    )

    /**
     * Ammunition items - stored with AMMUNITION type
     * Shows available_quantity
     */
    data object Ammunition : Category(
        titleRes = R.string.ammo,
        iconRes = R.drawable.bomb
    )

    /**
     * Items that have needed_quantity > 0 (all types including ammunition)
     * Used for display filtering only
     */
    data object Needs : Category(
        titleRes = R.string.needs,
        iconRes = null
    )

    /**
     * Equipment items - stored with EQUIPMENT type
     * Default storage category for non-ammunition items
     */
    data object Equipment : Category(
        titleRes = R.string.tool,
        iconRes = R.drawable.tool
    )
}

/**
 * Storage categories - used in database
 * Maps to server's item_category field (nullable)
 */
enum class StorageCategory {
    AMMUNITION,  // Maps to "ammunition" on server (but server doesn't enforce this)
    EQUIPMENT    // Maps to null or "equipment" on server (default)
}

/**
 * Extension: Convert Category to StorageCategory for database operations
 */
fun Category.toStorageCategory(): StorageCategory = when (this) {
    is Category.Ammunition -> StorageCategory.AMMUNITION
    is Category.Availability,
    is Category.Needs,
    is Category.Equipment -> StorageCategory.EQUIPMENT
}