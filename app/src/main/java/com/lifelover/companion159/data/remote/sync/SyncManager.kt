package com.lifelover.companion159.data.remote.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.lifelover.companion159.data.local.dao.SyncQueueDao
import com.lifelover.companion159.data.local.entities.InventoryItemEntity
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.remote.dto.SupabaseInventoryItemDto
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

    /**
     * Perform full bidirectional sync
     * Used ONLY after authentication and manual sync button
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
                Log.d(TAG, "🔄 Starting FULL bidirectional sync...")

                val userId = authService.getUserId()
                val crewName = positionRepository.getPosition()!!

                Log.d(TAG, "Syncing for crew: $crewName, user: $userId")

                // Step 1: Upload local changes FOR THIS CREW
                val uploadResult = uploadService.uploadPendingChanges(userId, crewName)
                uploadResult.fold(
                    onSuccess = { count ->
                        Log.d(TAG, "✅ Uploaded $count items for crew: $crewName")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Upload failed: ${error.message}")
                    }
                )

                // Step 2: Download remote changes FOR THIS CREW
                val downloadResult = downloadService.downloadChanges(
                    crewName = crewName,
                    userId = userId,
                    forceFullSync = forceFullDownload
                )
                downloadResult.fold(
                    onSuccess = { count ->
                        Log.d(TAG, "✅ Downloaded $count items for crew: $crewName")
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

                Log.d(TAG, "✅ FULL sync completed for crew: $crewName")

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
     * Performs FULL sync with download (first sync or after long time)
     */
    fun syncOnStartup() {
        Log.d(TAG, "🚀 Sync on startup triggered")

        // Check if this is first sync
        val isFirstSync = downloadService.getLastSyncTimestamp() == null

        // Full sync on startup
        sync(forceFullDownload = isFirstSync)
    }

    /**
     * Sync on network reconnection
     * Upload queued changes, then download updates
     */
    fun syncOnReconnect() {
        Log.d(TAG, "📶 Sync on reconnect triggered")
        sync(forceFullDownload = false)
    }

    /**
     * Force full sync - triggered by user button
     * Downloads all items regardless of lastSync timestamp
     *
     * Use cases:
     * - Manual sync button in UI
     * - User wants to refresh all data
     */
    fun forceFullSync() {
        Log.d(TAG, "🔄 Force full sync triggered by user")
        downloadService.resetLastSyncTimestamp()
        sync(forceFullDownload = true)
    }

    fun uploadOnlySync() {
        Log.d(TAG, "========================================")
        Log.d(TAG, "uploadOnlySync() called")

        if (_syncState.value.isSyncing) {
            Log.d(TAG, "⏭️ Sync already in progress, skipping")
            Log.d(TAG, "========================================")
            return
        }

        if (!canSync()) {
            Log.d(TAG, "❌ Cannot sync - conditions not met")
            Log.d(TAG, "========================================")
            return
        }

        syncScope.launch {
            try {
                Log.d(TAG, "⬆️ Starting upload-only sync...")

                val userId = authService.getUserId()
                val crewName = positionRepository.getPosition()

                Log.d(TAG, "  userId: $userId")
                Log.d(TAG, "  crewName: $crewName")

                if (crewName == null) {
                    Log.e(TAG, "❌ Cannot sync - no crew selected")
                    Log.d(TAG, "========================================")
                    return@launch
                }

                val uploadResult = uploadService.uploadPendingChanges(userId, crewName)
                uploadResult.fold(
                    onSuccess = { count ->
                        Log.d(TAG, "✅ Uploaded $count items for crew: $crewName")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Upload failed: ${error.message}", error)
                    }
                )
                Log.d(TAG, "========================================")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Upload-only sync exception", e)
                Log.d(TAG, "========================================")
            }
        }
    }
}