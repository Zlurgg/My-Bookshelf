package uk.co.zlurgg.mybookshelf.bookshelf.domain.util

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Domain-level constants for bookshelf business logic.
 */
object BookshelfConstants {
    /**
     * Name of the tutorial bookshelf created on first app launch
     * and restored via the help icon.
     */
    const val TUTORIAL_SHELF_NAME = "Tutorial Bookshelf"

    /**
     * Maximum length for shelf names to prevent UI overflow
     */
    const val MAX_SHELF_NAME_LENGTH = 50

    /**
     * Regex pattern for valid shelf names - alphanumeric, spaces, parentheses, and hyphens
     * Prevents special characters that could cause JSON or UI issues
     */
    private val SHELF_NAME_PATTERN = Regex("^[a-zA-Z0-9 ()\\-]+$")

    /**
     * Validates a shelf name according to business rules.
     *
     * Rules:
     * - Cannot be blank
     * - Must be 50 characters or less
     * - Can only contain letters, numbers, spaces, parentheses, and hyphens
     *
     * @param name The shelf name to validate
     * @return Result indicating success or specific validation error
     */
    fun validateShelfName(name: String): Result<Unit, DataError.Validation> {
        return when {
            name.isBlank() -> Result.Error(DataError.Validation.EMPTY_FIELD)
            name.length > MAX_SHELF_NAME_LENGTH -> Result.Error(DataError.Validation.TOO_LONG)
            !SHELF_NAME_PATTERN.matches(name) -> Result.Error(DataError.Validation.INVALID_CHARACTERS)
            else -> Result.Success(Unit)
        }
    }
}
