package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase for deleting a bookshelf.
 * Provides both delete and restore functionality for undo operations.
 */
interface DeleteShelfUseCase {
    suspend fun execute(shelfId: String): Result<Unit, DataError.Local>
    suspend fun restore(shelf: Bookshelf): Result<Unit, DataError.Local>
}
