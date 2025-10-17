package com.lifelover.companion159.di

import android.content.Context
import com.lifelover.companion159.data.local.UserPreferences
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.dao.PreferencesDao
import com.lifelover.companion159.data.local.dao.SyncDao
import com.lifelover.companion159.data.local.dao.SyncQueueDao
import com.lifelover.companion159.data.remote.SupabaseClient as AppSupabaseClient
import com.lifelover.companion159.data.remote.auth.GoogleAuthService
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.remote.sync.DownloadSyncService
import com.lifelover.companion159.data.remote.api.SupabaseInventoryApi
import com.lifelover.companion159.data.remote.sync.SyncManager
import com.lifelover.companion159.data.remote.sync.SyncMapper
import com.lifelover.companion159.data.remote.sync.SyncQueueManager
import com.lifelover.companion159.data.remote.sync.SyncQueueProcessor
import com.lifelover.companion159.data.remote.sync.UploadSyncService
import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.data.repository.PositionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return AppSupabaseClient.client
    }

    @Provides
    @Singleton
    fun provideSupabaseAuth(client: SupabaseClient): Auth {
        return client.auth
    }

    @Provides
    @Singleton
    fun provideSupabasePostgrest(client: SupabaseClient): Postgrest {
        return client.postgrest
    }

    @Provides
    @Singleton
    fun provideGoogleAuthService(
        @ApplicationContext context: Context
    ): GoogleAuthService {
        return GoogleAuthService(context)
    }

    @Provides
    @Singleton
    fun provideUserPreferences(
        @ApplicationContext context: Context
    ): UserPreferences {
        return UserPreferences(context)
    }

    @Provides
    @Singleton
    fun provideSupabaseAuthService(
        googleAuthService: GoogleAuthService,
        userPreferences: UserPreferences
    ): SupabaseAuthService {
        return SupabaseAuthService(googleAuthService, userPreferences)
    }

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
    fun provideSyncMapper(): SyncMapper {
        return SyncMapper
    }

    @Provides
    @Singleton
    fun provideSupabaseInventoryApi(
        client: SupabaseClient
    ): SupabaseInventoryApi {
        return SupabaseInventoryApi(client)
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
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    @Provides
    @Singleton
    fun provideSyncQueueManager(
        syncQueueDao: SyncQueueDao,
        @ApplicationContext context: Context
    ): SyncQueueManager {
        return SyncQueueManager(syncQueueDao, context)
    }

    @Provides
    @Singleton
    fun provideSyncQueueProcessor(
        syncQueueDao: SyncQueueDao,
        inventoryDao: InventoryDao,
        syncDao: SyncDao,
        api: SupabaseInventoryApi,
        mapper: SyncMapper
    ): SyncQueueProcessor {
        return SyncQueueProcessor(syncQueueDao, inventoryDao, syncDao, api, mapper)
    }

    @Provides
    @Singleton
    fun provideInventoryRepository(
        dao: InventoryDao,
        positionRepository: PositionRepository,
        authService: SupabaseAuthService,
        syncQueueManager: SyncQueueManager
    ): InventoryRepository {
        return InventoryRepository(dao, positionRepository, authService, syncQueueManager)
    }
}