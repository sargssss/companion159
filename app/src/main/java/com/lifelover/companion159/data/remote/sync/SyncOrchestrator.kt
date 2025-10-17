package com.lifelover.companion159.data.remote.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.lifelover.companion159.data.local.dao.InventoryDao
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.repository.PositionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates automatic synchronization based on database changes
 *
 * Strategy:
 * - Observes items with needsSync=true via Flow
 * - Debounces rapid changes (500ms)
 * - Only uploads when online + authenticated + position set
 * - Runs on dedicated coroutine scope
 *
 * Call startObserving() once from Application.onCreate()
 */
@Singleton
class SyncOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: InventoryDao,
    private val authService: SupabaseAuthService,
    private val positionRepository: PositionRepository,
    private val uploadService: UploadSyncService
) {
    companion object {
        private const val TAG = "SyncOrchestrator"
        private const val DEBOUNCE_MS = 500L
    }

    // Dedicated scope for sync operations
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /**
     * Start observing database changes and trigger sync
     * Call this once from Application.onCreate()
     */
    @OptIn(FlowPreview::class)
    fun startObserving() {
        syncScope.launch {
            Log.d(TAG, "🔄 Starting sync observation...")

            // Combine all required conditions
            combine(
                authService.isAuthenticated,
                positionRepository.currentPosition,
                flow {
                    // Poll for items needing sync every second
                    while (true) {
                        val userId = authService.getUserId()
                        val position = positionRepository.getPosition()

                        if (userId != null && position != null) {
                            val items = dao.getItemsNeedingSync(userId, position)
                            emit(items)
                        } else {
                            emit(emptyList())
                        }

                        delay(1000) // Check every second
                    }
                }
            ) { isAuth, position, items ->
                SyncTrigger(isAuth, position, items)
            }
                .debounce(DEBOUNCE_MS) // Wait 500ms after last change
                .filter { trigger ->
                    // Only sync if all conditions met
                    trigger.isAuthenticated &&
                            trigger.position != null &&
                            trigger.itemsNeedingSync.isNotEmpty() &&
                            isNetworkAvailable()
                }
                .collect { trigger ->
                    Log.d(TAG, "🔄 Auto-sync triggered: ${trigger.itemsNeedingSync.size} items")
                    performUploadSync(trigger.position!!)
                }
        }
    }

    /**
     * Data class for sync trigger conditions
     */
    private data class SyncTrigger(
        val isAuthenticated: Boolean,
        val position: String?,
        val itemsNeedingSync: List<com.lifelover.companion159.data.local.entities.InventoryItemEntity>
    )

    /**
     * Perform upload synchronization
     */
    private suspend fun performUploadSync(crewName: String) {
        if (_isSyncing.value) {
            Log.d(TAG, "⏭️ Sync already in progress")
            return
        }

        try {
            _isSyncing.value = true

            val userId = authService.getUserId()
            uploadService.uploadPendingChanges(userId, crewName)
                .onSuccess { count ->
                    Log.d(TAG, "✅ Auto-sync completed: $count items uploaded")
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Auto-sync failed: ${error.message}")
                }

        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Check network availability
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Stop observing (call from Application.onDestroy if needed)
     */
    fun stopObserving() {
        syncScope.cancel()
        Log.d(TAG, "Sync orchestrator stopped")
    }
}