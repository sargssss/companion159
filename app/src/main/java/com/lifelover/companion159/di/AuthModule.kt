package com.lifelover.companion159.di

import android.content.Context
import com.lifelover.companion159.data.local.UserPreferences
import com.lifelover.companion159.data.remote.auth.GoogleAuthService
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

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
        googleAuthService: GoogleAuthService,
        userPreferences: UserPreferences
    ): SupabaseAuthService {
        return SupabaseAuthService(googleAuthService, userPreferences)
    }
}