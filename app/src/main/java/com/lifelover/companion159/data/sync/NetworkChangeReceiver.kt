package com.lifelover.companion159.data.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class NetworkChangeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NetworkChangeReceiver"
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ConnectivityManager.CONNECTIVITY_ACTION,
            "android.net.wifi.STATE_CHANGE" -> {
                Log.d(TAG, "📡 Network state changed")

                val isOnline = isNetworkAvailable(context)
                Log.d(TAG, "Network status: ${if (isOnline) "ONLINE ✅" else "OFFLINE 📴"}")

                if (isOnline) {
                    GlobalScope.launch {
                        try {
                            val authService = com.lifelover.companion159.di.ServiceLocator
                                .getSupabaseAuthService(context)
                            val syncService = com.lifelover.companion159.di.ServiceLocator
                                .getSyncService(context)

                            val userId = authService.getUserIdForSync()
                            val currentUser = authService.getCurrentUser()

                            if (userId != null) {
                                if (currentUser != null) {
                                    Log.d(TAG, "👤 Current user: ${currentUser.email} ($userId)")
                                } else {
                                    Log.d(TAG, "👤 Last logged user: $userId")
                                }

                                val hasUnsynced = syncService.hasUnsyncedChanges()
                                val hasOffline = syncService.hasOfflineItems()

                                Log.d(TAG, "📊 Unsynced: $hasUnsynced, Offline: $hasOffline")

                                if (hasUnsynced || hasOffline) {
                                    Log.d(TAG, "🔥 Triggering foreground sync")
                                    SyncForegroundService.start(context)
                                } else {
                                    Log.d(TAG, "✨ No data to sync")
                                }
                            } else {
                                Log.d(TAG, "⚠️ No user found (never logged in)")
                                Log.d(TAG, "💡 Offline mode: Data will sync after first login")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Failed to check sync status", e)
                        }
                    }
                }
            }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnectedOrConnecting == true
        }
    }
}