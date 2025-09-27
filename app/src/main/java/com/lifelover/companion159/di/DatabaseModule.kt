package com.lifelover.companion159.di

import android.content.Context
import androidx.room.Room
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.database.InventoryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): InventoryDatabase {
        return Room.databaseBuilder(
            context,
            InventoryDatabase::class.java,
            "inventory_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideDao(database: InventoryDatabase): InventoryDao = database.inventoryDao()
}