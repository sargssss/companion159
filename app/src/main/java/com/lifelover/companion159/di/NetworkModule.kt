package com.lifelover.companion159.di

import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.remote.client.SupabaseClient
import com.lifelover.companion159.data.remote.repository.SupabaseInventoryRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
    fun provideSupabaseAuthService(): SupabaseAuthService {
        return SupabaseAuthService()
    }

    @Provides
    @Singleton
    fun provideSupabaseInventoryRepository(): SupabaseInventoryRepository {
        return SupabaseInventoryRepository()
    }
}