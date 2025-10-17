package com.lifelover.companion159.data.repository

import android.util.Log
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import com.lifelover.companion159.data.local.dao.PreferencesDao
import com.lifelover.companion159.data.local.entities.PreferencesEntity
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

@Singleton
class PositionRepository @Inject constructor(
    private val preferencesDao: PreferencesDao,
    private val authService: SupabaseAuthService
) {
    companion object {
        const val TAG = "PositionRepository"
        val PREDEFINED_POSITIONS = listOf(
            "Барі",
            "Редбул",
            "Одеса"
        )
    }

    val currentPosition: StateFlow<String?> = preferencesDao.getPreferences()
        .map { it?.position }
        .stateIn(
            scope = CoroutineScope(Dispatchers.IO),
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    fun getPosition(): String? = currentPosition.value

    /**
     * Save selected position
     * Also updates Supabase user metadata with crew_name
     */
    suspend fun savePosition(position: String) {
        withContext(Dispatchers.IO) {
            try {
                // Save to local database
                val entity = PreferencesEntity(
                    id = 1,
                    position = position
                )
                preferencesDao.insertOrUpdatePreferences(entity)

                // Update Supabase metadata
                authService.updateUserCrewName(position).fold(
                    onSuccess = {
                        Log.d(TAG, "✅ Position saved: $position (local + Supabase)")
                    },
                    onFailure = { error ->
                        // Don't fail if Supabase update fails - local save is more important
                        Log.e(TAG, "⚠️ Position saved locally but Supabase update failed", error)
                    }
                )

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to save position", e)
                throw e
            }
        }
    }

    fun getAutocompleteSuggestions(input: String): List<String> {
        if (input.isBlank()) return PREDEFINED_POSITIONS

        return PREDEFINED_POSITIONS.filter {
            it.startsWith(input, ignoreCase = true)
        }
    }
}