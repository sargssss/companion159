package com.lifelover.companion159.domain.models

/**
 * Sealed class hierarchy for type-safe error handling
 * Extends Throwable to work with Result<T>
 *
 * Usage:
 * ```
 * when (error) {
 *     is AppError.Network -> showNetworkError()
 *     is AppError.Authentication -> redirectToLogin()
 *     is AppError.Validation -> showValidationError(error.field)
 * }
 * ```
 */
sealed class AppError(
    override val message: String,
    override val cause: Throwable? = null
) : Throwable(message, cause) {
    /**
     * Network-related errors
     */
    sealed class Network(
        message: String,
        cause: Throwable? = null
    ) : AppError(message, cause) {

        data class NoConnection(
            override val cause: Throwable? = null
        ) : Network("No internet connection", cause)

        data class Timeout(
            override val cause: Throwable? = null
        ) : Network("Request timeout", cause)

        data class ServerError(
            val code: Int,
            override val cause: Throwable? = null
        ) : Network("Server error: $code", cause)
    }

    /**
     * Authentication errors
     */
    sealed class Authentication(
        message: String,
        cause: Throwable? = null
    ) : AppError(message, cause) {

        data object NotAuthenticated : Authentication("User not authenticated")

        data object InvalidCredentials : Authentication("Invalid credentials")

        data class GoogleSignInFailed(
            override val cause: Throwable? = null
        ) : Authentication("Google sign-in failed", cause)

        data object SessionExpired : Authentication("Session expired")
    }

    /**
     * Authorization errors
     */
    sealed class Authorization(
        message: String,
        cause: Throwable? = null
    ) : AppError(message, cause) {

        data object InsufficientPermissions : Authorization("Insufficient permissions")

        data class CannotModifyOtherUserData(
            val userId: String
        ) : Authorization("Cannot modify data of user: $userId")
    }

    /**
     * Validation errors
     */
    sealed class Validation(
        val field: String,
        message: String
    ) : AppError(message) {

        data class EmptyField(
            val fieldName: String
        ) : Validation(fieldName, "Field '$fieldName' cannot be empty")

        data class InvalidFormat(
            val fieldName: String,
            val expected: String
        ) : Validation(fieldName, "Field '$fieldName' has invalid format. Expected: $expected")

        data class OutOfRange(
            val fieldName: String,
            val min: Int?,
            val max: Int?
        ) : Validation(fieldName, "Field '$fieldName' is out of range [${min ?: "∞"}, ${max ?: "∞"}]")
    }

    /**
     * Database errors
     */
    sealed class Database(
        message: String,
        cause: Throwable? = null
    ) : AppError(message, cause) {

        data class NotFound(
            val entityType: String,
            val id: Long
        ) : Database("$entityType with id=$id not found")

        data class OperationFailed(
            val operation: String,
            override val cause: Throwable? = null
        ) : Database("Database operation '$operation' failed", cause)
    }

    /**
     * Business logic errors
     */
    sealed class Business(
        message: String
    ) : AppError(message) {

        data object PositionNotSet : Business("Position must be set before performing this action")

        data class ItemAlreadyExists(
            val itemName: String
        ) : Business("Item '$itemName' already exists")
    }

    /**
     * Unknown/unexpected errors
     */
    data class Unknown(
        override val message: String,
        override val cause: Throwable? = null
    ) : AppError(message, cause)
}

/**
 * Extension: Convert Throwable to AppError
 */
fun Throwable.toAppError(): AppError {
    return when (this) {
        is IllegalStateException -> {
            when {
                message?.contains("authenticated", ignoreCase = true) == true ->
                    AppError.Authentication.NotAuthenticated
                message?.contains("position", ignoreCase = true) == true ->
                    AppError.Business.PositionNotSet
                else -> AppError.Unknown(message ?: "Unknown error", this)
            }
        }
        is SecurityException -> AppError.Authorization.InsufficientPermissions
        is IllegalArgumentException -> AppError.Validation.InvalidFormat(
            fieldName = "unknown",
            expected = message ?: "valid input"
        )
        else -> AppError.Unknown(message ?: "Unknown error", this)
    }
}

/**
 * Extension: Get user-friendly error message resource ID
 */
fun AppError.toUserMessage(): Int {
    return when (this) {
        is AppError.Network.NoConnection -> com.lifelover.companion159.R.string.error_no_connection
        is AppError.Network.Timeout -> com.lifelover.companion159.R.string.error_timeout
        is AppError.Network.ServerError -> com.lifelover.companion159.R.string.error_server

        is AppError.Authentication.NotAuthenticated -> com.lifelover.companion159.R.string.error_not_authenticated
        is AppError.Authentication.InvalidCredentials -> com.lifelover.companion159.R.string.error_invalid_credentials
        is AppError.Authentication.GoogleSignInFailed -> com.lifelover.companion159.R.string.error_google_signin_failed
        is AppError.Authentication.SessionExpired -> com.lifelover.companion159.R.string.error_session_expired

        is AppError.Authorization.InsufficientPermissions -> com.lifelover.companion159.R.string.error_insufficient_permissions
        is AppError.Authorization.CannotModifyOtherUserData -> com.lifelover.companion159.R.string.error_cannot_modify_other_user_data

        is AppError.Validation.EmptyField -> com.lifelover.companion159.R.string.error_field_empty
        is AppError.Validation.InvalidFormat -> com.lifelover.companion159.R.string.error_invalid_format
        is AppError.Validation.OutOfRange -> com.lifelover.companion159.R.string.error_out_of_range

        is AppError.Database.NotFound -> com.lifelover.companion159.R.string.error_item_not_found
        is AppError.Database.OperationFailed -> com.lifelover.companion159.R.string.error_database_operation_failed

        is AppError.Business.PositionNotSet -> com.lifelover.companion159.R.string.error_position_not_set
        is AppError.Business.ItemAlreadyExists -> com.lifelover.companion159.R.string.error_item_already_exists

        is AppError.Unknown -> com.lifelover.companion159.R.string.error_unknown
    }
}