package uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.model.BookProvider
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase for fetching a book's description from the appropriate remote provider.
 *
 * Pure fetch — no persistence side effects. Callers decide what to do with the
 * returned description (persist via [UpdateBookDescriptionUseCase], merge into
 * UI state, log, etc.). Errors propagate as the original [DataError.Remote] so
 * callers can distinguish between provider outages and other failures.
 */
interface GetBookDescriptionUseCase {
    suspend operator fun invoke(bookId: String, provider: BookProvider): Result<String?, DataError.Remote>
}
