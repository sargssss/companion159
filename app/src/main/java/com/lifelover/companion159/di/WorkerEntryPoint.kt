package com.lifelover.companion159.di

import android.content.Context
import com.lifelover.companion159.data.local.database.InventoryDatabase
import com.lifelover.companion159.data.remote.auth.GoogleAuthService
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.remote.repository.SupabaseInventoryRepository
import com.lifelover.companion159.data.sync.SyncService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Service Locator
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerEntryPoint {
    fun syncService(): SyncService
    fun inventoryDatabase(): InventoryDatabase
    fun supabaseAuthService(): SupabaseAuthService
    fun supabaseInventoryRepository(): SupabaseInventoryRepository
}

object ServiceLocator {
    fun getSyncService(context: Context): SyncService {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WorkerEntryPoint::class.java
        )
        return entryPoint.syncService()
    }
}