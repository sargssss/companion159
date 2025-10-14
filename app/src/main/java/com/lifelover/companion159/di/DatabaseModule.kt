package com.lifelover.companion159.di

import android.content.Context
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.dao.PreferencesDao
import com.lifelover.companion159.data.local.dao.SyncQueueDao
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
    fun provideInventoryDatabase(
        @ApplicationContext context: Context
    ): InventoryDatabase {
        return InventoryDatabase.getDatabase(context)
    }

    @Provides
    fun providePreferencesDao(
        database: InventoryDatabase
    ): PreferencesDao {
        return database.preferencesDao()
    }

    @Provides
    @Singleton
    fun provideInventoryDao(
        database: InventoryDatabase
    ): InventoryDao {
        return database.inventoryDao()
    }

    @Provides
    @Singleton
    fun provideSyncQueueDao(
        database: InventoryDatabase
    ): SyncQueueDao {
        return database.syncQueueDao()
    }
}