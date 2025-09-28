package com.lifelover.companion159.data.remote.repository

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

    private val client = SupabaseClient.client

    // Отримати всі елементи користувача
    suspend fun getAllItems(): List<SupabaseInventoryItem> = withContext(Dispatchers.IO) {
        try {
            val userId = client.auth.currentUserOrNull()?.id ?: return@withContext emptyList()

            client.from(SupabaseConfig.TABLE_INVENTORY)
                .select(columns = Columns.ALL) {
                    filter {
                        eq("user_id", userId)
                        eq("is_deleted", false)
                    }
                }
                .decodeList<SupabaseInventoryItem>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Отримати елементи за категорією
    suspend fun getItemsByCategory(category: InventoryCategory): List<SupabaseInventoryItem> =
        withContext(Dispatchers.IO) {
            try {
                val userId = client.auth.currentUserOrNull()?.id ?: return@withContext emptyList()

                client.from(SupabaseConfig.TABLE_INVENTORY)
                    .select(columns = Columns.ALL) {
                        filter {
                            eq("user_id", userId)
                            eq("category", category.name.lowercase())
                            eq("is_deleted", false)
                        }
                    }
                    .decodeList<SupabaseInventoryItem>()
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }

    // Створити новий елемент
    suspend fun createItem(item: SupabaseInventoryItem): SupabaseInventoryItem? =
        withContext(Dispatchers.IO) {
            try {
                val userId = client.auth.currentUserOrNull()?.id ?: return@withContext null
                val itemWithUser = item.copy(userId = userId)

                client.from(SupabaseConfig.TABLE_INVENTORY)
                    .insert(itemWithUser)
                    .decodeSingle<SupabaseInventoryItem>()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    // Оновити елемент
    suspend fun updateItem(id: String, updates: Map<String, Any>): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val userId = client.auth.currentUserOrNull()?.id ?: return@withContext false

                client.from(SupabaseConfig.TABLE_INVENTORY)
                    .update(updates) {
                        filter {
                            eq("id", id)
                            eq("user_id", userId)
                        }
                    }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    // М'яке видалення елемента
    suspend fun softDeleteItem(id: String): Boolean =
        withContext(Dispatchers.IO) {
            updateItem(id, mapOf("is_deleted" to true))
        }

    // Синхронізація пакету елементів
    suspend fun syncItems(items: List<SupabaseInventoryItem>): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val userId = client.auth.currentUserOrNull()?.id ?: return@withContext false
                val itemsWithUser = items.map { it.copy(userId = userId) }

                client.from(SupabaseConfig.TABLE_INVENTORY)
                    .upsert(itemsWithUser)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
}

// Extension функції для конвертації
fun InventoryItemEntity.toSupabaseModel(): SupabaseInventoryItem {
    return SupabaseInventoryItem(
        id = serverId,
        name = name,
        quantity = quantity,
        category = category.name.lowercase(),
        isDeleted = isDeleted
    )
}

fun SupabaseInventoryItem.toEntity(): InventoryItemEntity {
    return InventoryItemEntity(
        id = 0, // Room згенерує новий локальний ID
        name = name,
        quantity = quantity,
        category = InventoryCategory.valueOf(category.uppercase()),
        serverId = id,
        lastModified = java.util.Date(),
        lastSynced = java.util.Date(),
        needsSync = false,
        isDeleted = isDeleted
    )
}