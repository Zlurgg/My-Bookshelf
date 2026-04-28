package uk.co.zlurgg.mybookshelf.book.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase for removing a book from a specific bookshelf.
 * Handles the business logic of removing the shelf association while keeping the book in the database.
 */
interface RemoveBookFromShelfUseCase {
    suspend operator fun invoke(bookId: String, shelfId: String): Result<Unit, DataError.Local>
}
