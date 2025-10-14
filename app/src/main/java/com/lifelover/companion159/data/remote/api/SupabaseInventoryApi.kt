package com.lifelover.companion159.data.remote.api

import android.util.Log
import com.lifelover.companion159.data.remote.dto.SupabaseInventoryItemDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API service for Supabase crew_inventory_items_duplicate table
 *
 * Provides CRUD operations with proper error handling
 * All operations run on IO dispatcher
 */
@Singleton
class SupabaseInventoryApi @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    companion object {
        private const val TAG = "SupabaseInventoryApi"
        private const val TABLE_NAME = "crew_inventory_items_duplicate"
    }

    /**
     * Fetch all items for a specific crew
     * Used for initial sync and polling
     *
     * @param crewName Crew name to filter by
     * @param updatedAfter Optional: only fetch items updated after this timestamp
     * @return List of items or empty list on error
     */
    suspend fun fetchItemsByCrewName(
        crewName: String,
        updatedAfter: String? = null
    ): Result<List<SupabaseInventoryItemDto>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching items for crew: $crewName, updatedAfter: $updatedAfter")

            val query = supabaseClient.from(TABLE_NAME)
                .select {
                    filter {
                        eq("crew_name", crewName)
                        eq("is_active", true)

                        // If updatedAfter provided, only fetch newer items
                        updatedAfter?.let { timestamp ->
                            gte("updated_at", timestamp)
                        }
                    }
                }

            val items = query.decodeList<SupabaseInventoryItemDto>()
            Log.d(TAG, "✅ Fetched ${items.size} items")
            Result.success(items)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to fetch items", e)
            Result.failure(e)
        }
    }

    /**
     * Insert new item to Supabase
     * Returns inserted item with server-generated ID
     *
     * @param item Item to insert (id should be null)
     * @return Inserted item with ID or null on error
     */
    suspend fun insertItem(item: SupabaseInventoryItemDto): Result<SupabaseInventoryItemDto> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Inserting item: ${item.itemName}")

                val response = supabaseClient.from(TABLE_NAME)
                    .insert(item) {
                        select()
                    }

                val insertedItem = response.decodeSingle<SupabaseInventoryItemDto>()
                Log.d(TAG, "✅ Inserted item with ID: ${insertedItem.id}")
                Result.success(insertedItem)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to insert item: ${item.itemName}", e)
                Result.failure(e)
            }
        }

    /**
     * Update existing item in Supabase
     * Uses server ID to identify item
     *
     * @param item Item to update (must have id)
     * @return Updated item or null on error
     */
    suspend fun updateItem(item: SupabaseInventoryItemDto): Result<SupabaseInventoryItemDto> =
        withContext(Dispatchers.IO) {
            try {
                require(item.id != null) { "Item must have ID for update" }
                Log.d(TAG, "Updating item: ${item.id} - ${item.itemName}")

                val response = supabaseClient.from(TABLE_NAME)
                    .update(item) {
                        filter {
                            eq("id", item.id)
                        }
                        select()
                    }

                val updatedItem = response.decodeSingle<SupabaseInventoryItemDto>()
                Log.d(TAG, "✅ Updated item: ${updatedItem.id}")
                Result.success(updatedItem)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to update item: ${item.id}", e)
                Result.failure(e)
            }
        }

    /**
     * Soft delete item (set is_active = false)
     * Does not physically delete the record
     *
     * @param supabaseId Server ID of item to delete
     * @return Success or failure
     */
    suspend fun deleteItem(supabaseId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Soft deleting item: $supabaseId")

            supabaseClient.from(TABLE_NAME)
                .update(mapOf("is_active" to false)) {
                    filter {
                        eq("id", supabaseId)
                    }
                }

            Log.d(TAG, "✅ Soft deleted item: $supabaseId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to delete item: $supabaseId", e)
            Result.failure(e)
        }
    }

    /**
     * Batch insert multiple items
     * More efficient than individual inserts
     *
     * @param items List of items to insert
     * @return List of inserted items with IDs
     */
    suspend fun batchInsert(items: List<SupabaseInventoryItemDto>): Result<List<SupabaseInventoryItemDto>> =
        withContext(Dispatchers.IO) {
            try {
                if (items.isEmpty()) return@withContext Result.success(emptyList())

                Log.d(TAG, "Batch inserting ${items.size} items")

                val response = supabaseClient.from(TABLE_NAME)
                    .insert(items) {
                        select()
                    }

                val insertedItems = response.decodeList<SupabaseInventoryItemDto>()
                Log.d(TAG, "✅ Batch inserted ${insertedItems.size} items")
                Result.success(insertedItems)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Batch insert failed", e)
                Result.failure(e)
            }
        }

    /**
     * Batch update multiple items
     * Updates each item individually (Supabase limitation)
     *
     * @param items List of items to update (must have IDs)
     * @return Number of successfully updated items
     */
    suspend fun batchUpdate(items: List<SupabaseInventoryItemDto>): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                if (items.isEmpty()) return@withContext Result.success(0)

                Log.d(TAG, "Batch updating ${items.size} items")

                var successCount = 0
                items.forEach { item ->
                    updateItem(item).onSuccess { successCount++ }
                }

                Log.d(TAG, "✅ Batch updated $successCount/${items.size} items")
                Result.success(successCount)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Batch update failed", e)
                Result.failure(e)
            }
        }

    /**
     * Check if item exists by ID
     * Useful for conflict detection
     */
    suspend fun itemExists(supabaseId: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = supabaseClient.from(TABLE_NAME)
                .select(Columns.Companion.raw("id")) {
                    filter {
                        eq("id", supabaseId)
                    }
                }

            val exists = response.decodeList<Map<String, Long>>().isNotEmpty()
            Result.success(exists)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to check if item exists: $supabaseId", e)
            Result.failure(e)
        }
    }
}