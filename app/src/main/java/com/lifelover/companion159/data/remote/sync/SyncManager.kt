package com.lifelover.companion159.data.remote.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.lifelover.companion159.data.local.dao.SyncQueueDao
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
 * Central sync manager
 *
 * Coordinates all sync operations between local Room DB and Supabase
 *
 * Responsibilities:
 * - Check sync conditions (auth + position + network)
 * - Orchestrate upload and download sync
 * - Manage sync state
 * - Handle offline queue
 *
 * Sync triggers:
 * - App startup (if conditions met)
 * - Data changes (instant)
 * - Network reconnection
 * - Periodic background (via WorkManager)
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authService: SupabaseAuthService,
    private val positionRepository: PositionRepository,
    private val uploadService: UploadSyncService,
    private val downloadService: DownloadSyncService,
    private val syncQueueDao: SyncQueueDao
) {
    companion object {
        private const val TAG = "SyncManager"
    }

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /**
     * Check if all conditions for sync are met
     *
     * Required:
     * - User is authenticated
     * - Position is selected
     * - Internet connection available
     */
    fun canSync(): Boolean {
        val isAuthenticated = authService.isUserAuthenticated()
        val hasPosition = positionRepository.getPosition() != null
        val hasInternet = isNetworkAvailable()

        Log.d(TAG, "Sync conditions: auth=$isAuthenticated, position=$hasPosition, internet=$hasInternet")

        return isAuthenticated && hasPosition && hasInternet
    }

    /**
     * Perform full bidirectional sync
     *
     * Process:
     * 1. Check sync conditions
     * 2. Upload local changes
     * 3. Download remote changes
     * 4. Update sync state
     *
     * @param forceFullDownload If true, download all items (ignore lastSync)
     */
    fun sync(forceFullDownload: Boolean = false) {
        if (_syncState.value.isSyncing) {
            Log.d(TAG, "Sync already in progress")
            return
        }

        if (!canSync()) {
            Log.d(TAG, "Sync conditions not met")
            return
        }

        syncScope.launch {
            try {
                _syncState.value = SyncState(isSyncing = true)
                Log.d(TAG, "🔄 Starting bidirectional sync...")

                val userId = authService.getUserId()
                val crewName = positionRepository.getPosition()!!

                // Step 1: Upload local changes
                val uploadResult = uploadService.uploadPendingChanges(userId)
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

                // Step 3: Cleanup old failed operations
                syncQueueDao.cleanupOldFailedOperations()

                _syncState.value = SyncState(
                    isSyncing = false,
                    lastSyncTime = System.currentTimeMillis()
                )

                Log.d(TAG, "✅ Sync completed successfully")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Sync failed", e)
                _syncState.value = SyncState(
                    isSyncing = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * Sync on app startup
     * Performs initial full sync if never synced before
     */
    fun syncOnStartup() {
        Log.d(TAG, "Sync on startup triggered")

        // Check if this is first sync
        val isFirstSync = downloadService.getLastSyncTimestamp() == null

        sync(forceFullDownload = isFirstSync)
    }

    /**
     * Sync after data change
     * Uploads changes immediately (if online)
     */
    fun syncOnDataChange() {
        if (!canSync()) {
            Log.d(TAG, "Offline - changes queued for later sync")
            return
        }

        Log.d(TAG, "Sync on data change triggered")
        sync(forceFullDownload = false)
    }

    /**
     * Sync on network reconnection
     * Processes offline queue and downloads changes
     */
    fun syncOnReconnect() {
        Log.d(TAG, "Sync on reconnect triggered")
        sync(forceFullDownload = false)
    }

    /**
     * Force full sync
     * Downloads all items regardless of lastSync timestamp
     */
    fun forceFullSync() {
        Log.d(TAG, "Force full sync triggered")
        downloadService.resetLastSyncTimestamp()
        sync(forceFullDownload = true)
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
     * Get pending queue size
     * Useful for UI indicators
     */
    suspend fun getPendingQueueSize(): Int {
        return syncQueueDao.getQueueSize()
    }

    /**
     * Clear sync error
     */
    fun clearError() {
        _syncState.value = _syncState.value.copy(error = null)
    }
}