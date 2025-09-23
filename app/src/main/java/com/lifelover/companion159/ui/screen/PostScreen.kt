package com.lifelover.companion159.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifelover.companion159.data.MainViewModel
import com.lifelover.companion159.data.Post
import com.lifelover.companion159.ui.UiState

/**
 * Main screen composable function
 *
 * @Composable annotation marks this as a Compose function
 * Compose functions describe UI rather than create UI widgets
 *
 * viewModel() automatically creates and provides the ViewModel
 * It handles lifecycle and survives configuration changes
 */
@Composable
fun PostScreen(
    viewModel: MainViewModel = viewModel(),  // Default parameter with ViewModel
    modifier: Modifier = Modifier
) {
    // Observe UI state from ViewModel
    // collectAsState() converts StateFlow to Compose State
    // by keyword creates a delegate that automatically recomposes when state changes
    //val uiState by viewModel.uiState.collectAsState()
    val uiState by viewModel.uiState.collectAsState(initial = UiState())

    /**
     * Column arranges children vertically
     * Similar to LinearLayout with vertical orientation
     */
    Column(
        modifier = modifier
            .fillMaxSize()      // Take up all available space
            .padding(16.dp)     // Add 16dp padding on all sides
    ) {
        /**
         * when expression handles different UI states
         * Similar to switch statement but more powerful
         */
        when {
            // Show loading indicator when data is being fetched
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center  // Center the loading indicator
                ) {
                    CircularProgressIndicator()  // Material 3 loading spinner
                }
            }

            // Show error message if something went wrong
            uiState.error != null -> {
                Text(
                    text = "Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error  // Use theme error color
                )
            }

            // Show the list of posts when data is loaded successfully
            else -> {
                /**
                 * LazyColumn is like RecyclerView but simpler
                 * Only renders visible items for better performance
                 */
                LazyColumn {
                    // items() creates list items from a collection
                    items(uiState.posts) { post ->
                        PostItem(post = post)
                    }
                }
            }
        }
    }
}

/**
 * Individual post item composable
 * Displays a single post in a card
 *
 * @param post The post data to display
 */
@Composable
fun PostItem(post: Post) {
    /**
     * Card provides Material 3 card styling
     * Includes elevation, rounded corners, and proper theming
     */
    Card(
        modifier = Modifier
            .fillMaxWidth()             // Take full width
            .padding(vertical = 4.dp),  // Vertical spacing between cards
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp     // Shadow depth
        )
    ) {
        /**
         * Column inside the card for vertical layout
         */
        Column(
            modifier = Modifier.padding(16.dp)  // Internal padding
        ) {
            // Post title - bold and prominent
            Text(
                text = post.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Space between title and body
            Spacer(modifier = Modifier.height(8.dp))

            // Post body text
            Text(
                text = post.body,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}