package com.lifelover.companion159.domain.models

/**
 * Represents the result of inventory operations with detailed error information
 * Replaces generic Result<Unit> for better error handling
 */
sealed class InventoryResult {
    /**
     * Operation completed successfully
     */
    data object Success : InventoryResult()

    /**
     * Operation failed due to network issues
     */
    data class NetworkError(
        val message: String = "Немає з'єднання з інтернетом"
    ) : InventoryResult()

    /**
     * Operation failed due to validation issues
     */
    data class ValidationError(
        val field: String,
        val message: String
    ) : InventoryResult()

    /**
     * Operation failed due to permission issues
     */
    data class PermissionError(
        val message: String = "Недостатньо прав для виконання операції"
    ) : InventoryResult()

    /**
     * Generic error with custom message
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : InventoryResult()
}

/**
 * Extension: Check if result is successful
 */
fun InventoryResult.isSuccess(): Boolean = this is InventoryResult.Success

/**
 * Extension: Get error message if result is not successful
 */
fun InventoryResult.getErrorMessage(): String? = when (this) {
    is InventoryResult.Success -> null
    is InventoryResult.NetworkError -> message
    is InventoryResult.ValidationError -> message
    is InventoryResult.PermissionError -> message
    is InventoryResult.Error -> message
}