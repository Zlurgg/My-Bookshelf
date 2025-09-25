package uk.co.zlurgg.mybookshelf.core.domain

import android.content.Context
import uk.co.zlurgg.mybookshelf.R

object ErrorFormatter {

    fun formatOperationError(operation: String, exception: Exception): String {
        return "Failed to $operation: ${exception.message}"
    }

    fun formatDataError(context: Context, error: DataError, operation: String = ""): String {
        val errorMessageRes = when (error) {
            // Remote errors
            DataError.Remote.REQUEST_TIMEOUT -> R.string.error_request_timeout
            DataError.Remote.TOO_MANY_REQUESTS -> R.string.error_too_many_requests
            DataError.Remote.NO_INTERNET -> R.string.error_no_internet
            DataError.Remote.SERVER_ERROR -> R.string.error_server_error
            DataError.Remote.CLIENT_ERROR -> R.string.error_client_error
            DataError.Remote.UNAUTHORIZED -> R.string.error_unauthorized
            DataError.Remote.FORBIDDEN -> R.string.error_forbidden
            DataError.Remote.NOT_FOUND -> R.string.error_not_found_remote
            DataError.Remote.SERIALIZATION -> R.string.error_serialization_remote
            DataError.Remote.MALFORMED_REQUEST -> R.string.error_malformed_request
            DataError.Remote.UNKNOWN -> R.string.error_unknown_remote

            // Local errors
            DataError.Local.DISK_FULL -> R.string.error_disk_full
            DataError.Local.STORAGE_ACCESS_DENIED -> R.string.error_storage_access_denied
            DataError.Local.DATABASE_ERROR -> R.string.error_database_error
            DataError.Local.INVALID_INPUT -> R.string.error_invalid_input
            DataError.Local.DUPLICATE_ENTRY -> R.string.error_duplicate_entry
            DataError.Local.NOT_FOUND -> R.string.error_not_found_local
            DataError.Local.SERIALIZATION_ERROR -> R.string.error_serialization_local
            DataError.Local.UNKNOWN -> R.string.error_unknown_local

            // Validation errors
            DataError.Validation.EMPTY_FIELD -> R.string.error_empty_field
            DataError.Validation.INVALID_FORMAT -> R.string.error_invalid_format
            DataError.Validation.TOO_SHORT -> R.string.error_too_short
            DataError.Validation.TOO_LONG -> R.string.error_too_long
            DataError.Validation.INVALID_CHARACTERS -> R.string.error_invalid_characters
            DataError.Validation.DUPLICATE_VALUE -> R.string.error_duplicate_value
        }

        val errorMessage = context.getString(errorMessageRes)

        return if (operation.isNotBlank()) {
            context.getString(R.string.error_operation_failed, operation, errorMessage)
        } else {
            errorMessage
        }
    }

    fun formatResultError(context: Context, error: DataError, operation: String): String {
        return formatDataError(context, error, operation)
    }
}