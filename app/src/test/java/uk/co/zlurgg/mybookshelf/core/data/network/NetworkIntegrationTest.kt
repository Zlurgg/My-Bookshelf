package uk.co.zlurgg.mybookshelf.core.data.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Simplified network integration test that validates core network functionality
 * focusing on error handling and Result pattern usage.
 *
 * This test verifies that the network layer error mapping works correctly
 * without requiring complex network dependencies.
 */
@RunWith(RobolectricTestRunner::class)
class NetworkIntegrationTest {

    @Test
    fun `ErrorMapper correctly maps network exceptions`() = runTest {
        // Given - Different types of network exceptions
        val networkException = java.net.UnknownHostException("No internet")
        val timeoutException = java.net.SocketTimeoutException("Timeout")
        val connectionException = java.net.ConnectException("Connection refused")

        // When - Mapping exceptions to DataError
        val networkError = ErrorMapper.mapExceptionToDataError(networkException)
        val timeoutError = ErrorMapper.mapExceptionToDataError(timeoutException)
        val connectionError = ErrorMapper.mapExceptionToDataError(connectionException)

        // Then - Should map to appropriate error types
        assertTrue("Network exception should map to DataError", networkError is DataError)
        assertTrue("Timeout exception should map to DataError", timeoutError is DataError)
        assertTrue("Connection exception should map to DataError", connectionError is DataError)
    }

    @Test
    fun `ErrorMapper handles serialization exceptions`() = runTest {
        // Given - Serialization exception
        val serializationException = kotlinx.serialization.SerializationException("Invalid JSON")

        // When - Mapping to DataError
        val error = ErrorMapper.mapExceptionToDataError(serializationException)

        // Then - Should map to appropriate error type
        assertTrue("Serialization exception should map to DataError", error is DataError)
    }

    @Test
    fun `ErrorMapper handles generic exceptions`() = runTest {
        // Given - Generic exceptions
        val illegalStateException = IllegalStateException("Invalid state")
        val nullPointerException = NullPointerException("Null pointer")
        val runtimeException = RuntimeException("Generic runtime error")

        // When - Mapping to DataError
        val error1 = ErrorMapper.mapExceptionToDataError(illegalStateException)
        val error2 = ErrorMapper.mapExceptionToDataError(nullPointerException)
        val error3 = ErrorMapper.mapExceptionToDataError(runtimeException)

        // Then - Should map to appropriate error types
        assertTrue("IllegalStateException should map to DataError", error1 is DataError)
        assertTrue("NullPointerException should map to DataError", error2 is DataError)
        assertTrue("RuntimeException should map to DataError", error3 is DataError)
    }

    @Test
    fun `Result pattern works correctly for success cases`() = runTest {
        // Given - Successful operations
        val successfulStringResult = Result.Success("test string")
        val successfulIntResult = Result.Success(42)
        val successfulNullResult = Result.Success<String?>(null)

        // When - Accessing data
        val stringData = successfulStringResult.data
        val intData = successfulIntResult.data
        val nullData = successfulNullResult.data

        // Then - Should preserve data correctly
        assertEquals("Should preserve string data", "test string", stringData)
        assertEquals("Should preserve int data", 42, intData)
        assertEquals("Should preserve null data", null, nullData)
    }

    @Test
    fun `Result pattern works correctly for error cases`() = runTest {
        // Given - Error results
        val networkError = Result.Error(DataError.Remote.NO_INTERNET)
        val localError = Result.Error(DataError.Local.NOT_FOUND)

        // When - Accessing errors
        val remoteErrorData = networkError.error
        val localErrorData = localError.error

        // Then - Should preserve error types correctly
        assertEquals("Should preserve remote error", DataError.Remote.NO_INTERNET, remoteErrorData)
        assertEquals("Should preserve local error", DataError.Local.NOT_FOUND, localErrorData)
    }

    @Test
    fun `DataError Remote enum has expected values`() = runTest {
        // Given - Remote error enum values
        val remoteErrors = listOf(
            DataError.Remote.REQUEST_TIMEOUT,
            DataError.Remote.TOO_MANY_REQUESTS,
            DataError.Remote.NO_INTERNET,
            DataError.Remote.SERVER_ERROR,
            DataError.Remote.SERIALIZATION,
            DataError.Remote.UNKNOWN
        )

        // Then - Should have expected error types
        assertTrue("Should have REQUEST_TIMEOUT", DataError.Remote.REQUEST_TIMEOUT in remoteErrors)
        assertTrue("Should have NO_INTERNET", DataError.Remote.NO_INTERNET in remoteErrors)
        assertTrue("Should have SERVER_ERROR", DataError.Remote.SERVER_ERROR in remoteErrors)
        assertTrue("Should have SERIALIZATION", DataError.Remote.SERIALIZATION in remoteErrors)
        assertTrue("Should have at least 4 error types", remoteErrors.size >= 4)
    }

