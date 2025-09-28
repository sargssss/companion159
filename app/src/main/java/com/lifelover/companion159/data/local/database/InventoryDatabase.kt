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
    override fun migrate(database: SupportSQLiteDatabase) {
        // Додаємо нову колонку supabaseId
        database.execSQL("ALTER TABLE inventory_items ADD COLUMN supabaseId TEXT")

        // Копіюємо дані з serverId в supabaseId (якщо потрібно)
        database.execSQL("UPDATE inventory_items SET supabaseId = serverId WHERE serverId IS NOT NULL")

        // Видаляємо стару колонку serverId (опціонально)
        // database.execSQL("ALTER TABLE inventory_items DROP COLUMN serverId")
    }
}

@Database(
    entities = [InventoryItemEntity::class],
    version = 2, // Збільшена версія
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
                    .addMigrations(MIGRATION_1_2) // Додаємо міграцію
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}