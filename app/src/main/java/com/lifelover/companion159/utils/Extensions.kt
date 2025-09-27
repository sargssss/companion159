package com.lifelover.companion159.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

// Для більш сучасного підходу з java.time (API 26+)
@RequiresApi(Build.VERSION_CODES.O)
fun parseApiTimestampModern(timestamp: String): Date {
    return try {
        when {
            timestamp.contains("T") -> {
                // ISO 8601 формат
                val instant = Instant.parse(timestamp)
                Date.from(instant)
            }
            timestamp.matches(Regex("\\d{10}")) -> {
                // Unix timestamp в секундах
                val instant = Instant.ofEpochSecond(timestamp.toLong())
                Date.from(instant)
            }
            timestamp.matches(Regex("\\d{13}")) -> {
                // Unix timestamp в мілісекундах
                val instant = Instant.ofEpochMilli(timestamp.toLong())
                Date.from(instant)
            }
            else -> Date()
        }
    } catch (e: Exception) {
        Date()
    }
}

// Парсинг ISO 8601 timestamp з сервера
fun parseApiTimestamp(timestamp: String): Date {
    return try {
        when {
            timestamp.contains("T") && timestamp.endsWith("Z") -> {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                format.timeZone = TimeZone.getTimeZone("UTC")
                format.parse(timestamp) ?: Date()
            }
            timestamp.contains("T") && timestamp.endsWith("Z") && !timestamp.contains(".") -> {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                format.timeZone = TimeZone.getTimeZone("UTC")
                format.parse(timestamp) ?: Date()
            }
            timestamp.matches(Regex("\\d{10}")) -> {
                Date(timestamp.toLong() * 1000)
            }
            timestamp.matches(Regex("\\d{13}")) -> {
                Date(timestamp.toLong())
            }
            else -> {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                format.timeZone = TimeZone.getTimeZone("UTC")
                format.parse(timestamp) ?: Date()
            }
        }
    } catch (e: Exception) {
        Date()
    }
}

// Конвертація Date в API формат
fun formatDateForApi(date: Date): String {
    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(date)
}