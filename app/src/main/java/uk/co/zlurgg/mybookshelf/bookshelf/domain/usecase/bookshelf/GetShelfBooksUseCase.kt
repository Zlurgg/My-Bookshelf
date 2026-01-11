package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book

/**
 * UseCase for getting all books on a specific bookshelf.
 * Provides reactive updates when books are added or removed from the shelf.
 */
interface GetShelfBooksUseCase {
    suspend operator fun invoke(shelfId: String): Flow<List<Book>>
}
