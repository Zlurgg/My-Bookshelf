package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase for caching/upserting a book in the local database.
 * Used when user clicks on a book to ensure it's available for the detail screen.
 */
interface UpsertBookUseCase {
    suspend fun execute(book: Book): Result<Unit, DataError.Local>
}