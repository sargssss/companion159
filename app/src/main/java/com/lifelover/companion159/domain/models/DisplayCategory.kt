package com.lifelover.companion159.domain.models

import com.lifelover.companion159.R

/**
 * Display categories for UI screens
 * Represents the three main screens in the app
 */
enum class DisplayCategory {
    AVAILABILITY,  // Наявність (non-ammunition items with available_quantity > 0)
    AMMUNITION,    // БК (ammunition with available_quantity)
    NEEDS          // Потреби (all items with needed_quantity > 0)
}

/**
 * Get string resource for display category title
 */
fun DisplayCategory.titleRes(): Int = when (this) {
    DisplayCategory.AVAILABILITY -> R.string.availability
    DisplayCategory.AMMUNITION -> R.string.ammo
    DisplayCategory.NEEDS -> R.string.needs
}