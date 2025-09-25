package uk.co.zlurgg.mybookshelf.core.domain

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorMapper {

    fun mapExceptionToDataError(exception: Exception): DataError {
        return when (exception) {
            // Network-related exceptions
            is SocketTimeoutException -> DataError.Remote.REQUEST_TIMEOUT
            is UnknownHostException -> DataError.Remote.NO_INTERNET
            is IOException -> DataError.Remote.UNKNOWN

            // Database/Storage exceptions
            is SecurityException -> DataError.Local.STORAGE_ACCESS_DENIED
            is IllegalArgumentException -> DataError.Local.INVALID_INPUT
            is IllegalStateException -> DataError.Local.DATABASE_ERROR

            // Generic mapping
            else -> DataError.Local.UNKNOWN
        }
    }

    fun mapHttpStatusToDataError(statusCode: Int): DataError.Remote {
        return when (statusCode) {
            400 -> DataError.Remote.CLIENT_ERROR
            401 -> DataError.Remote.UNAUTHORIZED
            403 -> DataError.Remote.FORBIDDEN
            404 -> DataError.Remote.NOT_FOUND
            408 -> DataError.Remote.REQUEST_TIMEOUT
            422 -> DataError.Remote.MALFORMED_REQUEST
            429 -> DataError.Remote.TOO_MANY_REQUESTS
            in 500..599 -> DataError.Remote.SERVER_ERROR
            else -> DataError.Remote.UNKNOWN
        }
    }

    inline fun <T> safeCall(action: () -> T): Result<T, DataError.Local> {
        return try {
            Result.Success(action())
        } catch (e: Exception) {
            Result.Error(mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN)
        }
    }

    inline fun <T> networkCall(action: () -> T): Result<T, DataError.Remote> {
        return try {
            Result.Success(action())
        } catch (e: Exception) {
            Result.Error(mapExceptionToDataError(e) as? DataError.Remote ?: DataError.Remote.UNKNOWN)
        }
    }
}