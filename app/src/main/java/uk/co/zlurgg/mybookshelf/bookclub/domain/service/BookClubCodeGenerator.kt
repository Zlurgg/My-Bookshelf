package uk.co.zlurgg.mybookshelf.bookclub.domain.service

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Service for generating unique book club codes.
 */
interface BookClubCodeGenerator {
    /**
     * Generates a unique 8-character alphanumeric code for a book club.
     * Checks for collisions with existing clubs and retries if necessary.
     *
     * @return A unique code or an error if generation fails after max retries
     */
    suspend fun generateUniqueCode(): Result<String, DataError.Sync>
}
