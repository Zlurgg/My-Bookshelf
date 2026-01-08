package uk.co.zlurgg.mybookshelf.core.domain.error

import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorMapper {
    // HTTP Status Codes (internal for inline function access)
    @PublishedApi internal const val HTTP_OK = 200
    @PublishedApi internal const val HTTP_OK_MAX = 299
    private const val HTTP_BAD_REQUEST = 400
    private const val HTTP_UNAUTHORIZED = 401
    private const val HTTP_FORBIDDEN = 403
    private const val HTTP_NOT_FOUND = 404
    private const val HTTP_TIMEOUT = 408
    private const val HTTP_UNPROCESSABLE = 422
    private const val HTTP_TOO_MANY_REQUESTS = 429
    private const val HTTP_SERVER_ERROR_MIN = 500
    private const val HTTP_SERVER_ERROR_MAX = 599

    fun mapExceptionToDataError(exception: Exception): DataError {
        return when (exception) {
            // Network-related exceptions (UnresolvedAddressException and SocketTimeoutException are Ktor-specific)
            is UnresolvedAddressException -> DataError.Remote.NO_INTERNET
            is UnknownHostException -> DataError.Remote.NO_INTERNET
            is SocketTimeoutException -> DataError.Remote.REQUEST_TIMEOUT // Handles both Java and Ktor variants
            is IOException -> DataError.Remote.UNKNOWN

            // Serialization exceptions (check specific types first)
            is NoTransformationFoundException -> DataError.Remote.SERIALIZATION
            is SerializationException -> DataError.Remote.SERIALIZATION

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
            HTTP_BAD_REQUEST -> DataError.Remote.CLIENT_ERROR
            HTTP_UNAUTHORIZED -> DataError.Remote.UNAUTHORIZED
            HTTP_FORBIDDEN -> DataError.Remote.FORBIDDEN
            HTTP_NOT_FOUND -> DataError.Remote.NOT_FOUND
            HTTP_TIMEOUT -> DataError.Remote.REQUEST_TIMEOUT
            HTTP_UNPROCESSABLE -> DataError.Remote.MALFORMED_REQUEST
            HTTP_TOO_MANY_REQUESTS -> DataError.Remote.TOO_MANY_REQUESTS
            in HTTP_SERVER_ERROR_MIN..HTTP_SERVER_ERROR_MAX -> DataError.Remote.SERVER_ERROR
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

    /**
     * HTTP-specific network call that handles Ktor HTTP operations.
     * Combines exception handling with HTTP status code analysis.
     */
    suspend inline fun <reified T> httpNetworkCall(execute: () -> HttpResponse): Result<T, DataError.Remote> {
        val response =
            try {
                execute()
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()

                // Log the actual exception for debugging
                val mappedError = mapExceptionToDataError(e) as? DataError.Remote ?: DataError.Remote.UNKNOWN
                Timber.tag("ErrorMapper").e(e, "HTTP call failed - Mapped to: %s", mappedError)

                return Result.Error(mappedError)
            }

        return responseToResult(response)
    }

    /**
     * Converts HTTP response to Result based on status code and response body.
     * Integrates with mapHttpStatusToDataError for comprehensive status handling.
     */
    suspend inline fun <reified T> responseToResult(response: HttpResponse): Result<T, DataError.Remote> {
        return when (response.status.value) {
            in HTTP_OK..HTTP_OK_MAX -> {
                try {
                    Result.Success(response.body<T>())
                } catch (e: Exception) {
                    val mappedError = mapExceptionToDataError(e) as? DataError.Remote ?: DataError.Remote.SERIALIZATION
                    Timber.tag("ErrorMapper").e(e, "Failed to deserialize HTTP response - Mapped to: %s", mappedError)
                    Result.Error(mappedError)
                }
            }
            else -> {
                val error = mapHttpStatusToDataError(response.status.value)
                Timber.tag("ErrorMapper").w("HTTP error response: %d - Mapped to: %s", response.status.value, error)
                Result.Error(error)
            }
        }
    }
}
