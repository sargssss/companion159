package com.lifelover.companion159.di

import android.content.Context
import com.lifelover.companion159.data.local.UserPreferences
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.local.dao.PreferencesDao
import com.lifelover.companion159.data.remote.auth.GoogleAuthService
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.remote.SupabaseClient
import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.data.repository.PositionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Singleton

/**
 * Provides network-related dependencies
 * Simplified: removed UseCases, direct Repository provision
 */
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
        preferencesDao: PreferencesDao
    ): PositionRepository {
        return PositionRepository(preferencesDao)
    }

    /**
     * Provide InventoryRepository directly (no interface)
     */
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