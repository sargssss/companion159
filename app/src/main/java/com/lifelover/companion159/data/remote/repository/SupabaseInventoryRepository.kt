package com.lifelover.companion159.data.remote.repository

import android.util.Log
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.data.remote.client.SupabaseClient
import com.lifelover.companion159.data.remote.models.CrewInventoryItem
import com.lifelover.companion159.data.sync.toCrewInventoryItem
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseInventoryRepository @Inject constructor() {

    companion object {
        private const val TAG = "SupabaseInventoryRepo"
        private const val TABLE_NAME = "crew_inventory_items_duplicate"
    }

    private val client = SupabaseClient.client

    suspend fun getAllItems(crewName: String): List<CrewInventoryItem> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching items for crew: $crewName")

                val items = client.from(TABLE_NAME)
                    .select(columns = Columns.ALL) {
                        filter {
                            //eq("tenant_id", 0)  // FIXED: snake_case
                            eq("crew_name", crewName)
                            eq("is_active", true)
                        }
                    }
                    .decodeList<CrewInventoryItem>()

                items
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error fetching items", e)
                emptyList()
            }
        }

    suspend fun createItem(
        localItem: InventoryItemEntity
    ): Long? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating item: ${localItem.itemName} for crew: ${localItem.crewName}")

            val crewItem = localItem.toCrewInventoryItem()

            val createdItems = client.from(TABLE_NAME)
                .insert(crewItem) {
                    select()
                }
                .decodeList<CrewInventoryItem>()

            val serverId = createdItems.firstOrNull()?.id

            if (serverId != null) {
                Log.d(TAG, "✅ Item created with server ID: $serverId")
            } else {
                Log.e(TAG, "❌ Item created but no ID returned")
            }

            serverId
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating item", e)
            null
        }
    }

    suspend fun updateItem(
        supabaseId: Long,
        localItem: InventoryItemEntity
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Updating item with server ID: $supabaseId")

            val updatedItems = client.from(TABLE_NAME)
                .update({
                    set("item_name", localItem.itemName)
                    set("available_quantity", localItem.availableQuantity)
                    set("needed_quantity", localItem.neededQuantity)
                    set("crew_name", localItem.crewName)
                    set("is_active", localItem.isActive)
                }) {
                    filter {
                        eq("id", supabaseId)
                        //eq("tenant_id", 0)
                    }
                    select()
                }
                .decodeList<CrewInventoryItem>()

            val success = updatedItems.isNotEmpty()

            if (success) {
                Log.d(TAG, "✅ Item updated successfully")
            } else {
                Log.e(TAG, "❌ No items updated")
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating item", e)
            false
        }
    }

    suspend fun deleteItem(supabaseId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val deletedItems = client.from(TABLE_NAME)
                .update({
                    set("is_active", false)
                }) {
                    filter {
                        eq("id", supabaseId)
                        eq("tenant_id", 0)
                    }
                    select()
                }
                .decodeList<CrewInventoryItem>()

            val success = deletedItems.isNotEmpty()

            if (success) {
                Log.d(TAG, "✅ Item deleted successfully")
            } else {
                Log.e(TAG, "❌ No items deleted")
            }

            success
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting item", e)
            false
        }
    }
}