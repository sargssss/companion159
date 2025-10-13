package com.lifelover.companion159.data.repository

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import com.lifelover.companion159.data.local.dao.PreferencesDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Singleton
class PositionRepository @Inject constructor(
    private val preferencesDao: PreferencesDao
) {
    companion object {
        val PREDEFINED_POSITIONS = listOf("Барі", "Редбул", "Одеса")
    }

    val currentPosition: StateFlow<String?> = preferencesDao.getPreferences()
        .map { it?.position }
        .stateIn(
            scope = CoroutineScope(Dispatchers.IO),
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    fun getPosition(): String? = currentPosition.value

    suspend fun savePosition(position: String) {
        preferencesDao.updatePosition(position.trim())
    }

    fun getAutocompleteSuggestions(input: String): List<String> {
        if (input.isBlank()) return PREDEFINED_POSITIONS
        return PREDEFINED_POSITIONS.filter {
            it.startsWith(input, ignoreCase = true)
        }
    }
}