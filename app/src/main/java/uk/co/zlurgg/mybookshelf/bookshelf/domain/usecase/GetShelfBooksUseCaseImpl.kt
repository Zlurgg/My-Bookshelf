package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookshelfRepository

/**
 * Implementation of GetShelfBooksUseCase that provides reactive book list updates.
 * Wraps the repository's reactive data source.
 */
class GetShelfBooksUseCaseImpl(
    private val bookshelfRepository: BookshelfRepository
) : GetShelfBooksUseCase {

    override suspend operator fun invoke(shelfId: String): Flow<List<Book>> {
        return bookshelfRepository.getBooksForShelf(shelfId)
    }
}
