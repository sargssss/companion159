package com.lifelover.companion159.net

import com.lifelover.companion159.data.Post
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Interface defining our API endpoints
 * Retrofit uses this interface to generate the actual implementation
 *
 * Base URL will be: https://jsonplaceholder.typicode.com/
 */
interface ApiService {

    /**
     * Get all posts from the API
     *
     * @GET annotation means this is a GET HTTP request
     * "posts" is appended to base URL: https://jsonplaceholder.typicode.com/posts
     *
     * suspend keyword means this function can be paused and resumed
     * This allows it to run on background threads without blocking the UI
     *
     * @return List of Post objects parsed from JSON response
     */
    @GET("posts")
    suspend fun getPosts(): List<Post>

    /**
     * Get a single post by ID
     *
     * {id} in the URL is a placeholder that gets replaced by the id parameter
     * Example: getPosts(5) calls https://jsonplaceholder.typicode.com/posts/5
     *
     * @Path("id") tells Retrofit to substitute {id} with the id parameter
     */
    @GET("posts/{id}")
    suspend fun getPost(@Path("id") id: Int): Post
}