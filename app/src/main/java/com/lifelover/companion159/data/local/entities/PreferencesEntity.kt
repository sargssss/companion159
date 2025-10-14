package com.lifelover.companion159.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "preferences")
data class PreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val position: String? = null
)