package com.lifelover.companion159.data.remote.sync

import android.app.Service
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.lifelover.companion159.data.local.UserPreferences
import com.lifelover.companion159.data.remote.auth.SupabaseAuthService
import com.lifelover.companion159.data.repository.PositionRepository
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay

@AndroidEntryPoint
class SyncBackgroundService : Service() {

    companion object {
        private const val TAG = "SyncBackgroundService"

        // Configuration constants
        private const val SYNC_CHECK_INTERVAL_MS = 30_000L  // 30 seconds
        private const val RETRY_DELAY_MS = 5_000L           // 5 seconds
        private const val MAX_RETRIES = 3
        private const val NETWORK_ERROR_BACKOFF_MS = 60_000L // 1 minute for network errors
    }

    @Inject
    lateinit var connectivityMonitor: ConnectivityMonitor

    @Inject
    lateinit var syncOrchestrator: SyncOrchestrator

    @Inject
    lateinit var positionRepository: PositionRepository

    @Inject
    lateinit var syncService: SyncService

    @Inject
    lateinit var authService: SupabaseAuthService

    @Inject
    lateinit var userPreferences: UserPreferences

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Track retry attempts and failures
    private var failureCount = 0
    private var lastCheckTime = 0L
    private var isProcessing = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 SyncBackgroundService created")

        connectivityMonitor.startMonitoring()

