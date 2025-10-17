package com.lifelover.companion159.data.remote.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.lifelover.companion159.data.local.dao.SyncDao
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
 * Uses SyncDao for sync-specific queries
 */
@Singleton
class SyncOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncDao: SyncDao,
    private val authService: SupabaseAuthService,
    private val positionRepository: PositionRepository,
    private val uploadService: UploadSyncService
) {
    companion object {
        private const val TAG = "SyncOrchestrator"
        private const val DEBOUNCE_MS = 500L
        private const val POLL_INTERVAL_MS = 1000L
    }

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    @OptIn(FlowPreview::class)
    fun startObserving() {
        syncScope.launch {
            Log.d(TAG, "🔄 Starting sync observation...")

            // Create a Flow that polls for items needing sync
            val itemsNeedingSyncFlow = flow {
                while (true) {
                    val userId = authService.getUserId()
                    val position = positionRepository.getPosition()

                    if (userId != null && position != null) {
                        val items = syncDao.getItemsNeedingSync(userId, position)

                        if (items.isNotEmpty()) {
                            Log.d(TAG, "📋 Found ${items.size} items needing sync:")
                            items.forEach { item ->
                                Log.d(
                                    TAG,
                                    "  - ${item.itemName}: available=${item.availableQuantity}, needed=${item.neededQuantity}, isActive=${item.isActive}"
                                )
                            }
                        }

                        emit(items)
                    } else {
                        emit(emptyList())
                    }

                    delay(POLL_INTERVAL_MS)
                }
            }
                .distinctUntilChanged()
                .debounce(DEBOUNCE_MS)

            combine(
                authService.isAuthenticated,
                positionRepository.currentPosition,
                itemsNeedingSyncFlow
            ) { isAuth, position, items ->
                SyncTrigger(isAuth, position, items)
            }
                .filter { trigger ->
                    val canSync = trigger.isAuthenticated &&
                            trigger.position != null &&
                            trigger.itemsNeedingSync.isNotEmpty() &&
                            isNetworkAvailable()

                    if (!canSync) {
                        if (trigger.itemsNeedingSync.isNotEmpty()) {
                            Log.d(
                                TAG,
                                "⏸️ Cannot sync yet: auth=${trigger.isAuthenticated}, position=${trigger.position != null}, network=${isNetworkAvailable()}"
                            )
                        }
                    }

                    canSync
                }
                .collect { trigger ->
                    Log.d(TAG, "🚀 Auto-sync triggered: ${trigger.itemsNeedingSync.size} items")
                    performUploadSync(trigger.position!!)
                }
        }
    }

    private data class SyncTrigger(
        val isAuthenticated: Boolean,
        val position: String?,
        val itemsNeedingSync: List<com.lifelover.companion159.data.local.entities.InventoryItemEntity>
    )

    private suspend fun performUploadSync(crewName: String) {
        if (_isSyncing.value) {
            Log.d(TAG, "⏭️ Sync already in progress")
            return
        }

        try {
            _isSyncing.value = true
            Log.d(TAG, "⬆️ Starting upload sync...")

            val userId = authService.getUserId()
            uploadService.uploadPendingChanges(userId, crewName)
                .onSuccess { count ->
                    Log.d(TAG, "✅ Auto-sync completed: $count items uploaded")
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Auto-sync failed: ${error.message}", error)
                }

        } finally {
            _isSyncing.value = false
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    fun stopObserving() {
        syncScope.cancel()
        Log.d(TAG, "Sync orchestrator stopped")
    }
}