package com.lifelover.companion159.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lifelover.companion159.data.local.entities.Converters
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.dao.PreferencesDao
import com.lifelover.companion159.data.local.dao.SyncDao
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.data.local.entities.PreferencesEntity

@Database(
    entities = [
        InventoryItemEntity::class,
        PreferencesEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class InventoryDatabase : RoomDatabase() {

    abstract fun inventoryDao(): InventoryDao
    abstract fun preferencesDao(): PreferencesDao
    abstract fun syncDao(): SyncDao

    companion object {
        @Volatile
        private var INSTANCE: InventoryDatabase? = null

        /**
         * Migration from version 1 to 2
         * Removes sync_queue table as we no longer use queue-based sync
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Drop sync_queue table - no longer needed
                db.execSQL("DROP TABLE IF EXISTS sync_queue")
            }
        }

        fun getDatabase(context: Context): InventoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InventoryDatabase::class.java,
                    "companion159_inventory_database"
                )
                    .addMigrations(MIGRATION_1_2) // Add migration instead of fallback
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}