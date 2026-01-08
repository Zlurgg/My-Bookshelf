package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClubComment
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Gets all comments for a book in a book club.
 *
 * Returns comments from all club members, ordered by creation time (oldest first).
 */
interface GetBookClubCommentsUseCase {
    suspend operator fun invoke(
        clubCode: String,
        bookId: String,
    ): Result<List<BookClubComment>, DataError.Sync>
}
