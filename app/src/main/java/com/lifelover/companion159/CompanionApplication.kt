package com.lifelover.companion159

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.lifelover.companion159.data.sync.AutoSyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CompanionApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var autoSyncManager: AutoSyncManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()
        Log.d("CompanionApplication", "🚀 Application started")
        try {
            // Ініціалізуємо автоматичну синхронізацію
            // Вона буде спостерігати за статусом мережі та автентифікації
            autoSyncManager.initialize()
            Log.d("CompanionApplication", "✅ AutoSyncManager initialized successfully")
        } catch (e: Exception) {
            Log.e("CompanionApplication", "❌ Failed to initialize AutoSyncManager", e)
        }
    }
}