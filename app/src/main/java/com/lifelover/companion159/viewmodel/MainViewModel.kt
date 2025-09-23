package com.lifelover.companion159.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelover.companion159.data.repository.PostRepository
import com.lifelover.companion159.ui.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel manages UI-related data and business logic
 * Survives configuration changes (like screen rotation)
 *
 * Key responsibilities:
 * - Hold UI state
 * - Handle user actions
 * - Communicate with Repository
 * - Manage loading states and errors
 */
class MainViewModel : ViewModel() {

    // Repository instance for data access
    private val repository = PostRepository()

    /**
     * MutableStateFlow holds mutable state that can be observed
     * Private so only this ViewModel can modify it
     */
    private val _uiState = MutableStateFlow(UiState())

    /**
     * Public read-only access to UI state
     * UI observes this to get state updates
     * asStateFlow() converts MutableStateFlow to read-only StateFlow
     */
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * init block runs when ViewModel is created
     * We start loading data immediately
     */
    init {
        loadPosts()
    }

    /**
     * Load posts from the repository
     * This function handles the entire loading flow:
     * 1. Set loading state
     * 2. Make API call
     * 3. Handle success/failure
     * 4. Update UI state
     */
    private fun loadPosts() {
        // Launch coroutine in viewModelScope
        // viewModelScope automatically cancels when ViewModel is destroyed
        viewModelScope.launch {
            // Step 1: Set loading state
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Step 2: Make API call through repository
            repository.getPosts()
                .onSuccess { posts ->
                    // Step 3a: Handle success
                    _uiState.value = _uiState.value.copy(
                        posts = posts,          // Set the received posts
                        isLoading = false,      // Stop loading
                        error = null            // Clear any previous errors
                    )
                }
                .onFailure { error ->
                    // Step 3b: Handle failure
                    _uiState.value = _uiState.value.copy(
                        error = error.message,  // Set error message
                        isLoading = false,      // Stop loading
                        posts = emptyList()     // Clear posts on error
                    )
                }
        }
    }

    /**
     * Public function to refresh data
     * Can be called from UI (e.g., pull-to-refresh)
     */
    fun refresh() {
        loadPosts()
    }
}