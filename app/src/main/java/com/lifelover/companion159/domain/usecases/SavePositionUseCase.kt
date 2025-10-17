package com.lifelover.companion159.domain.usecases

import com.lifelover.companion159.data.repository.PositionRepository
import com.lifelover.companion159.domain.validation.InputValidator
import javax.inject.Inject

/**
 * Use case for saving user position/crew name
 *
 * Business rules:
 * - Position must be 2-50 characters
 * - Position saved to both local DB and Supabase metadata
 * - Position determines which items user can access
 *
 * @param positionRepository Position repository for data persistence
 */
class SavePositionUseCase @Inject constructor(
    private val positionRepository: PositionRepository
) {
    suspend operator fun invoke(position: String): Result<Unit> {
        return try {
            // Validate position
            val validationResult = InputValidator.validatePosition(position)

            validationResult.fold(
                onSuccess = { validPosition ->
                    // Save to repository
                    // Repository handles both local DB and Supabase metadata
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