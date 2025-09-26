package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase for toggling the purchase status of a book.
 * Updates the book's purchased field and persists the change.
 */
interface ToggleBookPurchaseUseCase {
    suspend fun execute(book: Book, purchased: Boolean): Result<Book, DataError.Local>
}