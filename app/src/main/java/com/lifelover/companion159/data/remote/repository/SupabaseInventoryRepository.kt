package com.lifelover.companion159.data.remote.repository

import android.util.Log
import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.data.remote.client.SupabaseClient
import com.lifelover.companion159.data.remote.config.SupabaseConfig
import com.lifelover.companion159.data.remote.models.SupabaseInventoryItem
import io.github.jan.supabase.auth.auth
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
    }

    private val client = SupabaseClient.client

    // Отримати всі елементи користувача
    suspend fun getAllItems(): List<SupabaseInventoryItem> = withContext(Dispatchers.IO) {
        try {
            val userId = client.auth.currentUserOrNull()?.id ?: return@withContext emptyList()
            Log.d(TAG, "Fetching all items for user: $userId")

            val items = client.from(SupabaseConfig.TABLE_INVENTORY)
                .select(columns = Columns.ALL) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<SupabaseInventoryItem>()

            Log.d(TAG, "Fetched ${items.size} items from server")
            items
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all items", e)
            emptyList()
        }
    }

    // СТВОРИТИ новий елемент - КЛЮЧОВЕ: використовуємо .select() для отримання ID
    suspend fun createItem(localItem: InventoryItemEntity): String? = withContext(Dispatchers.IO) {
        try {
            val userId = client.auth.currentUserOrNull()?.id ?: return@withContext null
            Log.d(TAG, "Creating NEW item: ${localItem.name}")

            val supabaseItem = SupabaseInventoryItem(
                id = null, // Supabase згенерує UUID автоматично
                name = localItem.name,
                quantity = localItem.quantity,
                category = localItem.category.name.lowercase(),
                userId = userId,
                isDeleted = localItem.isDeleted
            )

            // КЛЮЧОВЕ ВИПРАВЛЕННЯ: Використовуємо .select() для отримання створеного запису з ID
            val createdItems = client.from(SupabaseConfig.TABLE_INVENTORY)
                .insert(supabaseItem) {
                    select()
                }
                .decodeList<SupabaseInventoryItem>()

            val createdItem = createdItems.firstOrNull()
            if (createdItem?.id != null) {
                Log.d(TAG, "✅ Successfully CREATED item: ${createdItem.name} with Supabase ID: ${createdItem.id}")
                createdItem.id
            } else {
                Log.e(TAG, "❌ Failed to get ID for created item: ${localItem.name}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error creating item: ${localItem.name}", e)
            null
        }
    }

    // ОНОВИТИ існуючий елемент
    suspend fun updateItem(supabaseId: String, localItem: InventoryItemEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val userId = client.auth.currentUserOrNull()?.id ?: return@withContext false
            Log.d(TAG, "UPDATING existing item with Supabase ID: $supabaseId, name: ${localItem.name}")

            val updatedItems = client.from(SupabaseConfig.TABLE_INVENTORY)
                .update({
                    set("name", localItem.name)
                    set("quantity", localItem.quantity)
                    set("category", localItem.category.name.lowercase())
                    set("is_deleted", localItem.isDeleted)
                }) {
                    filter {
                        eq("id", supabaseId)
                        eq("user_id", userId)
                    }
                    select() // Отримуємо оновлений запис
                }
                .decodeList<SupabaseInventoryItem>()

            if (updatedItems.isNotEmpty()) {
                Log.d(TAG, "✅ Successfully UPDATED item: ${localItem.name}")
                true
            } else {
                Log.e(TAG, "❌ No items updated for Supabase ID: $supabaseId")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating item with Supabase ID: $supabaseId", e)
            false
        }
    }

    // ВИДАЛИТИ елемент (м'яке видалення)
    suspend fun deleteItem(supabaseId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val userId = client.auth.currentUserOrNull()?.id ?: return@withContext false
            Log.d(TAG, "DELETING item with Supabase ID: $supabaseId")

            val deletedItems = client.from(SupabaseConfig.TABLE_INVENTORY)
                .update({
                    set("is_deleted", true)
                }) {
                    filter {
                        eq("id", supabaseId)
                        eq("user_id", userId)
                    }
                    select() // Отримуємо видалений запис для підтвердження
                }
                .decodeList<SupabaseInventoryItem>()

            if (deletedItems.isNotEmpty()) {
                Log.d(TAG, "✅ Successfully DELETED item with Supabase ID: $supabaseId")
                true
            } else {
                Log.e(TAG, "❌ No items deleted for Supabase ID: $supabaseId")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error deleting item with Supabase ID: $supabaseId", e)
            false
        }
    }
}

// Extension функції для конвертації
fun SupabaseInventoryItem.toEntity(): InventoryItemEntity {
    return InventoryItemEntity(
        id = 0, // Room згенерує новий локальний ID
        name = name,
        quantity = quantity,
        category = InventoryCategory.valueOf(category.uppercase()),
        supabaseId = id, // КЛЮЧОВЕ: зберігаємо Supabase UUID
        lastModified = java.util.Date(),
        lastSynced = java.util.Date(),
        needsSync = false,
        isDeleted = isDeleted
    )
}