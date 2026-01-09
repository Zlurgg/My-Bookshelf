package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase for adding a book to a specific bookshelf.
 * Handles the business logic of both persisting the book and creating the shelf association.
 */
interface AddBookToShelfUseCase {
    suspend fun execute(book: Book, shelfId: String): Result<Unit, DataError.Local>
}
