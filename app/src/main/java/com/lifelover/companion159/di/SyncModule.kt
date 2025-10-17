package com.lifelover.companion159.di

import android.content.Context
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.dao.SyncDao
import com.lifelover.companion159.data.remote.api.SupabaseInventoryApi
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.remote.sync.DownloadSyncService
import com.lifelover.companion159.data.remote.sync.SyncManager
import com.lifelover.companion159.data.remote.sync.SyncMapper
import com.lifelover.companion159.data.remote.sync.UploadSyncService
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
    fun provideSyncMapper(): SyncMapper {
        return SyncMapper
    }

    @Provides
    @Singleton
    fun provideDownloadSyncService(
        inventoryDao: InventoryDao,
        syncDao: SyncDao,
        api: SupabaseInventoryApi,
        mapper: SyncMapper
    ): DownloadSyncService {
        return DownloadSyncService(inventoryDao, syncDao, api, mapper)
    }

    @Provides
    @Singleton
    fun provideUploadSyncService(
        inventoryDao: InventoryDao,
        syncDao: SyncDao,
        api: SupabaseInventoryApi,
        mapper: SyncMapper
    ): UploadSyncService {
        return UploadSyncService(inventoryDao, syncDao, api, mapper)
    }

    @Provides
    @Singleton
    fun provideSyncManager(
        @ApplicationContext context: Context,
        authService: SupabaseAuthService,
        positionRepository: PositionRepository,
        uploadService: UploadSyncService,
        downloadService: DownloadSyncService
    ): SyncManager {
        return SyncManager(context, authService, positionRepository, uploadService, downloadService)
    }
}