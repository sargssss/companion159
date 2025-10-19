package com.lifelover.companion159.data.remote.sync

import java.text.SimpleDateFormat
import java.util.*

/**
 * Centralized date formatting for sync operations
 * Ensures consistent ISO8601 format across all sync layers
 */
object SyncDateUtils {
    private val iso8601Format = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
        Locale.US
    ).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Format Date to ISO8601 string
     */
    fun formatForSync(date: Date?): String? {
        return date?.let { iso8601Format.format(it) }
    }

    /**
     * Parse ISO8601 string to Date
     */
    fun parseFromSync(dateString: String?): Date? {
        return try {
            dateString?.let { iso8601Format.parse(it) }
        } catch (e: Exception) {
            null
        }
    }
}