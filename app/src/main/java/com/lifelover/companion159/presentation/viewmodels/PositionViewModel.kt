package com.lifelover.companion159.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelover.companion159.data.repository.PositionRepository
import com.lifelover.companion159.domain.models.AppError
import com.lifelover.companion159.domain.validation.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for position management
 */
data class PositionState(
    val currentPosition: String? = null,
    val isLoading: Boolean = false,
    val isPositionSaved: Boolean = false,
    val error: AppError? = null
)

/**
 * ViewModel for position management
 *
 * Responsibilities:
 * - Manage user position/crew name
 * - Validate position input
 * - Provide autocomplete suggestions
 */
@HiltViewModel
class PositionViewModel @Inject constructor(
    private val positionRepository: PositionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PositionState())
    val state: StateFlow<PositionState> = _state.asStateFlow()

    init {
        loadCurrentPosition()
    }

    /**
     * Load current position from repository
     */
    private fun loadCurrentPosition() {
        viewModelScope.launch {
            positionRepository.currentPosition.collect { position ->
                _state.update { it.copy(currentPosition = position) }
            }
        }
    }

    /**
     * Get autocomplete suggestions for input
     */
    fun getAutocompleteSuggestions(input: String): List<String> {
        return positionRepository.getAutocompleteSuggestions(input)
    }

    /**
     * Save position with validation
     */
    fun savePosition(position: String) {
        viewModelScope.launch {
            val validationResult = InputValidator.validatePosition(position)

            validationResult
                .onSuccess { validPosition ->
                    try {
                        _state.update { it.copy(isLoading = true, error = null) }
                        positionRepository.savePosition(validPosition)
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isPositionSaved = true,
                                currentPosition = validPosition
                            )
                        }
                    } catch (e: Exception) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = AppError.Unknown("Save failed", e)
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            error = error as? AppError ?: AppError.Validation.EmptyField("position")
                        )
                    }
                }
        }
    }
}