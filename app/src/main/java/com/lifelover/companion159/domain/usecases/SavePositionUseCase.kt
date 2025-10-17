package com.lifelover.companion159.domain.usecases

import com.lifelover.companion159.data.repository.PositionRepository
import com.lifelover.companion159.domain.validation.InputValidator
import javax.inject.Inject

class SavePositionUseCase @Inject constructor(
    private val positionRepository: PositionRepository
) {
    suspend operator fun invoke(position: String): Result<Unit> {
        return try {
            // Validate
            val validationResult = InputValidator.validatePosition(position)

            validationResult.fold(
                onSuccess = { validPosition ->
                    positionRepository.savePosition(validPosition)
                    Result.success(Unit)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}