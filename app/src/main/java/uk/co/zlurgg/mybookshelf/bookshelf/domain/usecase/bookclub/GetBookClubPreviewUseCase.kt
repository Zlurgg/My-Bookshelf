package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookClub
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Fetches book club metadata for preview before joining.
 *
 * This allows users to see the club name, style, book count, and member count
 * before committing to join.
 *
 * Returns null in the Success case if the club doesn't exist.
 */
interface GetBookClubPreviewUseCase {
    suspend operator fun invoke(code: String): Result<BookClub?, DataError.Sync>
}
