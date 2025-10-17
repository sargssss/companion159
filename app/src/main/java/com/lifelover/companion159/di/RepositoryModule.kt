package com.lifelover.companion159.di

import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.dao.PreferencesDao
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.data.repository.PositionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun providePositionRepository(
        preferencesDao: PreferencesDao,
        authService: SupabaseAuthService
    ): PositionRepository {
        return PositionRepository(preferencesDao, authService)
    }

    @Provides
    @Singleton
    fun provideInventoryRepository(
        dao: InventoryDao,
        positionRepository: PositionRepository,
        authService: SupabaseAuthService
    ): InventoryRepository {
        return InventoryRepository(dao, positionRepository, authService)
    }
}