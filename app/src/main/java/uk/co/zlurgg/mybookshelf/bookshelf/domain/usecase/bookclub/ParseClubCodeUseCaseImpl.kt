package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of ParseClubCodeUseCase that extracts and validates book club codes.
 *
 * Supports multiple input formats:
 * - Raw codes: "ABC12XYZ"
 * - Web URLs with query param: "https://zlurgg.github.io/My-Bookshelf/share/club?code=ABC12XYZ"
 * - Web URLs with path: "https://zlurgg.github.io/My-Bookshelf/club/ABC12XYZ"
 * - App links: "mybookshelf://club/ABC12XYZ"
 */
class ParseClubCodeUseCaseImpl : ParseClubCodeUseCase {
    override fun invoke(input: String): Result<String, DataError.Validation> {
        val trimmedInput = input.trim()

        if (trimmedInput.isBlank()) {
            Timber.tag(TAG).d("Empty input provided")
            return Result.Error(DataError.Validation.INVALID_CLUB_CODE)
        }

        val code = extractCode(trimmedInput)

        return if (isValidCode(code)) {
            Timber.tag(TAG).d("Valid code extracted: %s", code)
            Result.Success(code)
        } else {
            Timber.tag(TAG).d("Invalid code format: %s (from input: %s)", code, trimmedInput)
            Result.Error(DataError.Validation.INVALID_CLUB_CODE)
        }
    }

    /**
     * Extracts the club code from various input formats.
     */
    private fun extractCode(input: String): String {
        return when {
            // App link format: mybookshelf://club/CODE
            input.startsWith(APP_LINK_PREFIX, ignoreCase = true) -> {
                // Use drop() instead of removePrefix to handle case-insensitive removal
                input.drop(APP_LINK_PREFIX.length)
                    .removeSuffix("/")
                    .uppercase()
            }

            // Web URL with query param format: .../club?code=CODE
            input.contains(QUERY_PARAM_CODE, ignoreCase = true) -> {
                extractCodeFromQueryParam(input)
            }

            // Web URL with path format: .../club/CODE
            input.contains(WEB_URL_CLUB_PATH, ignoreCase = true) -> {
                val pathIndex = input.lowercase().indexOf(WEB_URL_CLUB_PATH.lowercase())
                if (pathIndex >= 0) {
                    input.substring(pathIndex + WEB_URL_CLUB_PATH.length)
                        .removeSuffix("/")
                        .removePrefix("/")
                        .uppercase()
                } else {
                    input.uppercase()
                }
            }

            // Raw code - just uppercase and validate
            else -> input.uppercase()
        }
    }

    /**
     * Extracts code from query parameter format: ?code=ABC12XYZ or &code=ABC12XYZ
     */
    private fun extractCodeFromQueryParam(input: String): String {
        val lowerInput = input.lowercase()
        val codeIndex = lowerInput.indexOf(QUERY_PARAM_CODE.lowercase())
        if (codeIndex < 0) return input.uppercase()

        val valueStart = codeIndex + QUERY_PARAM_CODE.length
        val valueEnd = input.indexOf('&', valueStart).takeIf { it >= 0 } ?: input.length

        return input.substring(valueStart, valueEnd)
            .trim()
            .uppercase()
    }

    /**
     * Validates that a code matches the expected format:
     * - Exactly 8 characters
     * - Only allowed characters (uppercase letters excluding O/I/L, digits excluding 0/1)
     */
    private fun isValidCode(code: String): Boolean {
        if (code.length != CODE_LENGTH) {
            return false
        }
        return code.all { it in ALLOWED_CHARS }
    }

    companion object {
        private const val TAG = "BookClubCode"
        private const val CODE_LENGTH = 8

        // Must match BookClubCodeGeneratorImpl
        private const val ALLOWED_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

        private const val APP_LINK_PREFIX = "mybookshelf://club/"
        private const val WEB_URL_CLUB_PATH = "/club/"
        private const val QUERY_PARAM_CODE = "code="
    }
}
