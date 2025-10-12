package com.lifelover.companion159.domain.models

import com.lifelover.companion159.R

/**
 * Display categories for UI
 * These are separate from internal InventoryCategory
 */
enum class DisplayCategory {
    AVAILABILITY,  // Наявність: items with available_quantity > 0 (except БК)
    AMMUNITION,    // БК: items with category == AMMUNITION
    NEEDS         // Потреба: items with needed_quantity > 0 (except БК)
}

fun DisplayCategory.titleRes(): Int = when (this) {
    DisplayCategory.AVAILABILITY -> R.string.availability
    DisplayCategory.AMMUNITION -> R.string.ammo
    DisplayCategory.NEEDS -> R.string.needs
}