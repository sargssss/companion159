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

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    fun setupRepositoryCallback(repository: InventoryRepository) {
        repository.onNeedsSyncCallback = { triggerSync() }
    }

    fun triggerSync(forceFullDownload: Boolean = false) {
        if (_syncState.value.isSyncing) {
            Log.d(TAG, "⏭️ Already syncing")
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
        }
    }

    private suspend fun performSync(forceFullDownload: Boolean) {
        syncMutex.withLock {
            try {
                _syncState.value = SyncState(isSyncing = true)
                Log.d(TAG, "🔄 SYNC START")

                val userId = authService.getUserId()
                val crewName = positionRepository.getPosition()!!

                // Upload
                syncService.uploadPendingItems(userId, crewName)
                    .onFailure { Log.e(TAG, "Upload error: ${it.message}") }

                // Download
                syncService.downloadServerItems(
                    crewName = crewName,
                    userId = userId,
                    forceFullSync = forceFullDownload
                )
                    .onFailure { Log.e(TAG, "Download error: ${it.message}") }

                _syncState.value = SyncState(
                    isSyncing = false,
                    lastSyncTime = System.currentTimeMillis()
                )

                Log.d(TAG, "🔄 SYNC END ✅")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Sync failed", e)
                _syncState.value = SyncState(
                    isSyncing = false,
                    error = e.message
                )
            }
        }
    }

    private fun canSync(): Boolean {
        return authService.isUserAuthenticated() &&
                positionRepository.getPosition() != null &&
                hasNetworkConnection()
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