package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf

import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

/**
 * UseCase for sharing a bookshelf.
 * Handles the export and sharing functionality for bookshelves.
 */
interface ShareBookshelfUseCase {
    suspend fun execute(shelfId: String): Result<Unit, DataError.Local>
}