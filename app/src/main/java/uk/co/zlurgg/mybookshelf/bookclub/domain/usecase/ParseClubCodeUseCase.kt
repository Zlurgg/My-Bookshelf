package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Parses and validates a book club code from user input.
 *
 * Accepts either:
 * - A raw 8-character club code (e.g., "ABC12XYZ")
 * - A full invite URL containing the code (e.g., "https://zlurgg.github.io/My-Bookshelf/club/ABC12XYZ")
 * - An app link with the code (e.g., "mybookshelf://club/ABC12XYZ")
 *
 * Returns the extracted and validated code, or an error if the input is invalid.
 */
interface ParseClubCodeUseCase {
    operator fun invoke(input: String): Result<String, DataError.Validation>
}
