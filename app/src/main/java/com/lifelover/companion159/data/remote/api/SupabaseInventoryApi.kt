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

@Singleton
class SupabaseInventoryApi @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    companion object {
        private const val TAG = "SupabaseInventoryApi"
        private const val TABLE_NAME = "crew_inventory_items_duplicate"
    }

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
     * Update existing item in Supabase
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
     * Batch insert multiple items
     * FIX: Create map manually to ensure tenant_id is included
     */
    suspend fun batchInsert(items: List<SupabaseInventoryItemDto>): Result<List<SupabaseInventoryItemDto>> =
        withContext(Dispatchers.IO) {
            try {
                if (items.isEmpty()) return@withContext Result.success(emptyList())

                Log.d(TAG, "Batch inserting ${items.size} items")

                // ✅ Convert to Map manually to ensure all fields are included
                val itemMaps = items.map { item ->
                    buildMap {
                        put("tenant_id", 9999)
                        put("crew_name", item.crewName)
                        put("item_name", item.itemName)
                        put("available_quantity", item.availableQuantity)
                        put("needed_quantity", item.neededQuantity)
                        put("item_category", item.itemCategory)
                        put("unit", item.unit)
                        put("priority", item.priority)
                        put("crew_type", item.crewType)
                        put("description", item.description)
                        put("notes", item.notes)
                        put("last_need_updated_at", item.lastNeedUpdatedAt)
                        put("needed_by", item.neededBy)
                        put("created_by", item.createdBy)
                        put("updated_by", item.updatedBy)
                        put("metadata", item.metadata ?: emptyMap<String, String>())
                        put("is_active", item.isActive)
                        item.createdAt?.let { put("created_at", it) }
                        item.updatedAt?.let { put("updated_at", it) }
                    }
                }

                val response = supabaseClient.from(TABLE_NAME)
                    .insert(itemMaps) {
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
}