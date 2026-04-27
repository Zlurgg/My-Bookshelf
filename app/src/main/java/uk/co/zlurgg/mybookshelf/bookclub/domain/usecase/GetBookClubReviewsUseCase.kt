package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClubReview
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Gets all reviews for a book in a book club.
 *
 * Returns reviews from all club members, including ratings and review text.
 */
interface GetBookClubReviewsUseCase {
    suspend operator fun invoke(
        clubCode: String,
        bookId: String
    ): Result<List<BookClubReview>, DataError.Sync>
}
