package com.lifelover.companion159.domain.validation

import com.lifelover.companion159.domain.models.AppError

/**
 * Centralized input validation logic
 */
object InputValidator {

    /**
     * Validate item name
     * Rules:
     * - Cannot be blank
     * - Length between 1-100 characters
     */
    fun validateItemName(name: String): Result<String> {
        val trimmed = name.trim()

        return when {
            trimmed.isBlank() -> Result.failure(
                AppError.Validation.EmptyField(fieldName = "name")
            )

            trimmed.length > 100 -> Result.failure(
                AppError.Validation.OutOfRange(
                    fieldName = "name",
                    min = 1,
                    max = 100
                )
            )

            else -> Result.success(trimmed)
        }
    }

    /**
     * Validate quantity
     * Rules:
     * - Must be non-negative
     * - Maximum value: 999999
     */
    fun validateQuantity(quantity: Int, fieldName: String = "quantity"): Result<Int> {
        return when {
            quantity < 0 -> Result.failure(
                AppError.Validation.OutOfRange(
                    fieldName = fieldName,
                    min = 0,
                    max = null
                )
            )

            quantity > 999999 -> Result.failure(
                AppError.Validation.OutOfRange(
                    fieldName = fieldName,
                    min = 0,
                    max = 999999
                )
            )

            else -> Result.success(quantity)
        }
    }

    /**
     * Validate and format position/crew name
     * Rules:
     * - Cannot be blank
     * - Length between 2-50 characters
     * - First letter uppercase, rest lowercase
     * - Trim whitespace
     *
     * @param position Raw position input
     * @return Result with formatted position string
     */
    fun validatePosition(position: String): Result<String> {
        val trimmed = position.trim()

        return when {
            trimmed.isBlank() -> Result.failure(
                AppError.Validation.EmptyField(fieldName = "position")
            )

            trimmed.length < 2 -> Result.failure(
                AppError.Validation.OutOfRange(
                    fieldName = "position",
                    min = 2,
                    max = 50
                )
            )

            trimmed.length > 50 -> Result.failure(
                AppError.Validation.OutOfRange(
                    fieldName = "position",
                    min = 2,
                    max = 50
                )
            )

            else -> Result.success(formatPosition(trimmed))
        }
    }

    /**
     * Format position string
     * Rules:
     * - First letter uppercase
     * - Rest lowercase
     * - Already trimmed (caller must trim first)
     *
     * @param position Valid trimmed position string
     * @return Formatted position
     */
    fun validateNewItem(
        name: String,
        availableQuantity: Int,
        neededQuantity: Int
    ): Result<ValidatedItem> {
        return runCatching {
            ValidatedItem(
                name = validateItemName(name).getOrThrow(),
                availableQuantity = validateQuantity(availableQuantity, "availableQuantity").getOrThrow(),
                neededQuantity = validateQuantity(neededQuantity, "neededQuantity").getOrThrow()
            )
        }
    }

    /**
     * Format position string
     * Rules:
     * - First letter uppercase
     * - Rest lowercase
     * - Already trimmed (caller must trim first)
     *
     * @param position Valid trimmed position string
     * @return Formatted position
     */
    private fun formatPosition(position: String): String {
        return position
            .lowercase()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    /**
     * Validated item data class
     * Guarantees all fields are valid
     */
    data class ValidatedItem(
        val name: String,
        val availableQuantity: Int,
        val neededQuantity: Int
    )
}