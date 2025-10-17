package com.lifelover.companion159

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.lifelover.companion159.data.remote.sync.SyncOrchestrator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class for Companion159
 * Sets up Hilt dependency injection and automatic sync
 */
@HiltAndroidApp
class CompanionApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncOrchestrator: SyncOrchestrator

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()
        Log.d("CompanionApplication", "🚀 Application started")

        // Start automatic sync observation
        syncOrchestrator.startObserving()
    }
}