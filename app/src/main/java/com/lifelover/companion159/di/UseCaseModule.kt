package com.lifelover.companion159.di

import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.usecases.inventory.AddItemUseCase
import com.lifelover.companion159.domain.usecases.inventory.DeleteItemUseCase
import com.lifelover.companion159.domain.usecases.inventory.GetItemsUseCase
import com.lifelover.companion159.domain.usecases.inventory.SyncUseCase
import com.lifelover.companion159.domain.usecases.inventory.UpdateItemUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Provides use case instances for dependency injection
 * Separated from DatabaseModule to avoid Hilt aggregation conflicts
 */
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideGetInventoryItems(
        repository: InventoryRepository
    ): GetItemsUseCase {
        return GetItemsUseCase(repository)
    }

    @Provides
    fun provideAddInventoryItem(
        repository: InventoryRepository
    ): AddItemUseCase {
        return AddItemUseCase(repository)
    }

    @Provides
    fun provideUpdateInventoryItem(
        repository: InventoryRepository
    ): UpdateItemUseCase {
        return UpdateItemUseCase(repository)
    }

    @Provides
    fun provideDeleteInventoryItem(
        repository: InventoryRepository
    ): DeleteItemUseCase {
        return DeleteItemUseCase(repository)
    }

    @Provides
    fun provideSyncInventory(
        repository: InventoryRepository
    ): SyncUseCase {
        return SyncUseCase(repository)
    }
}