package com.lifelover.companion159.data.repository

import com.lifelover.companion159.data.model.Post
import com.lifelover.companion159.network.RetrofitInstance

/**
 * Repository pattern centralizes data access
 * This is the single source of truth for Post data
 *
 * Benefits:
 * - Abstracts data sources (API, database, cache)
 * - Provides clean API to ViewModels
 * - Handles error handling consistently
 * - Easy to test and mock
 */
class PostRepository {
    // Reference to our API service
    private val api = RetrofitInstance.api

    /**
     * Fetch posts from the API with error handling
     *
     * Result<T> is Kotlin's way of handling success/failure
     * It's safer than throwing exceptions
     *
     * @return Result.success(data) if API call succeeds
     *         Result.failure(exception) if API call fails
     */
    suspend fun getPosts(): Result<List<Post>> {
        return try {
            // Make the API call
            val posts = api.getPosts()
            // Wrap successful result
            Result.success(posts)
        } catch (e: Exception) {
            // Catch any network/parsing errors
            // This includes: no internet, server errors, JSON parsing errors
            Result.failure(e)
        }
    }

    /**
     * Get a single post by ID
     * Same error handling pattern as getPosts()
     */
    suspend fun getPost(id: Int): Result<Post> {
        return try {
            val post = api.getPost(id)
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}