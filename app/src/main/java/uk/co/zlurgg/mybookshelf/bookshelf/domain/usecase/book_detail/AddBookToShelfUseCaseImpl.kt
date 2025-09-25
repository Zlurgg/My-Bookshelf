package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.Result

/**
 * Implementation of AddBookToShelfUseCase that orchestrates book persistence and shelf association.
 * Follows Clean Architecture by coordinating between domain repositories.
 */
class AddBookToShelfUseCaseImpl(
    private val bookRepository: BookRepository,
    private val bookshelfRepository: BookshelfRepository
) : AddBookToShelfUseCase {

    override suspend fun execute(book: Book, shelfId: String): Result<Unit, DataError.Local> {
        return try {
            // First persist the book (upsert handles both create and update)
            bookRepository.upsertBook(book)

            // Then create the shelf association
            bookshelfRepository.addBookToShelf(shelfId, book.id)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                ErrorMapper.mapExceptionToDataError(e) as? DataError.Local
                    ?: DataError.Local.UNKNOWN
            )
        }
    }
}