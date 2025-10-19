package com.lifelover.companion159.di

import android.content.Context
import androidx.core.content.ContextCompat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Singleton
class PermissionHelper(
    @ApplicationContext private val context: Context
) {

    fun getRequiredPermissions(): List<String> {
        return listOf(
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.RECEIVE_BOOT_COMPLETED
        )
    }

    fun hasAllPermissions(): Boolean {
        return getRequiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object PermissionModule {

    @Provides
    @Singleton
    fun providePermissionHelper(
        @ApplicationContext context: Context
    ): PermissionHelper {
        return PermissionHelper(context)
    }
}