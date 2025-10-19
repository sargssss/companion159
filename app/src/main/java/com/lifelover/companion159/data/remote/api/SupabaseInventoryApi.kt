// data/remote/api/SupabaseInventoryApi.kt
package com.lifelover.companion159.data.remote.api

import android.util.Log
import com.lifelover.companion159.data.remote.dto.SupabaseInventoryItemDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseInventoryApi @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    companion object {
        private const val TAG = "SupabaseInventoryApi"
        private const val TABLE_NAME = "crew_inventory_items_duplicate"
    }

    /**
     * Fetch items from server
     */
    suspend fun fetchItemsByCrewName(
        crewName: String,
        updatedAfter: String? = null
    ): Result<List<SupabaseInventoryItemDto>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching items for crew: $crewName")

            val query = supabaseClient.from(TABLE_NAME)
                .select {
                    filter {
                        eq("crew_name", crewName)
                        eq("is_active", true)

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
     * Insert single item
     */
    suspend fun insertSingleItem(item: SupabaseInventoryItemDto): Result<SupabaseInventoryItemDto> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Inserting item: ${item.itemName}")

                val response = supabaseClient.from(TABLE_NAME)
                    .insert(item) {
                        select()
                    }

                val inserted = response.decodeSingle<SupabaseInventoryItemDto>()
                Log.d(TAG, "✅ Inserted with ID: ${inserted.id}")
                Result.success(inserted)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Insert failed", e)
                Result.failure(e)
            }
        }

    /**
     * Update single item
     */
    suspend fun updateSingleItem(item: SupabaseInventoryItemDto): Result<SupabaseInventoryItemDto> =
        withContext(Dispatchers.IO) {
            try {
                require(item.id != null) { "Item must have ID for update" }
                Log.d(TAG, "Updating item: ${item.id}")

                val response = supabaseClient.from(TABLE_NAME)
                    .update(item) {
                        filter {
                            eq("id", item.id)
                        }
                        select()
                    }

                val updated = response.decodeSingle<SupabaseInventoryItemDto>()
                Log.d(TAG, "✅ Updated item: ${updated.id}")
                Result.success(updated)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Update failed", e)
                Result.failure(e)
            }
        }
}