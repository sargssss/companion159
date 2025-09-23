package com.lifelover.companion159.ui

import com.lifelover.companion159.data.model.Post

/**
 * Data class representing the current state of our UI
 *
 * This is immutable - we create new instances instead of modifying existing ones
 * This makes state changes predictable and easier to debug
 */
data class UiState(
    val posts: List<Post> = emptyList(),    // List of posts to display
    val isLoading: Boolean = false,          // Whether we're loading data
    val error: String? = null                // Error message if something went wrong
)

/**
 * Why separate UI state?
 * - Single source of truth for UI
 * - Easy to test different states
 * - Clear separation between data and UI state
 * - Compose can efficiently recompose only when state changes
 */