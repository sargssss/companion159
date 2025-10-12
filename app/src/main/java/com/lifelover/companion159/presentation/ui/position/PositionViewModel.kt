package com.lifelover.companion159.presentation.ui.position

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifelover.companion159.data.repository.PositionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PositionState(
    val currentPosition: String? = null,
    val isLoading: Boolean = false,
    val isPositionSaved: Boolean = false,
    val error: String? = null
)

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
     * Save position
     */
    fun savePosition(position: String) {
        if (position.isBlank()) {
            _state.update { it.copy(error = "Позиція не може бути порожньою") }
            return
        }

        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }

                // Save position
                positionRepository.savePosition(position)

                _state.update {
                    it.copy(
                        isLoading = false,
                        isPositionSaved = true,
                        currentPosition = position
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Помилка збереження: ${e.message}"
                    )
                }
            }
        }
    }
}