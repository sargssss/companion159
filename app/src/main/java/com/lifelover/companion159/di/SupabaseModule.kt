package com.lifelover.companion159.di

import com.lifelover.companion159.data.remote.api.SupabaseInventoryApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Singleton
import com.lifelover.companion159.data.remote.SupabaseClient as AppSupabaseClient

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return AppSupabaseClient.client
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
    fun provideSupabaseAuth(client: SupabaseClient): Auth {
        return client.auth
    }

    @Provides
    @Singleton
    fun provideSupabasePostgrest(client: SupabaseClient): Postgrest {
        return client.postgrest
    }
}