package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Adds a new comment to a book in a book club.
 *
 * Unlike reviews, users can add multiple comments to the same book.
 * Returns the generated comment ID on success.
 */
interface AddBookClubCommentUseCase {
    suspend operator fun invoke(
        clubCode: String,
        bookId: String,
        text: String
    ): Result<String, DataError.Sync>
}
