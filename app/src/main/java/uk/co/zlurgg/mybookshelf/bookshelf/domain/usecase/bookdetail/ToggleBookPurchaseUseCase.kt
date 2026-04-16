package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookdetail

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase for toggling the purchase status of a book.
 * Updates the book's purchased field and persists the change.
 */
interface ToggleBookPurchaseUseCase {
    suspend operator fun invoke(book: Book, purchased: Boolean): Result<Book, DataError.Local>
}
