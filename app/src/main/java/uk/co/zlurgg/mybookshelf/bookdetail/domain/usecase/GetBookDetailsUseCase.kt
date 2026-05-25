package uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.book.domain.model.BookDetailsWithShelfStatus

/**
 * UseCase for getting comprehensive book details including shelf membership.
 *
 * Description fetching/persistence is intentionally split into the separate
 * [GetBookDescriptionUseCase] / [UpdateBookDescriptionUseCase] pair — keeps this
 * use case focused on the initial load and lets the ViewModel skip the remote
 * fetch when a description is already cached.
 */
interface GetBookDetailsUseCase {
    suspend operator fun invoke(bookId: String, shelfId: String?): Flow<BookDetailsWithShelfStatus>
}
