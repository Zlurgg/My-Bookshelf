package uk.co.zlurgg.mybookshelf.test

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.GetShelfBooksUseCase

class FakeGetShelfBooksUseCase(
    private val bookshelfRepository: BookshelfRepository
) : GetShelfBooksUseCase {

    override suspend fun execute(shelfId: String): Flow<List<Book>> {
        return bookshelfRepository.getBooksForShelf(shelfId)
    }
}