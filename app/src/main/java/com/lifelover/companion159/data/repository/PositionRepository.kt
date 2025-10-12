package com.lifelover.companion159.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class PositionRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "position_prefs"
        private const val KEY_POSITION = "user_position"

        // Predefined positions for autocomplete
        val PREDEFINED_POSITIONS = listOf("Барі", "Редбул", "Одеса")
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentPosition = MutableStateFlow<String?>(getCurrentPositionFromPrefs())
    val currentPosition: StateFlow<String?> = _currentPosition.asStateFlow()

    /**
     * Get current position from SharedPreferences
     */
    private fun getCurrentPositionFromPrefs(): String? {
        return prefs.getString(KEY_POSITION, null)
    }

    /**
     * Get current position synchronously
     */
    fun getPosition(): String? {
        return _currentPosition.value
    }

    /**
     * Save position to SharedPreferences
     */
    fun savePosition(position: String) {
        prefs.edit { putString(KEY_POSITION, position.trim()) }
        _currentPosition.value = position.trim()
    }

    /**
     * Check if position is set
     */
    fun isPositionSet(): Boolean {
        return !getPosition().isNullOrBlank()
    }

    /**
     * Get autocomplete suggestions based on input
     */
    fun getAutocompleteSuggestions(input: String): List<String> {
        if (input.isBlank()) return PREDEFINED_POSITIONS

        return PREDEFINED_POSITIONS.filter {
            it.startsWith(input, ignoreCase = true)
        }
    }
}