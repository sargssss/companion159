package com.lifelover.companion159.di

import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.data.repository.InventoryRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRepository(impl: InventoryRepositoryImpl): InventoryRepository
}