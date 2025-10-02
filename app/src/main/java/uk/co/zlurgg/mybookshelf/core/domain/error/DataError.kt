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
        UNSUPPORTED_FORMAT_VERSION,
        SHARE_FAILED,
        SHARE_LINK_TOO_LARGE,
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
}