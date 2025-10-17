package com.lifelover.companion159.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelover.companion159.data.remote.sync.SyncManager
import com.lifelover.companion159.data.remote.sync.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for MainMenuScreen
 *
 * Responsibilities:
 * - Expose sync state to UI
 * - Trigger manual full sync from button
 */
@HiltViewModel
class MainMenuViewModel @Inject constructor(
    private val syncManager: SyncManager
) : ViewModel() {

    /**
     * Observe sync state for UI
     * Shows loading indicator and last sync time
     */
    val syncState: StateFlow<SyncState> = syncManager.syncState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SyncState()
        )

    /**
     * Trigger manual full sync from UI button
     * Downloads all items and uploads pending changes
     */
    fun triggerManualSync() {
        viewModelScope.launch {
            syncManager.forceFullSync()
        }
    }
}