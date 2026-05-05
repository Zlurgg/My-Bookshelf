package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClubCode
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of ParseClubCodeUseCase that validates raw 12-character book club codes.
 */
class ParseClubCodeUseCaseImpl : ParseClubCodeUseCase {

    override fun invoke(input: String): Result<String, DataError.Validation> {
        val code = input.trim().uppercase()

        if (code.isBlank()) {
            Timber.tag(TAG).d("Empty input provided")
            return Result.Error(DataError.Validation.INVALID_CLUB_CODE)
        }

        return if (isValidCode(code)) {
            Timber.tag(TAG).d("Valid code: %s", code)
            Result.Success(code)
        } else {
            Timber.tag(TAG).d("Invalid code format: %s", code)
            Result.Error(DataError.Validation.INVALID_CLUB_CODE)
        }
    }

    /**
     * Validates that a code matches the expected format:
     * - Exactly 12 characters
     * - Only allowed characters (uppercase letters excluding O/I/L, digits excluding 0/1)
     */
    private fun isValidCode(code: String): Boolean {
        if (code.length != BookClubCode.CODE_LENGTH) {
            return false
        }
        return code.all { it in BookClubCode.VALID_CHARS }
    }

    companion object {
        private const val TAG = "BookClubCode"
    }
}
