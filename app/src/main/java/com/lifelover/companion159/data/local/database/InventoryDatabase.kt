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
import com.lifelover.companion159.data.local.entities.InventoryItemEntity

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE inventory_items ADD COLUMN supabaseId TEXT")
        db.execSQL("UPDATE inventory_items SET supabaseId = serverId WHERE serverId IS NOT NULL")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE inventory_items ADD COLUMN userId TEXT")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE inventory_items ADD COLUMN createdAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
    }
}

// NEW: Migration 4->5 for position field
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE inventory_items ADD COLUMN position TEXT")
    }
}
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Step 1: Rename columns to match new schema
        db.execSQL("ALTER TABLE inventory_items RENAME COLUMN name TO itemName")
        db.execSQL("ALTER TABLE inventory_items RENAME COLUMN quantity TO availableQuantity")
        db.execSQL("ALTER TABLE inventory_items RENAME COLUMN position TO crewName")

        // Step 2: Change isDeleted to isActive with inverted logic
        db.execSQL("ALTER TABLE inventory_items ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
        db.execSQL("UPDATE inventory_items SET isActive = CASE WHEN isDeleted = 1 THEN 0 ELSE 1 END")
        // Keep isDeleted for now for compatibility, will remove later

        // Step 3: Change supabaseId from TEXT to INTEGER
        db.execSQL("ALTER TABLE inventory_items ADD COLUMN supabaseIdNew INTEGER")
        // Try to convert existing UUIDs to NULL (can't convert UUID to number)
        // Old data will need to be re-synced
        db.execSQL("ALTER TABLE inventory_items DROP COLUMN supabaseId")
        db.execSQL("ALTER TABLE inventory_items RENAME COLUMN supabaseIdNew TO supabaseId")

        // Step 4: Create indices for new column names
        db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_items_crewName ON inventory_items(crewName)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_items_isActive ON inventory_items(isActive)")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add neededQuantity column with default value 0
        db.execSQL("ALTER TABLE inventory_items ADD COLUMN neededQuantity INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [InventoryItemEntity::class],
    version = 7,  // CHANGED: increment version
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class InventoryDatabase : RoomDatabase() {
    abstract fun inventoryDao(): InventoryDao

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
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7  // NEW: Add migration
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}