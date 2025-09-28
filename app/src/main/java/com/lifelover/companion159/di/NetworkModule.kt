package com.lifelover.companion159.di

import android.content.Context
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.remote.auth.GoogleAuthService
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.remote.client.SupabaseClient
import com.lifelover.companion159.data.remote.repository.SupabaseInventoryRepository
import com.lifelover.companion159.data.sync.AutoSyncManager
import com.lifelover.companion159.data.sync.SyncService
import com.lifelover.companion159.network.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): io.github.jan.supabase.SupabaseClient {
        return SupabaseClient.client
    }

    @Provides
    @Singleton
    fun provideSupabaseAuth(client: io.github.jan.supabase.SupabaseClient): io.github.jan.supabase.auth.Auth {
        return client.auth
    }

    @Provides
    @Singleton
    fun provideSupabasePostgrest(client: io.github.jan.supabase.SupabaseClient): io.github.jan.supabase.postgrest.Postgrest {
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
    fun provideSupabaseAuthService(
        googleAuthService: GoogleAuthService
    ): SupabaseAuthService {
        return SupabaseAuthService(googleAuthService)
    }

    @Provides
    @Singleton
    fun provideSupabaseInventoryRepository(): SupabaseInventoryRepository {
        return SupabaseInventoryRepository()
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context
    ): NetworkMonitor {
        return NetworkMonitor(context)
    }

    @Provides
    @Singleton
    fun provideSyncService(
        localDao: InventoryDao,
        remoteRepository: SupabaseInventoryRepository,
        authService: SupabaseAuthService
    ): SyncService {
        return SyncService(localDao, remoteRepository, authService)
    }

    @Provides
    @Singleton
    fun provideAutoSyncManager(
        @ApplicationContext context: Context,
        syncService: SyncService,
        networkMonitor: NetworkMonitor,
        authService: SupabaseAuthService
    ): AutoSyncManager {
        return AutoSyncManager(context, syncService, networkMonitor, authService)
    }
}