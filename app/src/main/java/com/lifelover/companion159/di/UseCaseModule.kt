package com.lifelover.companion159.di

import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.domain.usecases.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

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