package com.lifelover.companion159.domain.models

import com.lifelover.companion159.R

/**
 * Display categories for UI
 * These are separate from internal InventoryCategory
 */
enum class DisplayCategory {
    AVAILABILITY,
    AMMUNITION,
    NEEDS
}

fun DisplayCategory.titleRes(): Int = when (this) {
    DisplayCategory.AVAILABILITY -> R.string.availability
    DisplayCategory.AMMUNITION -> R.string.ammo
    DisplayCategory.NEEDS -> R.string.needs
}