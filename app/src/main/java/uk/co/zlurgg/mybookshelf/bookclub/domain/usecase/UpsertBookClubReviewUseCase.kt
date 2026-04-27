package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Creates or updates the current user's review for a book in a book club.
 *
 * Validates:
 * - Rating must be 0 (no rating) or between 1-5
 * - Review text must be at most 2000 characters
 */
interface UpsertBookClubReviewUseCase {
    suspend operator fun invoke(
        clubCode: String,
        bookId: String,
        rating: Float,
        reviewText: String
    ): Result<Unit, DataError.Sync>
}