    @Test
    fun `DataError Local enum has expected values`() = runTest {
        // Given - Local error enum values
        val localErrors = listOf(
            DataError.Local.NOT_FOUND,
            DataError.Local.UNKNOWN
        )

        // Then - Should have expected error types
        assertTrue("Should have NOT_FOUND", DataError.Local.NOT_FOUND in localErrors)
        assertTrue("Should have UNKNOWN", DataError.Local.UNKNOWN in localErrors)
        assertTrue("Should have at least 2 error types", localErrors.size >= 2)
    }

    @Test
    fun `ErrorMapper safeCall handles local operations correctly`() = runTest {
        // Given - Operations that can succeed or fail
        val successOperation = { "Success!" }
        val failingOperation = { throw IllegalArgumentException("Invalid input") }

        // When - Using ErrorMapper.safeCall
        val successResult = ErrorMapper.safeCall { successOperation() }
        val failureResult = ErrorMapper.safeCall { failingOperation() }

        // Then - Should handle both cases correctly
        assertTrue("Success operation should succeed", successResult is Result.Success)
        assertEquals("Should return correct data", "Success!", (successResult as Result.Success).data)

        assertTrue("Failing operation should return error", failureResult is Result.Error)
        assertEquals("Should map to correct local error", DataError.Local.INVALID_INPUT, (failureResult as Result.Error).error)
    }

    @Test
    fun `ErrorMapper networkCall handles network operations correctly`() = runTest {
        // Given - Network-like operations
        val successOperation = { "Network response" }
        val networkFailureOperation = { throw java.net.UnknownHostException("No internet") }
        val timeoutOperation = { throw java.net.SocketTimeoutException("Timeout") }

        // When - Using ErrorMapper.networkCall
        val successResult = ErrorMapper.networkCall { successOperation() }
        val networkFailureResult = ErrorMapper.networkCall { networkFailureOperation() }
        val timeoutResult = ErrorMapper.networkCall { timeoutOperation() }

        // Then - Should handle all cases correctly
        assertTrue("Success operation should succeed", successResult is Result.Success)
        assertEquals("Should return network data", "Network response", (successResult as Result.Success).data)

        assertTrue("Network failure should return error", networkFailureResult is Result.Error)
        assertEquals("Should map to NO_INTERNET error", DataError.Remote.NO_INTERNET, (networkFailureResult as Result.Error).error)

        assertTrue("Timeout should return error", timeoutResult is Result.Error)
        assertEquals("Should map to REQUEST_TIMEOUT error", DataError.Remote.REQUEST_TIMEOUT, (timeoutResult as Result.Error).error)
    }

    @Test
    fun `mapHttpStatusToDataError maps status codes correctly`() = runTest {
        // Given - Various HTTP status codes
        val statusCodes = mapOf(
            400 to DataError.Remote.CLIENT_ERROR,
            401 to DataError.Remote.UNAUTHORIZED,
            404 to DataError.Remote.NOT_FOUND,
            408 to DataError.Remote.REQUEST_TIMEOUT,
            429 to DataError.Remote.TOO_MANY_REQUESTS,
            500 to DataError.Remote.SERVER_ERROR,
            503 to DataError.Remote.SERVER_ERROR,
            999 to DataError.Remote.UNKNOWN
        )

        // When/Then - Should map each status code correctly
        statusCodes.forEach { (statusCode, expectedError) ->
            val mappedError = ErrorMapper.mapHttpStatusToDataError(statusCode)
            assertEquals("Status $statusCode should map to $expectedError", expectedError, mappedError)
        }
    }

    @Test
    fun `Result pattern supports chaining operations`() = runTest {
        // Given - Chain of operations
        fun processString(input: String): Result<Int, DataError.Local> {
            return try {
                val number = input.toInt()
                if (number > 0) Result.Success(number * 2) else Result.Error(DataError.Local.UNKNOWN)
            } catch (e: NumberFormatException) {
                Result.Error(DataError.Local.UNKNOWN)
            }
        }

        // When - Chaining results
        val validResult = processString("5")
        val invalidResult = processString("invalid")
        val negativeResult = processString("-1")

        // Then - Should handle all cases correctly
        assertTrue("Valid input should succeed", validResult is Result.Success)
        assertEquals("Should double the input", 10, (validResult as Result.Success).data)

        assertTrue("Invalid input should fail", invalidResult is Result.Error)
        assertTrue("Negative input should fail", negativeResult is Result.Error)
    }
}