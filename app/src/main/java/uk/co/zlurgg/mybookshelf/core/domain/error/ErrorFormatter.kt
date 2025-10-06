package uk.co.zlurgg.mybookshelf.core.domain.error

import android.content.Context
import uk.co.zlurgg.mybookshelf.R

object ErrorFormatter {

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
            DataError.Local.VALIDATION_ERROR -> R.string.error_validation_error
            DataError.Local.NAME_CONFLICT -> R.string.error_name_conflict
            DataError.Local.UNSUPPORTED_FORMAT_VERSION -> R.string.error_unsupported_format_version
            DataError.Local.SHARE_FAILED -> R.string.error_share_failed
            DataError.Local.SHARE_LINK_TOO_LARGE -> R.string.error_share_link_too_large
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

    /**
     * Format a DataError into a user-friendly error message without requiring Context.
     * This method uses hardcoded English strings and is suitable for ViewModels that don't have Context.
     * For proper i18n support, use formatDataError(Context, DataError, String) instead.
     *
     * @param error The DataError to format
     * @param operation Optional operation description (e.g., "remove shelf")
     * @return Formatted error message
     */
    fun formatDataErrorMessage(error: DataError, operation: String = ""): String {
        val errorMessage = when (error) {
            // Remote errors
            DataError.Remote.REQUEST_TIMEOUT -> "Request timed out. Please check your internet connection."
            DataError.Remote.TOO_MANY_REQUESTS -> "Too many requests. Please try again later."
            DataError.Remote.NO_INTERNET -> "No internet connection available."
            DataError.Remote.SERVER_ERROR -> "Server error occurred. Please try again later."
            DataError.Remote.CLIENT_ERROR -> "Request error. Please check your input."
            DataError.Remote.UNAUTHORIZED -> "Authentication required."
            DataError.Remote.FORBIDDEN -> "Access denied."
            DataError.Remote.NOT_FOUND -> "Requested resource not found."
            DataError.Remote.SERIALIZATION -> "Data format error."
            DataError.Remote.MALFORMED_REQUEST -> "Invalid request format."
            DataError.Remote.UNKNOWN -> "Unknown network error occurred."

            // Local errors
            DataError.Local.DISK_FULL -> "Storage space is full."
            DataError.Local.STORAGE_ACCESS_DENIED -> "Storage access denied."
            DataError.Local.DATABASE_ERROR -> "Database operation failed."
            DataError.Local.INVALID_INPUT -> "Invalid input provided."
            DataError.Local.DUPLICATE_ENTRY -> "Entry already exists."
            DataError.Local.NOT_FOUND -> "Item not found."
            DataError.Local.SERIALIZATION_ERROR -> "Data processing error."
            DataError.Local.VALIDATION_ERROR -> "Data validation failed."
            DataError.Local.NAME_CONFLICT -> "Name already exists."
            DataError.Local.UNSUPPORTED_FORMAT_VERSION -> "Unsupported file format version."
            DataError.Local.SHARE_FAILED -> "Sharing failed."
            DataError.Local.SHARE_LINK_TOO_LARGE -> "This bookshelf is too large to share. Try sharing a smaller shelf or splitting it into multiple shelves."
            DataError.Local.UNKNOWN -> "Unknown local error occurred."

            // Validation errors
            DataError.Validation.EMPTY_FIELD -> "Required field is empty."
            DataError.Validation.INVALID_FORMAT -> "Invalid format."
            DataError.Validation.TOO_SHORT -> "Input is too short."
            DataError.Validation.TOO_LONG -> "Input is too long."
            DataError.Validation.INVALID_CHARACTERS -> "Contains invalid characters."
            DataError.Validation.DUPLICATE_VALUE -> "Value already exists."
        }

        return if (operation.isNotBlank()) {
            "Failed to $operation: $errorMessage"
        } else {
            errorMessage
        }
    }
}