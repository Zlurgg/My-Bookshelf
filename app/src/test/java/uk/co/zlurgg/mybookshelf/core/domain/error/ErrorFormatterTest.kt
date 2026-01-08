package uk.co.zlurgg.mybookshelf.core.domain.error

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for ErrorFormatter.
 *
 * Note: Error messages are grouped by category (Remote/Local/Validation) rather than
 * individual tests per error type, since they all follow identical logic (simple when
 * expression lookup). This pragmatic approach reduces 29 tests to 5 without losing coverage.
 */
class ErrorFormatterTest {
    @Test
    fun `formatDataErrorMessage returns correct messages for all Remote errors`() {
        assertEquals(
            "Request timed out. Please check your internet connection.",
            ErrorFormatter.formatDataErrorMessage(DataError.Remote.REQUEST_TIMEOUT),
        )
        assertEquals(
            "Too many requests. Please try again later.",
            ErrorFormatter.formatDataErrorMessage(DataError.Remote.TOO_MANY_REQUESTS),
        )
        assertEquals(
            "No internet connection available.",
            ErrorFormatter.formatDataErrorMessage(DataError.Remote.NO_INTERNET),
        )
        assertEquals(
            "Server error occurred. Please try again later.",
            ErrorFormatter.formatDataErrorMessage(DataError.Remote.SERVER_ERROR),
        )
        assertEquals(
            "Request error. Please check your input.",
            ErrorFormatter.formatDataErrorMessage(DataError.Remote.CLIENT_ERROR),
        )
        assertEquals(
            "Authentication required.",
            ErrorFormatter.formatDataErrorMessage(DataError.Remote.UNAUTHORIZED),
        )
        assertEquals(
            "Access denied.",
            ErrorFormatter.formatDataErrorMessage(DataError.Remote.FORBIDDEN),
        )
        assertEquals(
            "Requested resource not found.",
            ErrorFormatter.formatDataErrorMessage(DataError.Remote.NOT_FOUND),
        )
        assertEquals(
            "Data format error.",
            ErrorFormatter.formatDataErrorMessage(DataError.Remote.SERIALIZATION),
        )
        assertEquals(
            "Invalid request format.",
            ErrorFormatter.formatDataErrorMessage(DataError.Remote.MALFORMED_REQUEST),
        )
        assertEquals(
            "Unknown network error occurred.",
            ErrorFormatter.formatDataErrorMessage(DataError.Remote.UNKNOWN),
        )
    }

    @Test
    fun `formatDataErrorMessage returns correct messages for all Local errors`() {
        assertEquals(
            "Storage space is full.",
            ErrorFormatter.formatDataErrorMessage(DataError.Local.DISK_FULL),
        )
        assertEquals(
            "Storage access denied.",
            ErrorFormatter.formatDataErrorMessage(DataError.Local.STORAGE_ACCESS_DENIED),
        )
        assertEquals(
            "Database operation failed.",
            ErrorFormatter.formatDataErrorMessage(DataError.Local.DATABASE_ERROR),
        )
        assertEquals(
            "Invalid input provided.",
            ErrorFormatter.formatDataErrorMessage(DataError.Local.INVALID_INPUT),
        )
        assertEquals(
            "Entry already exists.",
            ErrorFormatter.formatDataErrorMessage(DataError.Local.DUPLICATE_ENTRY),
        )
        assertEquals(
            "Item not found.",
            ErrorFormatter.formatDataErrorMessage(DataError.Local.NOT_FOUND),
        )
        assertEquals(
            "Data processing error.",
            ErrorFormatter.formatDataErrorMessage(DataError.Local.SERIALIZATION_ERROR),
        )
        assertEquals(
            "Data validation failed.",
            ErrorFormatter.formatDataErrorMessage(DataError.Local.VALIDATION_ERROR),
        )
        assertEquals(
            "Name already exists.",
            ErrorFormatter.formatDataErrorMessage(DataError.Local.NAME_CONFLICT),
        )
        assertEquals(
            "Sharing failed.",
            ErrorFormatter.formatDataErrorMessage(DataError.Local.SHARE_FAILED),
        )
        assertEquals(
            "This bookshelf is too large to share. Try sharing a smaller shelf or splitting it into multiple shelves.",
            ErrorFormatter.formatDataErrorMessage(DataError.Local.SHARE_LINK_TOO_LARGE),
        )
        assertEquals(
            "Unknown local error occurred.",
            ErrorFormatter.formatDataErrorMessage(DataError.Local.UNKNOWN),
        )
    }

    @Test
    fun `formatDataErrorMessage returns correct messages for all Validation errors`() {
        assertEquals(
            "Required field is empty.",
            ErrorFormatter.formatDataErrorMessage(DataError.Validation.EMPTY_FIELD),
        )
        assertEquals(
            "Invalid format.",
            ErrorFormatter.formatDataErrorMessage(DataError.Validation.INVALID_FORMAT),
        )
        assertEquals(
            "Input is too short.",
            ErrorFormatter.formatDataErrorMessage(DataError.Validation.TOO_SHORT),
        )
        assertEquals(
            "Input is too long.",
            ErrorFormatter.formatDataErrorMessage(DataError.Validation.TOO_LONG),
        )
        assertEquals(
            "Contains invalid characters.",
            ErrorFormatter.formatDataErrorMessage(DataError.Validation.INVALID_CHARACTERS),
        )
        assertEquals(
            "Value already exists.",
            ErrorFormatter.formatDataErrorMessage(DataError.Validation.DUPLICATE_VALUE),
        )
    }

    @Test
    fun `formatDataErrorMessage with operation includes operation context`() {
        // When
        val result =
            ErrorFormatter.formatDataErrorMessage(
                DataError.Local.DATABASE_ERROR,
                "remove shelf",
            )

        // Then
        assertEquals("Failed to remove shelf: Database operation failed.", result)
    }

    @Test
    fun `formatDataErrorMessage without operation returns raw message`() {
        // When
        val result = ErrorFormatter.formatDataErrorMessage(DataError.Local.DATABASE_ERROR)

        // Then
        assertEquals("Database operation failed.", result)
    }
}
