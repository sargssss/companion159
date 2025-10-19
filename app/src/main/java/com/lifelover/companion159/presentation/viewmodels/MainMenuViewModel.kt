package com.lifelover.companion159.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelover.companion159.data.remote.sync.SyncOrchestrator
import com.lifelover.companion159.data.remote.sync.SyncState
import com.lifelover.companion159.data.repository.PositionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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
    private val syncOrchestrator: SyncOrchestrator,
    private val positionRepository: PositionRepository
) : ViewModel() {

    val syncState: StateFlow<SyncState> = syncOrchestrator.syncState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SyncState()
        )

    val currentPosition: StateFlow<String?> = positionRepository.currentPosition
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun triggerManualSync() {
        syncOrchestrator.forceFullSync()
    }

    fun clearError() {
        syncOrchestrator.clearError()
    }
}