package com.lifelover.companion159.data.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.lifelover.companion159.di.ServiceLocator
import kotlinx.coroutines.launch

/**
 * Запускає синхронізацію після перезавантаження пристрою
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.d(TAG, "Device booted - initializing sync")

                try {
                    val autoSyncManager = ServiceLocator.getAutoSyncManager(context)
                    autoSyncManager.initialize()

                    val syncService = ServiceLocator.getSyncService(context)
                    kotlinx.coroutines.GlobalScope.launch {
                        if (syncService.hasUnsyncedChanges()) {
                            autoSyncManager.triggerImmediateSync()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize sync after boot", e)
                }
            }
        }
    }
}