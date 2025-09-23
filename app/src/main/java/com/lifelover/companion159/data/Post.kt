package com.lifelover.companion159.data

import kotlinx.serialization.Serializable

/**
 * Data class representing a blog post from the API
 *
 * @Serializable annotation tells Kotlinx Serialization how to convert
 * JSON from the API into this Kotlin object
 *
 * Example JSON from API:
 * {
 *   "id": 1,
 *   "title": "Sample Title",
 *   "body": "Sample body text...",
 *   "userId": 1
 * }
 */
@Serializable
data class Post(
    val id: Int,        // Unique identifier for the post
    val title: String,  // Post title
    val body: String,   // Post content/description
    val userId: Int     // ID of the user who created the post
)