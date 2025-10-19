// di/SyncModule.kt
package com.lifelover.companion159.di

import android.content.Context
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.dao.SyncDao
import com.lifelover.companion159.data.remote.api.SupabaseInventoryApi
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.remote.sync.SimpleSyncService
import com.lifelover.companion159.data.remote.sync.SyncOrchestrator
import com.lifelover.companion159.data.repository.PositionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideSyncService(
        inventoryDao: InventoryDao,
        syncDao: SyncDao,
        api: SupabaseInventoryApi
    ): SimpleSyncService {
        return SimpleSyncService(inventoryDao, syncDao, api)
    }

    @Provides
    @Singleton
    fun provideSyncOrchestrator(
        @ApplicationContext context: Context,
        authService: SupabaseAuthService,
        positionRepository: PositionRepository,
        syncService: SimpleSyncService
    ): SyncOrchestrator {
        return SyncOrchestrator(context, authService, positionRepository, syncService)
    }
}