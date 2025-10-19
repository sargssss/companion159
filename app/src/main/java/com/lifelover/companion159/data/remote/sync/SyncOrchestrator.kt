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
    val error: String? = null,
    val pendingSyncs: Int = 0  // Track queued sync requests
)

@Singleton
class SyncOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authService: SupabaseAuthService,
    private val positionRepository: PositionRepository,
    private val syncService: SyncService
) {
    companion object {
        private const val TAG = "SyncOrchestrator"
        private const val DEBOUNCE_MS = 300L
    }

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncMutex = Mutex()
    private var lastSyncTriggerTime = 0L

    // Track if sync is queued while another is in progress
    private var syncQueuedWhileBusy = false

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    fun setupRepositoryCallback(repository: InventoryRepository) {
        repository.onNeedsSyncCallback = { triggerSync() }
    }

    /**
     * Trigger sync with queue support
     *
     * If sync is already running:
     * - Mark that another sync is needed (queued)
     * - When current sync finishes, queued sync will run automatically
     * - This prevents losing changes when user edits items rapidly
     */
    fun triggerSync(forceFullDownload: Boolean = false) {
        if (_syncState.value.isSyncing) {
            Log.d(TAG, "⏭️ Already syncing - queueing next sync")
            syncQueuedWhileBusy = true
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastSyncTriggerTime < DEBOUNCE_MS) {
            Log.d(TAG, "⏭️ Debounced")
            return
        }
        lastSyncTriggerTime = now

        if (!canSync()) {
            Log.d(TAG, "❌ Cannot sync")
            return
        }

        syncScope.launch {
            performSync(forceFullDownload)

            // Check if sync was queued while we were syncing
            if (syncQueuedWhileBusy) {
                syncQueuedWhileBusy = false
                Log.d(TAG, "🔄 Queued sync detected - running again")
                performSync(forceFullDownload = false)
            }
        }
    }

    private suspend fun performSync(forceFullDownload: Boolean) {
        syncMutex.withLock {
            try {
                _syncState.value = SyncState(isSyncing = true)
                Log.d(TAG, "========== SYNC START ==========")
                Log.d(TAG, "Force full download: $forceFullDownload")

                val userId = authService.getUserId()
                val crewName = positionRepository.getPosition()!!

                Log.d(TAG, "Syncing for userId=$userId, crewName=$crewName")

                // Upload pending items
                val uploadResult = syncService.uploadPendingItems(userId, crewName)
                uploadResult
                    .onSuccess { count ->
                        Log.d(TAG, "✅ Upload: $count items")
                    }
                    .onFailure { error ->
                        Log.e(TAG, "⚠️ Upload error: ${error.message}")
                    }

                // Download server changes
                val downloadResult = syncService.downloadServerItems(
                    crewName = crewName,
                    userId = userId,
                    forceFullSync = forceFullDownload
                )
                downloadResult
                    .onSuccess { count ->
                        Log.d(TAG, "✅ Download: $count items")
                    }
                    .onFailure { error ->
                        Log.e(TAG, "⚠️ Download error: ${error.message}")
                    }

                _syncState.value = SyncState(
                    isSyncing = false,
                    lastSyncTime = System.currentTimeMillis()
                )

                Log.d(TAG, "========== SYNC END ✅ ==========")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Sync failed", e)
                _syncState.value = SyncState(
                    isSyncing = false,
                    error = e.message
                )
                Log.d(TAG, "========== SYNC END ❌ ==========")
            }
        }
    }

    private fun canSync(): Boolean {
        // Check if there's a saved userId (user logged in before)
        val hasSavedUser = authService.getUserId() != null

        val hasPosition = positionRepository.getPosition() != null
        val hasNetwork = hasNetworkConnection()

        Log.d(TAG, "canSync check:")
        Log.d(TAG, "  hasSavedUser: $hasSavedUser")
        Log.d(TAG, "  hasPosition: $hasPosition")
        Log.d(TAG, "  hasNetwork: $hasNetwork")

        return hasSavedUser && hasPosition && hasNetwork
    }

    private fun hasNetworkConnection(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun syncOnStartup() {
        val isFirstSync = syncService.getLastSyncTimestamp() == null
        triggerSync(forceFullDownload = isFirstSync)
    }

    fun forceFullSync() {
        syncService.resetLastSyncTimestamp()
        triggerSync(forceFullDownload = true)
    }

    fun clearError() {
        _syncState.value = _syncState.value.copy(error = null)
    }
}