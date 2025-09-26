package com.lifelover.companion159.network

import com.lifelover.companion159.network.dto.ApiInventoryItem
import com.lifelover.companion159.network.dto.SyncResponse
import retrofit2.http.*

interface InventoryApiService {
    @POST("inventory/items")
    suspend fun createItem(@Body item: ApiInventoryItem): ApiInventoryItem

    @PUT("inventory/items/{id}")
    suspend fun updateItem(@Path("id") id: String, @Body item: ApiInventoryItem): ApiInventoryItem

    @DELETE("inventory/items/{id}")
    suspend fun deleteItem(@Path("id") id: String)

    @GET("inventory/sync")
    suspend fun getUpdates(@Query("since") timestamp: String): SyncResponse
}