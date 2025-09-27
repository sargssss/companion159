package com.lifelover.companion159.data.remote.api

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import com.lifelover.companion159.data.remote.dto.*
import io.github.jan.supabase.SupabaseClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryApiService @Inject constructor() {

    private val client = SupabaseClient.client

    // ═══════════════════════════════════════════════════════════════════
    // CRUD операції
    // ═══════════════════════════════════════════════════════════════════

    suspend fun getAllItems(): List<InventoryItemDto> {
        return client.from("inventory_items")
            .select()
            .decodeList<InventoryItemDto>()
    }

    suspend fun getItemsByCategory(category: String): List<InventoryItemDto> {
        return client.from("inventory_items")
            .select()
            .eq("category", category)
            .order("updated_at", ascending = false)
            .decodeList<InventoryItemDto>()
    }

    suspend fun getItemById(id: Long): InventoryItemDto? {
        return client.from("inventory_items")
            .select()
            .eq("id", id)
            .decodeSingleOrNull<InventoryItemDto>()
    }

    suspend fun createItem(item: InventoryItemCreateDto): InventoryItemDto {
        return client.from("inventory_items")
            .insert(item)
            .decodeSingle<InventoryItemDto>()
    }

    suspend fun updateItem(id: Long, item: InventoryItemUpdateDto): InventoryItemDto {
        return client.from("inventory_items")
            .update(item)
            .eq("id", id)
            .decodeSingle<InventoryItemDto>()
    }

    suspend fun deleteItem(id: Long) {
        client.from("inventory_items")
            .delete()
            .eq("id", id)
    }

    // ═══════════════════════════════════════════════════════════════════
    // Додаткові методи
    // ═══════════════════════════════════════════════════════════════════

    suspend fun searchItems(query: String, category: String? = null): List<InventoryItemDto> {
        var request = client.from("inventory_items")
            .select()
            .ilike("name", "%$query%")

        if (category != null) {
            request = request.eq("category", category)
        }

        return request
            .order("updated_at", ascending = false)
            .decodeList<InventoryItemDto>()
    }

    suspend fun getInventoryStats(): List<InventoryStatsDto> {
        return client.from("get_inventory_stats")
            .select()
            .decodeList<InventoryStatsDto>()
    }

    // ═══════════════════════════════════════════════════════════════════
    // Синхронізація (отримання оновлень з певного часу)
    // ═══════════════════════════════════════════════════════════════════

    suspend fun getUpdatedSince(timestamp: String): List<InventoryItemDto> {
        return client.from("inventory_items")
            .select()
            .gte("updated_at", timestamp)
            .order("updated_at", ascending = false)
            .decodeList<InventoryItemDto>()
    }

    // ═══════════════════════════════════════════════════════════════════
    // Batch операції
    // ═══════════════════════════════════════════════════════════════════

    suspend fun createMultipleItems(items: List<InventoryItemCreateDto>): List<InventoryItemDto> {
        return client.from("inventory_items")
            .insert(items)
            .decodeList<InventoryItemDto>()
    }

    suspend fun updateQuantities(updates: Map<Long, Int>) {
        updates.forEach { (id, quantity) ->
            client.from("inventory_items")
                .update(mapOf("quantity" to quantity))
                .eq("id", id)
        }
    }
}