package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase for duplicating a bookshelf with all its books.
 * Creates a complete copy of the shelf with a new ID and auto-generated name.
 */
interface DuplicateShelfUseCase {
    suspend fun execute(shelfId: String): Result<Bookshelf, DataError.Local>
}
