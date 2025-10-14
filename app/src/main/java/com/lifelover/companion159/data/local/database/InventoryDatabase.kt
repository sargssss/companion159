package com.lifelover.companion159.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lifelover.companion159.data.local.entities.Converters
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.dao.PreferencesDao
import com.lifelover.companion159.data.local.dao.SyncQueueDao
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.data.local.entities.PreferencesEntity
import com.lifelover.companion159.data.local.entities.SyncQueueEntity

@Database(
    entities = [
        InventoryItemEntity::class,
        PreferencesEntity::class,
        SyncQueueEntity::class
    ],
    version = 1,  // increment version
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class InventoryDatabase : RoomDatabase() {

    abstract fun inventoryDao(): InventoryDao

    abstract fun preferencesDao(): PreferencesDao

    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: InventoryDatabase? = null

        fun getDatabase(context: Context): InventoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InventoryDatabase::class.java,
                    "companion159_inventory_database"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}