package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

/**
 * UseCase for getting all books on a specific bookshelf.
 * Provides reactive updates when books are added or removed from the shelf.
 */
interface GetShelfBooksUseCase {
    suspend fun execute(shelfId: String): Flow<List<Book>>
}