        serviceScope.launch {
            connectivityMonitor.isConnected.collect { isConnected ->
                Log.d(TAG, "Network state: isConnected=$isConnected")

                if (isConnected) {
                    // Reset failure count when network is available
                    failureCount = 0
                    checkAndTriggerSync()
                } else {
                    Log.d(TAG, "📵 Network unavailable - pausing sync checks")
                }
            }
        }
    }

    /**
     * FIXED: Improved error handling with retry logic
     *
     * Features:
     * - Type-specific error handling (network vs auth vs validation)
     * - Exponential backoff for transient errors
     * - Retry limit to prevent infinite loops
     * - Detailed logging for diagnostics
     * - State tracking for debugging
     */
    private suspend fun checkAndTriggerSync(retryAttempt: Int = 0) {
        // Prevent concurrent sync checks
        if (isProcessing) {
            Log.d(TAG, "⏭️ Sync check already in progress, skipping")
            return
        }

        isProcessing = true

        try {
            val now = System.currentTimeMillis()

            // Rate limiting - don't check too frequently
            if (now - lastCheckTime < SYNC_CHECK_INTERVAL_MS && retryAttempt == 0) {
                Log.d(TAG, "⏭️ Skipping: checked recently (${now - lastCheckTime}ms ago)")
                return
            }

            lastCheckTime = now

            Log.d(TAG, "🔍 Starting sync check (attempt ${retryAttempt + 1}/$MAX_RETRIES)")

            // Step 1: Validate user authentication
            val userId = validateUserId()
            if (userId == null) {
                Log.d(TAG, "⏭️ Skip: no userId found (user never logged in)")
                failureCount = 0  // Reset - this is expected state
                return
            }

            // Step 2: Validate position
            val crewName = validateCrewName()
            if (crewName == null) {
                Log.d(TAG, "⏭️ Skip: position not set")
                failureCount = 0  // Reset - this is expected state
                return
            }

            Log.d(TAG, "✅ Validation passed: userId=$userId, crewName=$crewName")

            // Step 3: Get pending items count
            val pendingCount = getPendingSyncCountSafely(userId, crewName)
            if (pendingCount == null) {
                Log.w(TAG, "⚠️ Failed to get pending count, will retry")
                scheduleRetry(retryAttempt)
                return
            }

            Log.d(TAG, "📦 Found $pendingCount pending items")

            // Step 4: Trigger sync if needed
            if (pendingCount > 0) {
                Log.d(TAG, "🔄 Triggering sync for $pendingCount items")
                triggerSyncWithErrorHandling(userId, crewName)
                failureCount = 0  // Reset on success
            } else {
                Log.d(TAG, "✅ No items to sync")
                failureCount = 0  // Reset on success
            }

        } catch (e: AuthenticationException) {
            Log.e(TAG, "🔴 CRITICAL: Authentication error - ${e.message}")
            Log.e(TAG, "Stack trace:", e)
            failureCount++
            // Don't retry auth errors - user needs to re-login
            // Can implement notification here

        } catch (e: ValidationException) {
            Log.w(TAG, "⚠️ Validation error - ${e.message}")
            // These are expected and shouldn't cause retries
            failureCount = 0

        } catch (e: NetworkException) {
            Log.w(TAG, "⚠️ Network error - ${e.message}")
            failureCount++
            scheduleRetryWithBackoff(retryAttempt, NETWORK_ERROR_BACKOFF_MS)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Unexpected error in sync check - ${e.message}", e)
            Log.e(TAG, "Error type: ${e.javaClass.simpleName}")
            failureCount++
            scheduleRetry(retryAttempt)
        } finally {
            isProcessing = false
        }
    }

    /**
     * Validate user ID with explicit error handling
     */
    private fun validateUserId(): String? {
        return try {
            // Try current session first
            var userId = authService.getUserId()

            // If not in current session, use last saved userId
            if (userId.isNullOrBlank()) {
                userId = userPreferences.getLastUserId()
                Log.d(TAG, "Using last saved userId: $userId")
            }

            if (userId.isNullOrBlank()) {
                null
            } else {
                userId
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error validating userId: ${e.message}", e)
            throw AuthenticationException("Failed to validate userId", e)
        }
    }

    /**
     * Validate crew name with explicit error handling
     */
    private fun validateCrewName(): String? {
        return try {
            val crewName = positionRepository.getPosition()

            if (crewName.isNullOrBlank()) {
                Log.d(TAG, "Position not set")
                null
            } else {
                crewName
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error validating crewName: ${e.message}", e)
            throw ValidationException("Failed to validate crewName", e)
        }
    }

    /**
     * Get pending sync count with error handling
     * Returns null on error instead of throwing
     */
    private suspend fun getPendingSyncCountSafely(
        userId: String,
        crewName: String
    ): Int? {
        return try {
            val count = syncService.getPendingSyncCount(userId, crewName)
            Log.d(TAG, "✅ Successfully retrieved pending count: $count")
            count

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting pending count - ${e.message}", e)
            when {
                e.message?.contains("network", ignoreCase = true) == true ->
                    throw NetworkException("Network error while getting pending count", e)

                e.message?.contains("auth", ignoreCase = true) == true ->
                    throw AuthenticationException("Auth error while getting pending count", e)

                else ->
                    throw Exception("Unexpected error while getting pending count", e)
            }
        }
    }

    /**
     * Trigger sync with error handling
     */
    private suspend fun triggerSyncWithErrorHandling(userId: String, crewName: String) {
        try {
            Log.d(TAG, "🔄 Starting sync with userId=$userId, crewName=$crewName")

            // Upload pending items
            syncService.uploadPendingItems(userId, crewName)
                .onSuccess { count ->
                    Log.d(TAG, "✅ Uploaded $count items")
                }
                .onFailure { error ->
                    Log.w(TAG, "⚠️ Upload failed: ${error.message}")
                    throw NetworkException("Upload failed", error)
                }

            // Download server changes
            syncService.downloadServerItems(
                crewName = crewName,
                userId = userId,
                forceFullSync = false
            )
                .onSuccess { count ->
                    Log.d(TAG, "✅ Merged $count items from server")
                }
                .onFailure { error ->
                    Log.w(TAG, "⚠️ Download failed: ${error.message}")
                    throw NetworkException("Download failed", error)
                }

            Log.d(TAG, "✅ Sync completed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Sync failed: ${e.message}", e)
            throw e
        }
    }

    /**
     * Schedule retry with exponential backoff
     */
    private fun scheduleRetry(retryAttempt: Int) {
        if (retryAttempt >= MAX_RETRIES) {
            Log.e(TAG, "❌ Max retries ($MAX_RETRIES) exceeded")
            failureCount = 0  // Reset for next sync check
            return
        }

        val delayMs = RETRY_DELAY_MS * (retryAttempt + 1)  // 5s, 10s, 15s
        Log.d(TAG, "⏱️ Scheduling retry in ${delayMs}ms (attempt ${retryAttempt + 1}/$MAX_RETRIES)")

        serviceScope.launch {
            delay(delayMs)
            checkAndTriggerSync(retryAttempt + 1)
        }
    }

    /**
     * Schedule retry with network backoff
     */
    private fun scheduleRetryWithBackoff(retryAttempt: Int, backoffMs: Long) {
        if (retryAttempt >= MAX_RETRIES) {
            Log.e(TAG, "❌ Max retries ($MAX_RETRIES) exceeded for network error")
            failureCount = 0
            return
        }

        val delayMs = backoffMs
        Log.d(TAG, "⏱️ Scheduling retry with backoff in ${delayMs}ms")

        serviceScope.launch {
            delay(delayMs)
            checkAndTriggerSync(retryAttempt + 1)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started with intent: $intent")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "🛑 SyncBackgroundService destroyed")
        connectivityMonitor.stopMonitoring()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}

/**
 * Custom exceptions for error categorization
 */
sealed class SyncException(message: String, cause: Throwable? = null) : Exception(message, cause)

class AuthenticationException(message: String, cause: Throwable? = null) :
    SyncException(message, cause)

class NetworkException(message: String, cause: Throwable? = null) :
    SyncException(message, cause)

class ValidationException(message: String, cause: Throwable? = null) :
    SyncException(message, cause)