package com.lifelover.companion159.data.remote.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.repository.InventoryRepository
import com.lifelover.companion159.data.repository.PositionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class SyncState(
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val error: String? = null
)

/**
 * Simplified sync manager
 *
 * Strategy:
 * 1. After any DB change → trigger sync()
 * 2. sync() uploads needsSync=1 items
 * 3. sync() downloads server changes
 * 4. Done
 *
 * No queues, no workers, just direct sync
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authService: SupabaseAuthService,
    private val positionRepository: PositionRepository,
    private val uploadService: UploadSyncService,
    private val downloadService: DownloadSyncService
) {
    companion object {
        private const val TAG = "SyncManager"
        private const val DEBOUNCE_DELAY_MS = 300L
    }

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private var lastSyncTriggerTime = 0L

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /**
     * Setup callback in repository
     * Called during app initialization
     */
    fun setupSyncCallback(repository: InventoryRepository) {
        repository.onNeedsSyncCallback = {
            Log.d(TAG, "🔔 Sync triggered by repository")
            sync()
        }
    }

    /**
     * Check conditions
     */
    fun canSync(): Boolean {
        val isAuthenticated = authService.isUserAuthenticated()
        val hasPosition = positionRepository.getPosition() != null
        val hasInternet = isNetworkAvailable()

        if (!isAuthenticated || !hasPosition || !hasInternet) {
            Log.d(TAG, "Cannot sync: auth=$isAuthenticated, position=$hasPosition, internet=$hasInternet")
        }

        return isAuthenticated && hasPosition && hasInternet
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun clearError() {
        _syncState.value = _syncState.value.copy(error = null)
    }

    /**
     * Main sync method
     *
     * Flow:
     * 1. Upload items with needsSync=1
     * 2. Download server changes
     * 3. Update UI state
     *
     * Debouncing: ignores calls within 300ms
     */
    fun sync(forceFullDownload: Boolean = false) {
        // Skip if already syncing
        if (_syncState.value.isSyncing) {
            Log.d(TAG, "⏭️ Already syncing, skipping")
            return
        }

        // Debounce: skip if last sync < 300ms ago
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSyncTriggerTime < DEBOUNCE_DELAY_MS) {
            Log.d(TAG, "⏭️ Debounced (${currentTime - lastSyncTriggerTime}ms since last)")
            return
        }
        lastSyncTriggerTime = currentTime

        if (!canSync()) {
            Log.d(TAG, "❌ Cannot sync (no auth/position/internet)")
            return
        }

        syncScope.launch {
            syncMutex.withLock {
                try {
                    _syncState.value = SyncState(isSyncing = true)
                    Log.d(TAG, "🔄 Starting sync...")

                    val userId = authService.getUserId()
                    val crewName = positionRepository.getPosition()!!

                    // Step 1: Upload needsSync=1 items
                    val uploadResult = uploadService.uploadPendingChanges(userId, crewName)
                    uploadResult.fold(
                        onSuccess = { count ->
                            Log.d(TAG, "✅ Uploaded $count items")
                        },
                        onFailure = { error ->
                            Log.e(TAG, "❌ Upload failed: ${error.message}")
                        }
                    )

                    // Step 2: Download server changes
                    val downloadResult = downloadService.downloadChanges(
                        crewName = crewName,
                        userId = userId,
                        forceFullSync = forceFullDownload
                    )
                    downloadResult.fold(
                        onSuccess = { count ->
                            Log.d(TAG, "✅ Downloaded $count items")
                        },
                        onFailure = { error ->
                            Log.e(TAG, "❌ Download failed: ${error.message}")
                        }
                    )

                    _syncState.value = SyncState(
                        isSyncing = false,
                        lastSyncTime = System.currentTimeMillis()
                    )

                    Log.d(TAG, "✅ Sync completed")

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Sync failed", e)
                    _syncState.value = SyncState(
                        isSyncing = false,
                        error = e.message
                    )
                }
            }
        }
    }

    /**
     * Sync on app startup
     */
    fun syncOnStartup() {
        Log.d(TAG, "🚀 Startup sync triggered")
        val isFirstSync = downloadService.getLastSyncTimestamp() == null
        sync(forceFullDownload = isFirstSync)
    }

    /**
     * Force full sync from UI button
     */
    fun forceFullSync() {
        Log.d(TAG, "🔄 Force full sync triggered")
        downloadService.resetLastSyncTimestamp()
        sync(forceFullDownload = true)
    }
}