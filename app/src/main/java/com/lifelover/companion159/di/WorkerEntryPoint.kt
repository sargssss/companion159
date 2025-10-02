package com.lifelover.companion159.di

import android.content.Context
import com.lifelover.companion159.data.local.UserPreferences
import com.lifelover.companion159.data.local.database.InventoryDatabase
import com.lifelover.companion159.data.remote.auth.GoogleAuthService
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.remote.repository.SupabaseInventoryRepository
import com.lifelover.companion159.data.sync.AutoSyncManager
import com.lifelover.companion159.data.sync.SyncService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerEntryPoint {
    fun syncService(): SyncService
    fun inventoryDatabase(): InventoryDatabase
    fun supabaseAuthService(): SupabaseAuthService
    fun supabaseInventoryRepository(): SupabaseInventoryRepository
    fun autoSyncManager(): AutoSyncManager
    fun userPreferences(): UserPreferences // НОВИЙ
}

object ServiceLocator {

    fun getSyncService(context: Context): SyncService {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WorkerEntryPoint::class.java
        )
        return entryPoint.syncService()
    }

    fun getAutoSyncManager(context: Context): AutoSyncManager {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WorkerEntryPoint::class.java
        )
        return entryPoint.autoSyncManager()
    }

    fun getSupabaseAuthService(context: Context): SupabaseAuthService {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WorkerEntryPoint::class.java
        )
        return entryPoint.supabaseAuthService()
    }

    fun getInventoryDatabase(context: Context): InventoryDatabase {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WorkerEntryPoint::class.java
        )
        return entryPoint.inventoryDatabase()
    }

    // НОВИЙ метод (якщо потрібно)
    fun getUserPreferences(context: Context): UserPreferences {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            WorkerEntryPoint::class.java
        )
        return entryPoint.userPreferences()
    }
}