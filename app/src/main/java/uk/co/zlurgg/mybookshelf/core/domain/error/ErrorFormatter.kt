package uk.co.zlurgg.mybookshelf.core.domain.error

object ErrorFormatter {

    /**
     * Format a DataError into a user-friendly error message without requiring Context.
     * This method uses hardcoded English strings and is suitable for ViewModels that don't have Context.
     *
     * @param error The DataError to format
     * @param operation Optional operation description (e.g., "remove shelf")
     * @return Formatted error message
     */
    fun formatDataErrorMessage(error: DataError, operation: String = ""): String {
        val errorMessage = when (error) {
            is DataError.Remote -> formatRemoteError(error)
            is DataError.Local -> formatLocalError(error)
            is DataError.Validation -> formatValidationError(error)
            is DataError.Sync -> formatSyncError(error)
        }

        return if (operation.isNotBlank()) {
            "Failed to $operation: $errorMessage"
        } else {
            errorMessage
        }
    }

    private fun formatRemoteError(error: DataError.Remote): String = when (error) {
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
    }

    private fun formatLocalError(error: DataError.Local): String = when (error) {
        DataError.Local.DISK_FULL -> "Storage space is full."
        DataError.Local.STORAGE_ACCESS_DENIED -> "Storage access denied."
        DataError.Local.DATABASE_ERROR -> "Database operation failed."
        DataError.Local.INVALID_INPUT -> "Invalid input provided."
        DataError.Local.DUPLICATE_ENTRY -> "Entry already exists."
        DataError.Local.NOT_FOUND -> "Item not found."
        DataError.Local.SERIALIZATION_ERROR -> "Data processing error."
        DataError.Local.VALIDATION_ERROR -> "Data validation failed."
        DataError.Local.NAME_CONFLICT -> "Name already exists."
        DataError.Local.SHARE_FAILED -> "Sharing failed."
        DataError.Local.SHARE_LINK_TOO_LARGE ->
            "This bookshelf is too large to share. " +
                "Try sharing a smaller shelf or splitting it into multiple shelves."
        DataError.Local.PERMISSION_DENIED ->
            "You don't have permission to perform this action."
        DataError.Local.MAX_SHELVES_REACHED -> "You've reached the maximum of 20 shelves."
        DataError.Local.MAX_BOOKS_REACHED ->
            "This shelf has reached its maximum of 20 books."
        DataError.Local.UNKNOWN -> "Unknown local error occurred."
        DataError.Local.AUTH_CANCELLED,
        DataError.Local.AUTH_NO_CREDENTIAL,
        DataError.Local.AUTH_FAILED,
        DataError.Local.AUTH_NETWORK_ERROR,
        -> formatLocalAuthError(error)
    }

    private fun formatLocalAuthError(error: DataError.Local): String = when (error) {
        DataError.Local.AUTH_CANCELLED -> "Sign-in was cancelled."
        DataError.Local.AUTH_NO_CREDENTIAL ->
            "No Google account found. Please add a Google account to your device."
        DataError.Local.AUTH_FAILED -> "Sign-in failed. Please try again."
        DataError.Local.AUTH_NETWORK_ERROR ->
            "Network error during sign-in. Please check your connection."
        else -> "Authentication error occurred."
    }

    private fun formatValidationError(error: DataError.Validation): String = when (error) {
        DataError.Validation.EMPTY_FIELD -> "Required field is empty."
        DataError.Validation.INVALID_FORMAT -> "Invalid format."
        DataError.Validation.TOO_SHORT -> "Input is too short."
        DataError.Validation.TOO_LONG -> "Input is too long."
        DataError.Validation.INVALID_CHARACTERS -> "Contains invalid characters."
        DataError.Validation.DUPLICATE_VALUE -> "Value already exists."
        DataError.Validation.INVALID_CLUB_CODE -> "Invalid book club code format."
    }

    private fun formatSyncError(error: DataError.Sync): String = when (error) {
        DataError.Sync.NOT_SIGNED_IN -> "Please sign in to sync your data."
        DataError.Sync.SYNC_IN_PROGRESS -> "Sync is already in progress."
        DataError.Sync.CONFLICT_UNRESOLVED ->
            "There are unresolved conflicts that require your attention."
        DataError.Sync.MIGRATION_FAILED -> "Failed to migrate local data. Please try again."
        DataError.Sync.QUOTA_EXCEEDED -> "Cloud storage quota exceeded."
        DataError.Sync.PERMISSION_DENIED -> "Permission denied. Please sign in again."
        DataError.Sync.DOCUMENT_NOT_FOUND -> "Requested data not found in cloud."
        DataError.Sync.NETWORK_ERROR ->
            "Network error during sync. Please check your connection."
        DataError.Sync.GENERATION_FAILED ->
            "Unable to generate book club code. Please try again."
        DataError.Sync.CLUB_NOT_FOUND -> "Book club not found. Check the code and try again."
        DataError.Sync.ALREADY_MEMBER -> "You're already a member of this book club."
        DataError.Sync.NOT_MEMBER -> "You're not a member of this book club."
        DataError.Sync.CREATOR_CANNOT_LEAVE ->
            "As the creator, you cannot leave. Delete the club instead."
        DataError.Sync.MAX_BOOK_CLUBS_REACHED -> "You can have a maximum of 5 book clubs."
        DataError.Sync.INVALID_INPUT -> "Invalid input. Please check your data."
        DataError.Sync.UNKNOWN -> "Unknown sync error occurred."
    }
}
