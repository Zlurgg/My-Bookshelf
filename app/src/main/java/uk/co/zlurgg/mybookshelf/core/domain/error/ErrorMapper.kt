package uk.co.zlurgg.mybookshelf.core.domain.error

import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.core.data.network.HttpStatusCodes
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
            HttpStatusCodes.BAD_REQUEST -> DataError.Remote.CLIENT_ERROR
            HttpStatusCodes.UNAUTHORIZED -> DataError.Remote.UNAUTHORIZED
            HttpStatusCodes.FORBIDDEN -> DataError.Remote.FORBIDDEN
            HttpStatusCodes.NOT_FOUND -> DataError.Remote.NOT_FOUND
            HttpStatusCodes.REQUEST_TIMEOUT -> DataError.Remote.REQUEST_TIMEOUT
            HttpStatusCodes.UNPROCESSABLE_ENTITY -> DataError.Remote.MALFORMED_REQUEST
            HttpStatusCodes.TOO_MANY_REQUESTS -> DataError.Remote.TOO_MANY_REQUESTS
            in HttpStatusCodes.SERVER_ERROR_RANGE -> DataError.Remote.SERVER_ERROR
            else -> DataError.Remote.UNKNOWN
        }
    }

    /**
     * Wraps a synchronous operation with exception handling and logging.
     * Converts any exception to a typed DataError.Local.
     *
     * @param tag Identifier for logging (e.g., class or operation name)
     * @param action The operation to execute
     */
    @Suppress("TooGenericExceptionCaught") // Intentional: converts all exceptions to Result.Error
    inline fun <T> safeCall(
        tag: String = "ErrorMapper",
        action: () -> T
    ): Result<T, DataError.Local> {
        return try {
            Result.Success(action())
        } catch (e: Exception) {
            val error = mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN
            Timber.tag(tag).e(e, "Operation failed - Mapped to: %s", error)
            Result.Error(error)
        }
    }

    /**
     * Wraps a suspend operation with exception handling and logging.
     * Use this for coroutine-based operations (database, network, etc.)
     *
     * @param tag Identifier for logging (e.g., class or operation name)
     * @param action The suspend operation to execute
     */
    @Suppress("TooGenericExceptionCaught") // Intentional: converts all exceptions to Result.Error
    suspend inline fun <T> safeSuspendCall(
        tag: String = "ErrorMapper",
        action: () -> T
    ): Result<T, DataError.Local> {
        return try {
            Result.Success(action())
        } catch (e: Exception) {
            coroutineContext.ensureActive()
            val error = mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN
            Timber.tag(tag).e(e, "Operation failed - Mapped to: %s", error)
            Result.Error(error)
        }
    }

    /**
     * HTTP-specific network call that handles Ktor HTTP operations.
     * Combines exception handling with HTTP status code analysis.
     */
    @Suppress("TooGenericExceptionCaught") // Intentional: converts all exceptions to Result.Error
    suspend inline fun <reified T> httpNetworkCall(
        execute: () -> HttpResponse
    ): Result<T, DataError.Remote> {
        val response = try {
            execute()
        } catch (e: Exception) {
            coroutineContext.ensureActive()

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
    @Suppress("TooGenericExceptionCaught") // Intentional: converts deserialization errors to Result.Error
    suspend inline fun <reified T> responseToResult(
        response: HttpResponse
    ): Result<T, DataError.Remote> {
        return when (response.status.value) {
            in HttpStatusCodes.SUCCESS_RANGE -> {
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
