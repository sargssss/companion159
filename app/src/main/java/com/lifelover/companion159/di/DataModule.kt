package com.lifelover.companion159.di

import android.content.Context
import androidx.room.Room
import com.lifelover.companion159.data.SyncPreferences
import com.lifelover.companion159.data.repository.LocalInventoryRepository
import com.lifelover.companion159.data.repository.LocalInventoryRepositoryImpl
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.database.InventoryDatabase
import com.lifelover.companion159.data.remote.api.InventoryApiService
import com.lifelover.companion159.network.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindLocalInventoryRepository(
        localInventoryRepositoryImpl: LocalInventoryRepositoryImpl
    ): LocalInventoryRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideInventoryDatabase(@ApplicationContext context: Context): InventoryDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            InventoryDatabase::class.java,
            "companion159_inventory_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideInventoryDao(database: InventoryDatabase): InventoryDao {
        return database.inventoryDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://your-api-base-url.com/api/") // Replace with your API URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideInventoryApiService(retrofit: Retrofit): InventoryApiService =
        retrofit.create(InventoryApiService::class.java)

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor =
        NetworkMonitor(context)

    @Provides
    @Singleton
    fun provideSyncPreferences(@ApplicationContext context: Context): SyncPreferences =
        SyncPreferences(context)
}