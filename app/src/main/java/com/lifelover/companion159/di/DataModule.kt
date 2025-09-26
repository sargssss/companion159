package com.lifelover.companion159.di

import android.content.Context
import androidx.room.Room
import com.lifelover.companion159.data.SyncPreferences
import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.data.repository.InventoryRepositoryImpl
import com.lifelover.companion159.data.room.InventoryDatabase
import com.lifelover.companion159.network.InventoryApiService
import com.lifelover.companion159.network.NetworkMonitor
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
object DataModule {

    @Provides
    @Singleton
    fun provideInventoryDatabase(@ApplicationContext context: Context): InventoryDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            InventoryDatabase::class.java,
            "inventory_database"
        ).build()
    }

    @Provides
    fun provideInventoryDao(database: InventoryDatabase) = database.inventoryDao()

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://your-api-base-url.com/api/") // Замініть на ваш URL
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

    @Provides
    @Singleton
    fun provideInventoryRepository(
        dao: com.lifelover.companion159.data.room.InventoryDao,
        apiService: InventoryApiService,
        networkMonitor: NetworkMonitor,
        syncPreferences: SyncPreferences
    ): InventoryRepository = InventoryRepositoryImpl(dao, apiService, networkMonitor, syncPreferences)
}