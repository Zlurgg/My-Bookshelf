package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Deletes the current user's review for a book in a book club.
 */
interface DeleteBookClubReviewUseCase {
    suspend operator fun invoke(
        clubCode: String,
        bookId: String
    ): Result<Unit, DataError.Sync>
}
