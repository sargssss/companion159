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
import com.lifelover.companion159.viewmodel.MainViewModel
import com.lifelover.companion159.data.model.Post
import com.lifelover.companion159.ui.UiState

@Composable
fun CrewScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()      // Take up all available space
            .padding(16.dp)     // Add 16dp padding on all sides
    ) {


        LazyColumn(
            modifier = Modifier.padding(16.dp)  // Internal padding
        ) {

        }
    }
}

/**
 * Individual post item composable
 * Displays a single post in a card
 *
 * @param post The post data to display
 */
/*@Composable
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
}*/