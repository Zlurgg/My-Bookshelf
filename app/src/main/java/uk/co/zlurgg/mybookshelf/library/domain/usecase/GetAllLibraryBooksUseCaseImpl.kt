package uk.co.zlurgg.mybookshelf.library.domain.usecase

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository

class GetAllLibraryBooksUseCaseImpl(
    private val bookRepository: BookRepository,
) : GetAllLibraryBooksUseCase {

    override operator fun invoke(): Flow<List<Book>> {
        return bookRepository.getAllPersonalBooks()
    }
}
