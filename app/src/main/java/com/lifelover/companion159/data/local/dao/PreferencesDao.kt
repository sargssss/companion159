package com.lifelover.companion159.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.lifelover.companion159.data.local.entities.PreferencesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PreferencesDao {
    @Query("SELECT * FROM preferences WHERE id = 1 LIMIT 1")
    fun getPreferences(): Flow<PreferencesEntity?>

    @Query("SELECT * FROM preferences WHERE id = 1 LIMIT 1")
    suspend fun getPreferencesOnce(): PreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePreferences(preferences: PreferencesEntity)
}