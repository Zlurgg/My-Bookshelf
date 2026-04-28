package uk.co.zlurgg.mybookshelf.bookcase.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase for duplicating a bookshelf with all its books.
 * Creates a complete copy of the shelf with a new ID and auto-generated name.
 */
interface DuplicateShelfUseCase {
    suspend operator fun invoke(shelfId: String): Result<Bookshelf, DataError.Local>
}
