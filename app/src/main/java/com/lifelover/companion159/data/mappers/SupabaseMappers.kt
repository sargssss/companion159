package com.lifelover.companion159.data.mappers

import com.lifelover.companion159.data.local.entities.InventoryCategory
import com.lifelover.companion159.data.remote.dto.*
import com.lifelover.companion159.domain.models.InventoryItem
import java.time.Instant
import java.time.ZoneId
import java.util.Date

// ═══════════════════════════════════════════════════════════════════
// API DTO → Domain Model
// ═══════════════════════════════════════════════════════════════════

fun InventoryItemDto.toDomain(): InventoryItem {
    return InventoryItem(
        id = id ?: 0,
        name = name,
        quantity = quantity,
        category = category.toDomainCategory(),
        lastModified = updatedAt?.let { parseSupabaseTimestamp(it) } ?: Date(),
        isSynced = true // Якщо прийшло з API, то синхронізовано
    )
}

fun InventoryStatsDto.toDomain(): InventoryStats {
    return InventoryStats(
        category = category.toDomainCategory(),
        totalItems = totalItems.toInt(),
        totalQuantity = totalQuantity.toInt()
    )
}

// ═══════════════════════════════════════════════════════════════════
// Domain Model → API DTO
// ═══════════════════════════════════════════════════════════════════

fun InventoryItem.toCreateDto(): InventoryItemCreateDto {
    return InventoryItemCreateDto(
        name = name,
        quantity = quantity,
        category = category.toApiString()
    )
}

fun InventoryItem.toUpdateDto(): InventoryItemUpdateDto {
    return InventoryItemUpdateDto(
        name = name,
        quantity = quantity,
        category = category.toApiString()
    )
}

// ═══════════════════════════════════════════════════════════════════
// Мапери для категорій
// ═══════════════════════════════════════════════════════════════════

fun String.toDomainCategory(): InventoryCategory {
    return when (this.uppercase()) {
        "SHIPS" -> InventoryCategory.SHIPS
        "AMMUNITION" -> InventoryCategory.AMMUNITION
        "EQUIPMENT" -> InventoryCategory.EQUIPMENT
        "PROVISIONS" -> InventoryCategory.PROVISIONS
        else -> throw IllegalArgumentException("Unknown category: $this")
    }
}

fun InventoryCategory.toApiString(): String {
    return when (this) {
        InventoryCategory.SHIPS -> "SHIPS"
        InventoryCategory.AMMUNITION -> "AMMUNITION"
        InventoryCategory.EQUIPMENT -> "EQUIPMENT"
        InventoryCategory.PROVISIONS -> "PROVISIONS"
    }
}

// ═══════════════════════════════════════════════════════════════════
// Утиліти для роботи з часом
// ═══════════════════════════════════════════════════════════════════

/**
 * Парсинг Supabase timestamp (ISO 8601 з timezone)
 * Приклад: "2023-12-25T14:30:00.000000+00:00"
 */
fun parseSupabaseTimestamp(timestamp: String): Date {
    return try {
        val instant = Instant.parse(timestamp)
        Date.from(instant)
    } catch (e: Exception) {
        // Fallback на поточний час у разі помилки парсингу
        Date()
    }
}

/**
 * Форматування Date в Supabase timestamp
 */
fun Date.toSupabaseTimestamp(): String {
    val instant = this.toInstant()
    return instant.toString()
}

/**
 * Отримання поточного часу в Supabase форматі
 */
fun getCurrentSupabaseTimestamp(): String {
    return Instant.now().toString()
}

// ═══════════════════════════════════════════════════════════════════
// Додаткові domain models для статистики
// ═══════════════════════════════════════════════════════════════════

data class InventoryStats(
    val category: InventoryCategory,
    val totalItems: Int,
    val totalQuantity: Int
)

// ═══════════════════════════════════════════════════════════════════
// Приклади використання мапперів
// ═══════════════════════════════════════════════════════════════════

/**
 * В Repository при отриманні даних з API:
 *
 * suspend fun getItemsByCategory(category: InventoryCategory): List<InventoryItem> {
 *     val dtos = apiService.getItemsByCategory(category.toApiString())
 *     return dtos.map { it.toDomain() }
 * }
 */

/**
 * В Repository при створенні нового item:
 *
 * suspend fun createItem(item: InventoryItem): InventoryItem {
 *     val createDto = item.toCreateDto()
 *     val responseDto = apiService.createItem(createDto)
 *     return responseDto.toDomain()
 * }
 */

/**
 * В Repository при оновленні item:
 *
 * suspend fun updateItem(item: InventoryItem): InventoryItem {
 *     val updateDto = item.toUpdateDto()
 *     val responseDto = apiService.updateItem(item.id, updateDto)
 *     return responseDto.toDomain()
 * }
 */

// ═══════════════════════════════════════════════════════════════════
// Обробка помилок API
// ═══════════════════════════════════════════════════════════════════

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: String? = null) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

// Mapper для обробки результатів API
inline fun <T> safeApiCall(apiCall: () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(apiCall())
    } catch (e: Exception) {
        ApiResult.Error(
            message = e.message ?: "Unknown error occurred",
            code = e::class.simpleName
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
// Розширені mapper'и для синхронізації
// ═══════════════════════════════════════════════════════════════════

/**
 * Порівняння локального item з серверним для синхронізації
 */
fun InventoryItem.needsSync(serverItem: InventoryItemDto?): Boolean {
    if (serverItem == null) return true // Потрібно створити на сервері

    val serverModified = parseSupabaseTimestamp(serverItem.updatedAt ?: "")
    return lastModified.after(serverModified) // Локальна версія новіша
}

/**
 * Злиття локального item з серверним (conflict resolution)
 */
fun InventoryItem.mergeWithServer(serverItem: InventoryItemDto): InventoryItem {
    val serverModified = parseSupabaseTimestamp(serverItem.updatedAt ?: "")

    return if (lastModified.after(serverModified)) {
        // Локальна версія новіша - залишаємо локальні дані
        this.copy(id = serverItem.id ?: id, isSynced = false)
    } else {
        // Серверна версія новіша - використовуємо серверні дані
        serverItem.toDomain()
    }
}