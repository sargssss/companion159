package com.lifelover.companion159.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifelover.companion159.data.local.entities.PreferencesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PreferencesDao {
    @Query("SELECT * FROM preferences WHERE id = 1")
    fun getPreferences(): Flow<PreferencesEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreferences(prefs: PreferencesEntity)

    @Query("UPDATE preferences SET position = :position, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updatePosition(position: String?, updatedAt: Long = System.currentTimeMillis())
}