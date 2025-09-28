package com.lifelover.companion159.di

import android.content.Context
import androidx.room.Room
import com.lifelover.companion159.data.repository.LocalInventoryRepository
import com.lifelover.companion159.data.repository.LocalInventoryRepositoryImpl
import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.data.repository.InventoryRepositoryImpl
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.database.InventoryDatabase
import com.lifelover.companion159.domain.usecases.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindLocalInventoryRepository(
        localInventoryRepositoryImpl: LocalInventoryRepositoryImpl
    ): LocalInventoryRepository

    @Binds
    @Singleton
    abstract fun bindInventoryRepository(
        inventoryRepositoryImpl: InventoryRepositoryImpl
    ): InventoryRepository
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
object UseCaseModule {

    @Provides
    fun provideGetItems(repository: InventoryRepository) = GetInventoryItemsUseCase(repository)

    @Provides
    fun provideAddItem(repository: InventoryRepository) = AddInventoryItemUseCase(repository)

    @Provides
    fun provideUpdateItem(repository: InventoryRepository) = UpdateInventoryItemUseCase(repository)

    @Provides
    fun provideDeleteItem(repository: InventoryRepository) = DeleteInventoryItemUseCase(repository)

    @Provides
    fun provideSync(repository: InventoryRepository) = SyncInventoryUseCase(repository)
}