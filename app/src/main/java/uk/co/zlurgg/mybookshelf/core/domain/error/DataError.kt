package uk.co.zlurgg.mybookshelf.core.domain.error

sealed interface DataError: Error {
    enum class Remote: DataError {
        REQUEST_TIMEOUT,
        TOO_MANY_REQUESTS,
        NO_INTERNET,
        SERVER_ERROR,
        CLIENT_ERROR,
        UNAUTHORIZED,
        FORBIDDEN,
        NOT_FOUND,
        SERIALIZATION,
        MALFORMED_REQUEST,
        UNKNOWN
    }

    enum class Local: DataError {
        DISK_FULL,
        STORAGE_ACCESS_DENIED,
        DATABASE_ERROR,
        INVALID_INPUT,
        DUPLICATE_ENTRY,
        NOT_FOUND,
        SERIALIZATION_ERROR,
        VALIDATION_ERROR,
        NAME_CONFLICT,
        SHARE_FAILED,
        SHARE_LINK_TOO_LARGE,
        AUTH_CANCELLED,
        AUTH_NO_CREDENTIAL,
        AUTH_FAILED,
        AUTH_NETWORK_ERROR,
        UNKNOWN
    }

    enum class Validation: DataError {
        EMPTY_FIELD,
        INVALID_FORMAT,
        TOO_SHORT,
        TOO_LONG,
        INVALID_CHARACTERS,
        DUPLICATE_VALUE
    }

    enum class Sync: DataError {
        NOT_SIGNED_IN,
        SYNC_IN_PROGRESS,
        CONFLICT_UNRESOLVED,
        MIGRATION_FAILED,
        QUOTA_EXCEEDED,
        PERMISSION_DENIED,
        DOCUMENT_NOT_FOUND,
        NETWORK_ERROR,
        UNKNOWN
    }
}