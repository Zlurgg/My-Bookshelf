package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase for sharing a bookshelf.
 * Handles the export and sharing functionality for bookshelves.
 */
interface ShareBookshelfUseCase {
    suspend operator fun invoke(shelfId: String): Result<Unit, DataError.Local>
}
