package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookdetail

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.BookDetailsWithShelfStatus
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase for getting comprehensive book details including shelf membership.
 * Handles the orchestration of loading book data, description, and shelf status.
 */
interface GetBookDetailsUseCase {
    suspend operator fun invoke(bookId: String, shelfId: String): Flow<BookDetailsWithShelfStatus>
    suspend fun loadBookDescription(bookId: String): Result<Unit, DataError.Local>
}
