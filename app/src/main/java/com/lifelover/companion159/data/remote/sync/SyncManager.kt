package com.lifelover.companion159.data.remote.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
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

/**
 * Sync state for UI
 */
data class SyncState(
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val error: String? = null
)

/**
 * Manual sync manager - simplified version
 *
 * Only handles manual sync triggered by:
 * - UI sync button
 * - App startup
 * - User actions (if needed)
 *
 * NO automatic polling - keeps architecture simple
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
        private const val DEBOUNCE_DELAY_MS = 500L
    }

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private var lastSyncTriggerTime = 0L

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /**
     * Check if all conditions for sync are met
     */
    fun canSync(): Boolean {
        val isAuthenticated = authService.isUserAuthenticated()
        val hasPosition = positionRepository.getPosition() != null
        val hasInternet = isNetworkAvailable()

        Log.d(TAG, "Sync conditions: auth=$isAuthenticated, position=$hasPosition, internet=$hasInternet")

        return isAuthenticated && hasPosition && hasInternet
    }

    /**
     * Check if internet connection is available
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Clear sync error
     */
    fun clearError() {
        _syncState.value = _syncState.value.copy(error = null)
    }

    /**
     * Perform full bidirectional sync
     *
     * Features:
     * - Deduplication (ignores rapid repeated calls)
     * - Thread-safe (mutex)
     * - Uploads local changes first, then downloads
     *
     * @param forceFullDownload If true, downloads all items (ignores lastSyncTimestamp)
     */
    fun sync(forceFullDownload: Boolean = false) {
        // Deduplication: skip if already syncing
        if (_syncState.value.isSyncing) {
            Log.d(TAG, "⏭️ Sync already in progress, skipping")
            return
        }

        // Debounce: skip if last sync was < 500ms ago
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSyncTriggerTime < DEBOUNCE_DELAY_MS) {
            Log.d(TAG, "⏭️ Sync debounced (too soon after last sync)")
            return
        }
        lastSyncTriggerTime = currentTime

        if (!canSync()) {
            Log.d(TAG, "❌ Sync conditions not met")
            return
        }

        syncScope.launch {
            syncMutex.withLock {
                try {
                    _syncState.value = SyncState(isSyncing = true)
                    Log.d(TAG, "🔄 Starting FULL bidirectional sync...")

                    val userId = authService.getUserId()
                    val crewName = positionRepository.getPosition()!!

                    Log.d(TAG, "Syncing for crew: $crewName, user: $userId")

                    // Step 1: Upload local changes
                    val uploadResult = uploadService.uploadPendingChanges(userId, crewName)
                    uploadResult.fold(
                        onSuccess = { count ->
                            Log.d(TAG, "✅ Uploaded $count items")
                        },
                        onFailure = { error ->
                            Log.e(TAG, "❌ Upload failed: ${error.message}")
                        }
                    )

                    // Step 2: Download remote changes
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

                    Log.d(TAG, "✅ FULL sync completed")

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
     * Downloads all items on first launch
     */
    fun syncOnStartup() {
        Log.d(TAG, "🚀 Sync on startup triggered")
        val isFirstSync = downloadService.getLastSyncTimestamp() == null
        sync(forceFullDownload = isFirstSync)
    }

    /**
     * Force full sync - triggered by user button
     * Resets timestamp and downloads everything
     */
    fun forceFullSync() {
        Log.d(TAG, "🔄 Force full sync triggered by user")
        downloadService.resetLastSyncTimestamp()
        sync(forceFullDownload = true)
    }
}