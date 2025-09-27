package com.lifelover.companion159.di

import android.content.Context
import com.lifelover.companion159.data.remote.api.InventoryApiService
import com.lifelover.companion159.data.remote.auth.AuthService
import com.lifelover.companion159.network.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor =
        NetworkMonitor(context)

    @Provides
    @Singleton
    fun provideInventoryApiService(): InventoryApiService = InventoryApiService()

    @Provides
    @Singleton
    fun provideAuthService(): AuthService = AuthService()
}