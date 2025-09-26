package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Data class representing complete book details including shelf membership
 */
data class BookDetailsWithShelfStatus(
    val book: Book?,
    val isOnShelf: Boolean
)

/**
 * UseCase for getting comprehensive book details including shelf membership.
 * Handles the orchestration of loading book data, description, and shelf status.
 */
interface GetBookDetailsUseCase {
    suspend fun execute(bookId: String, shelfId: String): Flow<BookDetailsWithShelfStatus>
    suspend fun loadBookDescription(bookId: String): Result<Unit, DataError.Local>
}