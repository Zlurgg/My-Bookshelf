package uk.co.zlurgg.mybookshelf.core.domain.error

import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.coroutineContext

object ErrorMapper {

    fun mapExceptionToDataError(exception: Exception): DataError {
        return when (exception) {
            // Network-related exceptions (UnresolvedAddressException and SocketTimeoutException are Ktor-specific)
            is UnresolvedAddressException -> DataError.Remote.NO_INTERNET
            is UnknownHostException -> DataError.Remote.NO_INTERNET
            is SocketTimeoutException -> DataError.Remote.REQUEST_TIMEOUT  // Handles both Java and Ktor variants
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

    /**
     * HTTP-specific network call that handles Ktor HTTP operations.
     * Combines exception handling with HTTP status code analysis.
     */
    suspend inline fun <reified T> httpNetworkCall(
        execute: () -> HttpResponse
    ): Result<T, DataError.Remote> {
        val response = try {
            execute()
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            return Result.Error(mapExceptionToDataError(e) as? DataError.Remote ?: DataError.Remote.UNKNOWN)
        }

        return responseToResult(response)
    }

    /**
     * Converts HTTP response to Result based on status code and response body.
     * Integrates with mapHttpStatusToDataError for comprehensive status handling.
     */
    suspend inline fun <reified T> responseToResult(
        response: HttpResponse
    ): Result<T, DataError.Remote> {
        return when (response.status.value) {
            in 200..299 -> {
                try {
                    Result.Success(response.body<T>())
                } catch (e: Exception) {
                    Result.Error(mapExceptionToDataError(e) as? DataError.Remote ?: DataError.Remote.SERIALIZATION)
                }
            }
            else -> Result.Error(mapHttpStatusToDataError(response.status.value))
        }
    }
}